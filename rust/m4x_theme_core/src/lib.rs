use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use serde::Serialize;
use sha2::{Digest, Sha256};
use std::collections::HashSet;
use std::fs::File;
use std::io::{self, BufReader, Read};
use std::path::{Component, Path};
use std::ptr;
use zip::ZipArchive;

const MAX_ENTRIES: usize = 20_000;
const MAX_PATH_LENGTH: usize = 512;
const MAX_DIRECTORY_DEPTH: usize = 32;
const MAX_TOTAL_UNCOMPRESSED_BYTES: u64 = 2 * 1024 * 1024 * 1024;
const SUSPICIOUS_RATIO: u64 = 500;
const RATIO_CHECK_MIN_BYTES: u64 = 10 * 1024 * 1024;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct ValidationResult {
    valid: bool,
    status: &'static str,
    message: String,
    sha256: String,
    size_bytes: u64,
    entry_count: usize,
    total_uncompressed_bytes: u64,
    warnings: Vec<String>,
    errors: Vec<String>,
}

impl ValidationResult {
    fn failed(message: impl Into<String>, size_bytes: u64, sha256: String) -> Self {
        let message = message.into();
        Self {
            valid: false,
            status: "failed",
            message: message.clone(),
            sha256,
            size_bytes,
            entry_count: 0,
            total_uncompressed_bytes: 0,
            warnings: Vec::new(),
            errors: vec![message],
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_m4xtheme_app_rust_RustThemeValidator_validateThemePath(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    max_size_bytes: jlong,
) -> jstring {
    let path_string: String = match env.get_string(&path) {
        Ok(value) => value.into(),
        Err(error) => {
            return json_to_jstring(
                &mut env,
                &ValidationResult::failed(
                    format!("Không đọc được đường dẫn file: {error}"),
                    0,
                    String::new(),
                ),
            );
        }
    };

    let max_size = if max_size_bytes <= 0 {
        100 * 1024 * 1024
    } else {
        max_size_bytes as u64
    };

    let result = validate_theme(Path::new(&path_string), max_size);
    json_to_jstring(&mut env, &result)
}

fn json_to_jstring(env: &mut JNIEnv, result: &ValidationResult) -> jstring {
    let json = serde_json::to_string(result).unwrap_or_else(|_| {
        r#"{"valid":false,"status":"failed","message":"Không tạo được kết quả kiểm tra","sha256":"","sizeBytes":0,"entryCount":0,"totalUncompressedBytes":0,"warnings":[],"errors":["Lỗi JSON"]}"#.to_string()
    });

    match env.new_string(json) {
        Ok(output) => output.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

fn validate_theme(path: &Path, max_size_bytes: u64) -> ValidationResult {
    let metadata = match path.metadata() {
        Ok(value) => value,
        Err(error) => {
            return ValidationResult::failed(
                format!("Không đọc được thông tin file: {error}"),
                0,
                String::new(),
            )
        }
    };

    let size_bytes = metadata.len();
    if !metadata.is_file() {
        return ValidationResult::failed(
            "Đường dẫn không phải là file",
            size_bytes,
            String::new(),
        );
    }

    let extension = path
        .extension()
        .and_then(|value| value.to_str())
        .unwrap_or_default()
        .to_ascii_lowercase();

    if extension != "mtz" && extension != "zip" {
        return ValidationResult::failed(
            "Chỉ chấp nhận file .mtz hoặc .zip",
            size_bytes,
            String::new(),
        );
    }

    if size_bytes == 0 {
        return ValidationResult::failed("File trống", 0, String::new());
    }

    if size_bytes > max_size_bytes {
        return ValidationResult::failed(
            format!("File vượt giới hạn {} MB", max_size_bytes / 1024 / 1024),
            size_bytes,
            String::new(),
        );
    }

    let sha256 = match calculate_sha256(path) {
        Ok(value) => value,
        Err(error) => {
            return ValidationResult::failed(
                format!("Không tính được SHA-256: {error}"),
                size_bytes,
                String::new(),
            )
        }
    };

    let file = match File::open(path) {
        Ok(value) => value,
        Err(error) => {
            return ValidationResult::failed(
                format!("Không mở được file: {error}"),
                size_bytes,
                sha256,
            )
        }
    };

    let mut archive = match ZipArchive::new(file) {
        Ok(value) => value,
        Err(error) => {
            return ValidationResult::failed(
                format!("File không phải ZIP/MTZ hợp lệ hoặc đã bị hỏng: {error}"),
                size_bytes,
                sha256,
            )
        }
    };

    let entry_count = archive.len();
    let mut warnings = Vec::new();
    let mut errors = Vec::new();
    let mut names = HashSet::new();
    let mut total_uncompressed_bytes = 0_u64;
    let mut has_theme_descriptor = false;

    if entry_count == 0 {
        errors.push("Gói theme không chứa dữ liệu".to_string());
    }

    if entry_count > MAX_ENTRIES {
        errors.push(format!(
            "Gói theme có quá nhiều mục: {entry_count}/{MAX_ENTRIES}"
        ));
    }

    let max_total_uncompressed = max_size_bytes
        .saturating_mul(20)
        .min(MAX_TOTAL_UNCOMPRESSED_BYTES);

    for index in 0..entry_count.min(MAX_ENTRIES + 1) {
        let entry = match archive.by_index(index) {
            Ok(value) => value,
            Err(error) => {
                errors.push(format!("Không đọc được mục #{index}: {error}"));
                continue;
            }
        };

        let raw_name = entry.name().replace('\\', "/");
        let lower_name = raw_name.to_ascii_lowercase();

        if raw_name.len() > MAX_PATH_LENGTH {
            errors.push(format!("Đường dẫn quá dài trong gói: {raw_name}"));
        }

        if raw_name.starts_with('/') || raw_name.starts_with('\\') {
            errors.push(format!("Phát hiện đường dẫn tuyệt đối: {raw_name}"));
        }

        if entry.enclosed_name().is_none() || has_unsafe_component(Path::new(&raw_name)) {
            errors.push(format!("Phát hiện đường dẫn không an toàn: {raw_name}"));
        }

        let depth = Path::new(&raw_name).components().count();
        if depth > MAX_DIRECTORY_DEPTH {
            errors.push(format!("Cấu trúc thư mục quá sâu: {raw_name}"));
        }

        if !names.insert(lower_name.clone()) {
            warnings.push(format!("Tên file bị lặp: {raw_name}"));
        }

        if is_symlink(entry.unix_mode()) {
            errors.push(format!("Không cho phép symbolic link: {raw_name}"));
        }

        if contains_blocked_extension(&lower_name) {
            errors.push(format!("Phát hiện loại file nguy hiểm: {raw_name}"));
        }

        if lower_name.ends_with("description.xml")
            || lower_name.ends_with("manifest.xml")
            || lower_name.ends_with("theme_values.xml")
        {
            has_theme_descriptor = true;
        }

        if !entry.is_dir() {
            let uncompressed = entry.size();
            let compressed = entry.compressed_size();
            total_uncompressed_bytes =
                total_uncompressed_bytes.saturating_add(uncompressed);

            if total_uncompressed_bytes > max_total_uncompressed {
                errors.push(format!(
                    "Dữ liệu sau giải nén vượt giới hạn an toàn {} MB",
                    max_total_uncompressed / 1024 / 1024
                ));
                break;
            }

            if uncompressed >= RATIO_CHECK_MIN_BYTES
                && (compressed == 0
                    || uncompressed / compressed.max(1) > SUSPICIOUS_RATIO)
            {
                errors.push(format!(
                    "Tỷ lệ nén bất thường, có nguy cơ ZIP bomb: {raw_name}"
                ));
            }
        }

        if errors.len() >= 50 {
            errors.push("Đã dừng do phát hiện quá nhiều lỗi".to_string());
            break;
        }
    }

    if !has_theme_descriptor {
        warnings.push(
            "Không tìm thấy description.xml, manifest.xml hoặc theme_values.xml; Admin cần kiểm tra thủ công"
                .to_string(),
        );
    }

    if !errors.is_empty() {
        ValidationResult {
            valid: false,
            status: "failed",
            message: format!("File bị từ chối: phát hiện {} lỗi", errors.len()),
            sha256,
            size_bytes,
            entry_count,
            total_uncompressed_bytes,
            warnings,
            errors,
        }
    } else if !warnings.is_empty() {
        ValidationResult {
            valid: true,
            status: "warning",
            message: format!(
                "File có thể upload nhưng cần Admin kiểm tra: {} cảnh báo",
                warnings.len()
            ),
            sha256,
            size_bytes,
            entry_count,
            total_uncompressed_bytes,
            warnings,
            errors,
        }
    } else {
        ValidationResult {
            valid: true,
            status: "passed",
            message: format!("File hợp lệ, đã kiểm tra {entry_count} mục"),
            sha256,
            size_bytes,
            entry_count,
            total_uncompressed_bytes,
            warnings,
            errors,
        }
    }
}

fn calculate_sha256(path: &Path) -> io::Result<String> {
    let file = File::open(path)?;
    let mut reader = BufReader::new(file);
    let mut hasher = Sha256::new();
    let mut buffer = [0_u8; 64 * 1024];

    loop {
        let read = reader.read(&mut buffer)?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }

    Ok(format!("{:x}", hasher.finalize()))
}

fn has_unsafe_component(path: &Path) -> bool {
    path.components().any(|component| {
        matches!(
            component,
            Component::ParentDir | Component::RootDir | Component::Prefix(_)
        )
    })
}

fn is_symlink(mode: Option<u32>) -> bool {
    mode.map(|value| value & 0o170000 == 0o120000)
        .unwrap_or(false)
}

fn contains_blocked_extension(lower_name: &str) -> bool {
    const BLOCKED: &[&str] = &[
        ".apk", ".dex", ".exe", ".dll", ".so", ".com", ".scr", ".bat", ".cmd",
        ".ps1", ".msi",
    ];

    BLOCKED.iter().any(|extension| lower_name.ends_with(extension))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unsafe_components_are_detected() {
        assert!(has_unsafe_component(Path::new("../evil.txt")));
        assert!(!has_unsafe_component(Path::new(
            "lockscreen/advance/manifest.xml"
        )));
    }

    #[test]
    fn blocked_extensions_are_detected() {
        assert!(contains_blocked_extension("payload/classes.dex"));
        assert!(contains_blocked_extension("payload/app.apk"));
        assert!(!contains_blocked_extension("lockscreen/manifest.xml"));
    }
}
