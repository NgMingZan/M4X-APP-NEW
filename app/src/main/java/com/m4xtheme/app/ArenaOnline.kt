package com.m4xtheme.app

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class ArenaOnlinePlayer(
    val userId: String,
    val displayName: String,
    val slot: Int
)

data class ArenaMatchTicket(
    val matchId: String,
    val slot: Int,
    val hostUserId: String,
    val status: String,
    val players: List<ArenaOnlinePlayer>,
    val waitSeconds: Int
) {
    val isHostFor: (String) -> Boolean
        get() = { userId -> hostUserId == userId }
}

data class ArenaRewardClaim(
    val reward: Int,
    val balance: Long,
    val message: String
)

/**
 * Client Supabase Realtime tối giản dùng trực tiếp protocol Phoenix v1.
 *
 * Kênh sử dụng Broadcast public với topic chứa UUID ngẫu nhiên của trận.
 * Dữ liệu nhạy cảm và M4X Coin không truyền qua kênh này; phần thưởng vẫn do
 * Postgres RPC xử lý.
 */
class ArenaRealtimeRoom(
    private val session: Session,
    private val matchId: String,
    private val onConnected: () -> Unit,
    private val onInput: (JSONObject) -> Unit,
    private val onSnapshot: (JSONObject) -> Unit,
    private val onEvent: (String, JSONObject) -> Unit,
    private val onError: (String) -> Unit
) {
    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val refs = AtomicLong(1L)
    private val topic = "realtime:arena-$matchId"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var socket: WebSocket? = null
    @Volatile private var joined = false

    fun connect() {
        if (!SupabaseConfig.configured) {
            onError("Chưa cấu hình Supabase")
            return
        }

        /*
         * WebSocket handshake chỉ dùng API key trong query parameter.
         * JWT người dùng được gửi trong payload phx_join bên dưới.
         *
         * Không gửi Authorization header ở bước handshake: token hết hạn
         * hoặc gateway không chấp nhận header này sẽ trả HTTP 401 trước khi
         * WebSocket được nâng cấp lên HTTP 101.
         */
        val wsUrl = (
            SupabaseConfig.url
                .replaceFirst("https://", "wss://")
                .replaceFirst("http://", "ws://") +
                "/realtime/v1/websocket"
            )
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("apikey", SupabaseConfig.key)
            .addQueryParameter("vsn", "1.0.0")
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sendJoin()
                scheduler.scheduleAtFixedRate(
                    { sendHeartbeat() },
                    18,
                    18,
                    TimeUnit.SECONDS
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val message = JSONObject(text)
                    val event = message.optString("event")
                    val payload = message.optJSONObject("payload") ?: JSONObject()

                    when (event) {
                        "phx_reply" -> {
                            if (payload.optString("status") == "ok" && !joined) {
                                joined = true
                                onConnected()
                                sendEvent(
                                    event = "player_joined",
                                    payload = JSONObject()
                                        .put("userId", session.userId)
                                        .put("at", System.currentTimeMillis())
                                )
                            } else if (payload.optString("status") == "error") {
                                onError(
                                    payload.optJSONObject("response")
                                        ?.optString("reason")
                                        .orEmpty()
                                        .ifBlank { "Không vào được phòng Realtime" }
                                )
                            }
                        }

                        "broadcast" -> {
                            val broadcastEvent = payload.optString("event")
                            val body = payload.optJSONObject("payload") ?: JSONObject()
                            when (broadcastEvent) {
                                "arena_input" -> onInput(body)
                                "arena_snapshot" -> onSnapshot(body)
                                else -> onEvent(broadcastEvent, body)
                            }
                        }

                        "phx_error", "phx_close" -> {
                            joined = false
                            onError("Kết nối trận đấu bị gián đoạn")
                        }
                    }
                }.onFailure {
                    onError(it.message ?: "Không đọc được dữ liệu trận đấu")
                }
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                joined = false

                val status = response?.code
                val message = when (status) {
                    401 -> (
                        "Supabase Realtime từ chối API key (401). " +
                            "Kiểm tra SUPABASE_ANON_KEY trong GitHub Secrets."
                    )
                    403 -> "Dự án chưa cho phép kết nối Realtime (403)."
                    else -> t.message
                        ?: "Không kết nối được máy chủ trận đấu"
                }

                onError(message)
            }
        })
    }

    fun sendInput(payload: JSONObject) {
        sendEvent("arena_input", payload)
    }

    fun sendSnapshot(payload: JSONObject) {
        sendEvent("arena_snapshot", payload)
    }

    fun sendEvent(event: String, payload: JSONObject) {
        if (!joined && event != "player_joined") return
        val ref = refs.getAndIncrement().toString()
        val message = JSONObject()
            .put("topic", topic)
            .put("event", "broadcast")
            .put(
                "payload",
                JSONObject()
                    .put("type", "broadcast")
                    .put("event", event)
                    .put("payload", payload)
            )
            .put("ref", ref)
            .put("join_ref", "1")
        socket?.send(message.toString())
    }

    fun close() {
        runCatching {
            if (joined) {
                val ref = refs.getAndIncrement().toString()
                socket?.send(
                    JSONObject()
                        .put("topic", topic)
                        .put("event", "phx_leave")
                        .put("payload", JSONObject())
                        .put("ref", ref)
                        .put("join_ref", "1")
                        .toString()
                )
            }
        }
        joined = false
        scheduler.shutdownNow()
        socket?.close(1000, "leave")
        socket = null
        http.dispatcher.executorService.shutdown()
        http.connectionPool.evictAll()
    }

    private fun sendJoin() {
        val payload = JSONObject()
            .put(
                "config",
                JSONObject()
                    .put(
                        "broadcast",
                        JSONObject()
                            .put("ack", false)
                            .put("self", true)
                    )
                    .put(
                        "presence",
                        JSONObject()
                            .put("enabled", false)
                    )
                    .put("postgres_changes", JSONArray())
                    .put("private", false)
            )
            .put("access_token", session.token)

        socket?.send(
            JSONObject()
                .put("topic", topic)
                .put("event", "phx_join")
                .put("payload", payload)
                .put("ref", "1")
                .put("join_ref", "1")
                .toString()
        )
    }

    private fun sendHeartbeat() {
        val ref = refs.getAndIncrement().toString()
        socket?.send(
            JSONObject()
                .put("topic", "phoenix")
                .put("event", "heartbeat")
                .put("payload", JSONObject())
                .put("ref", ref)
                .put("join_ref", JSONObject.NULL)
                .toString()
        )
    }
}
