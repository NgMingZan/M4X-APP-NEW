package com.m4xtheme.app.rust

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class ThemeMetadataResult(
    val title: String = "",
    val author: String = "",
    val designer: String = "",
    val version: String = "",
    val uiVersion: String = "",
    val platform: String = "",
    val description: String = "",
    val sourceFile: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("author", author)
        put("designer", designer)
        put("version", version)
        put("uiVersion", uiVersion)
        put("platform", platform)
        put("description", description)
        put("sourceFile", sourceFile)
    }
}

data class ThemeModuleResult(
    val key: String,
    val label: String,
    val present: Boolean,
    val entryCount: Int,
    val totalBytes: Long,
    val status: String,
    val message: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("key", key)
        put("label", label)
        put("present", present)
        put("entryCount", entryCount)
        put("totalBytes", totalBytes)
        put("status", status)
        put("message", message)
    }
}

data class ThemeValidationResult(
    val valid: Boolean,
    val status: String,
    val message: String,
    val sha256: String,
    val sizeBytes: Long,
    val entryCount: Int,
    val totalUncompressedBytes: Long,
    val safetyScore: Int,
    val safetyLevel: String,
    val metadata: ThemeMetadataResult,
    val modules: List<ThemeModuleResult>,
    val findings: List<String>,
    val warnings: List<String>,
    val errors: List<String>,
    val rawJson: String
) {
    val adminSummary: String
        get() = buildString {
            append(message)
            metadata.title.takeIf { it.isNotBlank() }?.let {
                append(" | Theme: ")
                append(it)
            }
            if (warnings.isNotEmpty()) {
                append(" | Cảnh báo: ")
                append(warnings.take(3).joinToString("; "))
            }
        }

    fun modulesJson(): JSONArray = JSONArray().apply {
        modules.forEach { put(it.toJson()) }
    }
}

data class HashVerificationResult(
    val valid: Boolean,
    val matches: Boolean,
    val message: String,
    val actualSha256: String,
    val expectedSha256: String,
    val sizeBytes: Long
)

object RustThemeValidator {
    const val DEFAULT_MAX_SIZE_BYTES: Long = 100L * 1024L * 1024L

    private val loadFailure: Throwable? = runCatching {
        System.loadLibrary("m4x_theme_core")
    }.exceptionOrNull()

    @JvmStatic
    private external fun validateThemePath(
        path: String,
        maxSizeBytes: Long
    ): String

    @JvmStatic
    private external fun verifyFileSha256Path(
        path: String,
        expectedSha256: String
    ): String

    fun validate(
        context: Context,
        uri: Uri,
        maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
    ): Result<ThemeValidationResult> = runCatching {
        ensureLoaded()
        require(maxSizeBytes > 0) {
            "Giới hạn dung lượng phải lớn hơn 0"
        }

        val originalName = queryFileName(context, uri)
        val suffix = when {
            originalName.endsWith(".mtz", ignoreCase = true) -> ".mtz"
            originalName.endsWith(".zip", ignoreCase = true) -> ".zip"
            else -> throw IllegalArgumentException(
                "Chỉ nhận file .mtz hoặc .zip"
            )
        }

        val temporaryFile = File.createTempFile(
            "m4x_theme_validation_",
            suffix,
            context.cacheDir
        )

        try {
            copyUriWithLimit(
                context = context,
                uri = uri,
                destination = temporaryFile,
                maxSizeBytes = maxSizeBytes
            )

            val rawJson = validateThemePath(
                temporaryFile.absolutePath,
                maxSizeBytes
            )

            require(rawJson.isNotBlank()) {
                "Rust không trả về kết quả kiểm tra"
            }

            parseValidationResult(rawJson)
        } finally {
            runCatching { temporaryFile.delete() }
        }
    }

    fun verifyDownloadedFile(
        file: File,
        expectedSha256: String
    ): Result<HashVerificationResult> = runCatching {
        ensureLoaded()
        require(file.isFile) { "Không tìm thấy file đã tải" }
        require(expectedSha256.isNotBlank()) {
            "Theme chưa có SHA-256 được duyệt để xác minh"
        }

        val rawJson = verifyFileSha256Path(
            file.absolutePath,
            expectedSha256
        )
        require(rawJson.isNotBlank()) {
            "Rust không trả về kết quả xác minh"
        }

        val json = JSONObject(rawJson)
        HashVerificationResult(
            valid = json.optBoolean("valid", false),
            matches = json.optBoolean("matches", false),
            message = json.optString("message", "Không có kết quả"),
            actualSha256 = json.optString("actualSha256"),
            expectedSha256 = json.optString("expectedSha256"),
            sizeBytes = json.optLong("sizeBytes")
        )
    }

    private fun ensureLoaded() {
        loadFailure?.let {
            throw IllegalStateException(
                "APK chưa đóng gói thư viện Rust m4x_theme_core",
                it
            )
        }
    }

    private fun copyUriWithLimit(
        context: Context,
        uri: Uri,
        destination: File,
        maxSizeBytes: Long
    ) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Không mở được file đã chọn")

        input.use { source ->
            FileOutputStream(destination).use { target ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L

                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue

                    total += count
                    if (total > maxSizeBytes) {
                        throw IllegalArgumentException(
                            "File vượt giới hạn ${maxSizeBytes / 1024 / 1024} MB"
                        )
                    }

                    target.write(buffer, 0, count)
                }

                target.fd.sync()
            }
        }
    }

    private fun queryFileName(
        context: Context,
        uri: Uri
    ): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val column = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )
                if (column >= 0) {
                    return cursor.getString(column).orEmpty()
                }
            }
        }

        return uri.lastPathSegment.orEmpty()
    }

    private fun parseValidationResult(rawJson: String): ThemeValidationResult {
        val json = JSONObject(rawJson)
        val metadataJson = json.optJSONObject("metadata") ?: JSONObject()
        val moduleJson = json.optJSONArray("modules") ?: JSONArray()

        val metadata = ThemeMetadataResult(
            title = metadataJson.optString("title"),
            author = metadataJson.optString("author"),
            designer = metadataJson.optString("designer"),
            version = metadataJson.optString("version"),
            uiVersion = metadataJson.optString("uiVersion"),
            platform = metadataJson.optString("platform"),
            description = metadataJson.optString("description"),
            sourceFile = metadataJson.optString("sourceFile")
        )

        val modules = List(moduleJson.length()) { index ->
            val item = moduleJson.optJSONObject(index) ?: JSONObject()
            ThemeModuleResult(
                key = item.optString("key"),
                label = item.optString("label"),
                present = item.optBoolean("present", false),
                entryCount = item.optInt("entryCount"),
                totalBytes = item.optLong("totalBytes"),
                status = item.optString("status"),
                message = item.optString("message")
            )
        }

        return ThemeValidationResult(
            valid = json.optBoolean("valid", false),
            status = json.optString("status", "failed"),
            message = json.optString(
                "message",
                "Không có nội dung kết quả"
            ),
            sha256 = json.optString("sha256"),
            sizeBytes = json.optLong("sizeBytes"),
            entryCount = json.optInt("entryCount"),
            totalUncompressedBytes =
                json.optLong("totalUncompressedBytes"),
            safetyScore = json.optInt("safetyScore"),
            safetyLevel = json.optString("safetyLevel", "danger"),
            metadata = metadata,
            modules = modules,
            findings = json.optJSONArray("findings").toStringList(),
            warnings = json.optJSONArray("warnings").toStringList(),
            errors = json.optJSONArray("errors").toStringList(),
            rawJson = rawJson
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }
    }
}
