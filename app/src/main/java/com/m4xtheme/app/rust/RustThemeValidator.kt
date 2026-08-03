package com.m4xtheme.app.rust

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class ThemeValidationResult(
    val valid: Boolean,
    val status: String,
    val message: String,
    val sha256: String,
    val sizeBytes: Long,
    val entryCount: Int,
    val totalUncompressedBytes: Long,
    val warnings: List<String>,
    val errors: List<String>
) {
    val adminSummary: String
        get() = buildString {
            append(message)
            if (warnings.isNotEmpty()) {
                append(" | Cảnh báo: ")
                append(warnings.take(3).joinToString("; "))
            }
        }
}

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

    /**
     * Hàm đồng bộ vì SupabaseApi.uploadTheme đã chạy trong Dispatchers.IO.
     */
    fun validate(
        context: Context,
        uri: Uri,
        maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
    ): Result<ThemeValidationResult> = runCatching {
        loadFailure?.let {
            throw IllegalStateException(
                "APK chưa đóng gói thư viện Rust m4x_theme_core",
                it
            )
        }

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

            parseResult(rawJson)
        } finally {
            runCatching { temporaryFile.delete() }
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

    private fun parseResult(rawJson: String): ThemeValidationResult {
        val json = JSONObject(rawJson)

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
            warnings = json.optJSONArray("warnings").toStringList(),
            errors = json.optJSONArray("errors").toStringList()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }
    }
}
