use jni::objects::{JClass, JString};
use jni::sys::{jlong, jstring};
use jni::JNIEnv;
use serde::Serialize;
use sha2::{Digest, Sha256};
use std::collections::{BTreeMap, HashSet};
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
const MAX_METADATA_XML_BYTES: u64 = 1024 * 1024;

#[derive(Debug, Default, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct ThemeMetadata {
    title: String,
    author: String,
    designer: String,
    version: String,
    ui_version: String,
    platform: String,
    description: String,
    source_file: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct ModuleReport {
    key: String,
    label: String,
    present: bool,
    entry_count: usize,
    total_bytes: u64,
    status: &'static str,
    message: String,
}

#[derive(Debug, Default, Clone)]
struct ModuleAccumulator {
    entry_count: usize,
    total_bytes: u64,
}

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
    safety_score: u8,
    safety_level: &'static str,
    metadata: ThemeMetadata,
    modules: Vec<ModuleReport>,
    findings: Vec<String>,
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
            safety_score: 0,
            safety_level: "danger",
            metadata: ThemeMetadata::default(),
            modules: empty_module_reports(),
            findings: Vec::new(),
            warnings: Vec::new(),
            errors: vec![message],
        }
    }
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct HashVerificationResult {
    valid: bool,
    matches: bool,
    message: String,
    actual_sha256: String,
    expected_sha256: String,
    size_bytes: u64,
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

#[no_mangle]
pub extern "system" fn Java_com_m4xtheme_app_rust_RustThemeValidator_verifyFileSha256Path(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    expected_sha256: JString,
) -> jstring {
    let path_string: String = match env.get_string(&path) {
        Ok(value) => value.into(),
        Err(error) => {
            let result = HashVerificationResult {
                valid: false,
                matches: false,
                message: format!("Không đọc được đường dẫn file: {error}"),
                actual_sha256: String::new(),
                expected_sha256: String::new(),
                size_bytes: 0,
            };
            return json_to_jstring(&mut env, &result);
        }
    };

    let expected: String = match env.get_string(&expected_sha256) {
        Ok(value) => value.into(),
        Err(error) => {
            let result = HashVerificationResult {
                valid: false,
                matches: false,
                message: format!("Không đọc được SHA-256 dự kiến: {error}"),
                actual_sha256: String::new(),
                expected_sha256: String::new(),
                size_bytes: 0,
            };
            return json_to_jstring(&mut env, &result);
        }
    };

    let result = verify_file_sha256(Path::new(&path_string), &expected);
    json_to_jstring(&mut env, &result)
}

fn json_to_jstring<T: Serialize>(env: &mut JNIEnv, value: &T) -> jstring {
    let json = serde_json::to_string(value).unwrap_or_else(|_| {
        r#"{"valid":false,"message":"Không tạo được kết quả JSON"}"#.to_string()
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
    let mut findings = Vec::new();
    let mut names = HashSet::new();
    let mut total_uncompressed_bytes = 0_u64;
    let mut has_theme_descriptor = false;
    let mut theme_metadata = ThemeMetadata::default();
    let mut module_stats = initial_module_stats();

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
        let mut entry = match archive.by_index(index) {
            Ok(value) => value,
            Err(error) => {
                errors.push(format!("Không đọc được mục #{index}: {error}"));
                continue;
            }
        };

        let raw_name = entry.name().replace('\\', "/");
        let lower_name = raw_name.to_ascii_lowercase();
        let uncompressed = entry.size();

        if raw_name.len() > MAX_PATH_LENGTH {
            errors.push(format!("Đường dẫn quá dài trong gói: {raw_name}"));
        }

        if raw_name.starts_with('/') || raw_name.starts_with('\\') {
            errors.push(format!("Phát hiện đường dẫn tuyệt đối: {raw_name}"));
        }

        if entry.enclosed_name().is_none()
            || has_unsafe_component(Path::new(&raw_name))
        {
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

        for module_key in detect_modules(&lower_name) {
            if let Some(stat) = module_stats.get_mut(module_key) {
                stat.entry_count += 1;
                stat.total_bytes = stat.total_bytes.saturating_add(uncompressed);
            }
        }

        let is_metadata_xml = lower_name.ends_with("description.xml")
            || lower_name.ends_with("manifest.xml")
            || lower_name.ends_with("theme_values.xml");

        if is_metadata_xml {
            has_theme_descriptor = true;

            if uncompressed > MAX_METADATA_XML_BYTES {
                warnings.push(format!(
                    "Bỏ qua đọc metadata vì XML quá lớn: {raw_name}"
                ));
            } else if !entry.is_dir() {
                let mut bytes = Vec::with_capacity(uncompressed as usize);
                match entry.read_to_end(&mut bytes) {
                    Ok(_) => {
                        let xml = String::from_utf8_lossy(&bytes);
                        if contains_unsafe_xml_declaration(&xml) {
                            errors.push(format!(
                                "XML chứa DOCTYPE/ENTITY không an toàn: {raw_name}"
                            ));
                        }
                        merge_metadata(&mut theme_metadata, &xml, &raw_name);
                    }
                    Err(error) => warnings.push(format!(
                        "Không đọc được metadata {raw_name}: {error}"
                    )),
                }
            }
        }

        if !entry.is_dir() {
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

    if theme_metadata.title.is_empty() {
        warnings.push("Không đọc được tên theme từ metadata".to_string());
    } else {
        findings.push(format!("Đã nhận diện theme: {}", theme_metadata.title));
    }

    if !theme_metadata.author.is_empty() || !theme_metadata.designer.is_empty() {
        let creator = if !theme_metadata.author.is_empty() {
            &theme_metadata.author
        } else {
            &theme_metadata.designer
        };
        findings.push(format!("Tác giả/nhà thiết kế: {creator}"));
    }

    let modules = finalize_module_reports(module_stats);
    let present_modules: Vec<&str> = modules
        .iter()
        .filter(|module| module.present)
        .map(|module| module.label.as_str())
        .collect();

    if present_modules.is_empty() {
        warnings.push("Không nhận diện được module Xiaomi Theme phổ biến".to_string());
    } else {
        findings.push(format!(
            "Đã nhận diện {} module: {}",
            present_modules.len(),
            present_modules.join(", ")
        ));
    }

    if size_bytes > 0 {
        let expansion_ratio = total_uncompressed_bytes / size_bytes.max(1);
        if expansion_ratio >= 15 {
            warnings.push(format!(
                "Tỷ lệ dữ liệu sau giải nén cao: khoảng {expansion_ratio} lần"
            ));
        }
    }

    findings.push(format!("SHA-256: {sha256}"));
    findings.push(format!(
        "Đã kiểm tra {entry_count} mục, tổng giải nén {} MB",
        total_uncompressed_bytes / 1024 / 1024
    ));

    let safety_score = calculate_safety_score(
        &errors,
        &warnings,
        has_theme_descriptor,
        &theme_metadata,
        &modules,
        entry_count,
    );
    let safety_level = safety_level(safety_score);

    let (valid, status, message) = if !errors.is_empty() {
        (
            false,
            "failed",
            format!(
                "File bị từ chối: {} lỗi, điểm an toàn {safety_score}/100",
                errors.len()
            ),
        )
    } else if !warnings.is_empty() || safety_score < 90 {
        (
            true,
            "warning",
            format!(
                "File có thể upload nhưng cần Admin kiểm tra: {} cảnh báo, điểm {safety_score}/100",
                warnings.len()
            ),
        )
    } else {
        (
            true,
            "passed",
            format!(
                "File hợp lệ, đã kiểm tra {entry_count} mục, điểm an toàn {safety_score}/100"
            ),
        )
    };

    ValidationResult {
        valid,
        status,
        message,
        sha256,
        size_bytes,
        entry_count,
        total_uncompressed_bytes,
        safety_score,
        safety_level,
        metadata: theme_metadata,
        modules,
        findings,
        warnings,
        errors,
    }
}

fn verify_file_sha256(path: &Path, expected_sha256: &str) -> HashVerificationResult {
    let expected = expected_sha256.trim().to_ascii_lowercase();
    if expected.len() != 64
        || !expected
            .chars()
            .all(|character| character.is_ascii_hexdigit())
    {
        return HashVerificationResult {
            valid: false,
            matches: false,
            message: "SHA-256 dự kiến không hợp lệ hoặc đang để trống".to_string(),
            actual_sha256: String::new(),
            expected_sha256: expected,
            size_bytes: 0,
        };
    }

    let metadata = match path.metadata() {
        Ok(value) => value,
        Err(error) => {
            return HashVerificationResult {
                valid: false,
                matches: false,
                message: format!("Không đọc được file đã tải: {error}"),
                actual_sha256: String::new(),
                expected_sha256: expected,
                size_bytes: 0,
            }
        }
    };

    let actual = match calculate_sha256(path) {
        Ok(value) => value,
        Err(error) => {
            return HashVerificationResult {
                valid: false,
                matches: false,
                message: format!("Không tính được SHA-256 file đã tải: {error}"),
                actual_sha256: String::new(),
                expected_sha256: expected,
                size_bytes: metadata.len(),
            }
        }
    };

    let matches = constant_time_hex_equal(&actual, &expected);
    HashVerificationResult {
        valid: true,
        matches,
        message: if matches {
            "File tải xuống khớp đúng bản đã được duyệt".to_string()
        } else {
            "File tải xuống đã bị thay đổi hoặc không đúng bản được duyệt"
                .to_string()
        },
        actual_sha256: actual,
        expected_sha256: expected,
        size_bytes: metadata.len(),
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

fn module_definitions() -> &'static [(&'static str, &'static str)] {
    &[
        ("lockscreen", "Màn hình khóa"),
        ("icons", "Biểu tượng"),
        ("systemui", "System UI/Trung tâm điều khiển"),
        ("framework", "Framework hệ thống"),
        ("launcher", "Màn hình chính"),
        ("wallpaper", "Hình nền"),
        ("fonts", "Phông chữ"),
        ("bootanimation", "Ảnh động khởi động"),
        ("sounds", "Âm thanh/nhạc chuông"),
        ("clock", "Đồng hồ"),
    ]
}

fn initial_module_stats() -> BTreeMap<&'static str, ModuleAccumulator> {
    module_definitions()
        .iter()
        .map(|(key, _)| (*key, ModuleAccumulator::default()))
        .collect()
}

fn detect_modules(lower_name: &str) -> Vec<&'static str> {
    let mut modules = Vec::new();

    if path_matches(lower_name, &["lockscreen", "advance/lockscreen"]) {
        modules.push("lockscreen");
    }
    if path_matches(lower_name, &["icons", "icon/"]) {
        modules.push("icons");
    }
    if path_matches(
        lower_name,
        &[
            "com.android.systemui",
            "systemui",
            "statusbar",
            "control_center",
        ],
    ) {
        modules.push("systemui");
    }
    if path_matches(lower_name, &["framework-res", "framework/"]) {
        modules.push("framework");
    }
    if path_matches(lower_name, &["com.miui.home", "launcher", "home/"]) {
        modules.push("launcher");
    }
    if path_matches(lower_name, &["wallpaper", "wallpapers/"]) {
        modules.push("wallpaper");
    }
    if path_matches(lower_name, &["fonts", "font/"]) {
        modules.push("fonts");
    }
    if path_matches(lower_name, &["bootanimation", "bootanimation.zip"]) {
        modules.push("bootanimation");
    }
    if path_matches(
        lower_name,
        &[
            "ringtones",
            "ringtone",
            "alarms",
            "notifications",
            "sounds/",
        ],
    ) {
        modules.push("sounds");
    }
    if path_matches(lower_name, &["clock", "com.android.deskclock"]) {
        modules.push("clock");
    }

    modules
}

fn path_matches(path: &str, patterns: &[&str]) -> bool {
    patterns.iter().any(|pattern_ref| {
        let pattern = *pattern_ref;
        path == pattern
            || path.starts_with(&format!("{pattern}/"))
            || path.contains(&format!("/{pattern}/"))
            || path.ends_with(&format!("/{pattern}"))
            || path.contains(pattern)
    })
}

fn finalize_module_reports(
    stats: BTreeMap<&'static str, ModuleAccumulator>,
) -> Vec<ModuleReport> {
    module_definitions()
        .iter()
        .map(|(key, label)| {
            let stat = stats.get(*key).cloned().unwrap_or_default();
            let present = stat.entry_count > 0;
            ModuleReport {
                key: (*key).to_string(),
                label: (*label).to_string(),
                present,
                entry_count: stat.entry_count,
                total_bytes: stat.total_bytes,
                status: if present { "present" } else { "missing" },
                message: if present {
                    format!("Đã nhận diện {} mục", stat.entry_count)
                } else {
                    "Không có trong gói hoặc chưa nhận diện được".to_string()
                },
            }
        })
        .collect()
}

fn empty_module_reports() -> Vec<ModuleReport> {
    finalize_module_reports(initial_module_stats())
}

fn merge_metadata(metadata: &mut ThemeMetadata, xml: &str, source_file: &str) {
    set_if_empty(
        &mut metadata.title,
        extract_xml_value(
            xml,
            &["title", "name", "themeName", "theme_name"],
        ),
    );
    set_if_empty(
        &mut metadata.author,
        extract_xml_value(xml, &["author", "creator"]),
    );
    set_if_empty(
        &mut metadata.designer,
        extract_xml_value(xml, &["designer", "designBy", "design_by"]),
    );
    set_if_empty(
        &mut metadata.version,
        extract_xml_value(
            xml,
            &["version", "themeVersion", "theme_version"],
        ),
    );
    set_if_empty(
        &mut metadata.ui_version,
        extract_xml_value(xml, &["uiVersion", "ui_version", "ui-version"]),
    );
    set_if_empty(
        &mut metadata.platform,
        extract_xml_value(
            xml,
            &["platform", "target", "osVersion", "os_version"],
        ),
    );
    set_if_empty(
        &mut metadata.description,
        extract_xml_value(xml, &["description", "summary"]),
    );

    if metadata.source_file.is_empty()
        && (!metadata.title.is_empty()
            || !metadata.author.is_empty()
            || !metadata.version.is_empty())
    {
        metadata.source_file = source_file.to_string();
    }
}

fn set_if_empty(target: &mut String, value: Option<String>) {
    if target.is_empty() {
        if let Some(value) = value {
            let clean = sanitize_metadata_value(&value);
            if !clean.is_empty() {
                *target = clean;
            }
        }
    }
}

fn extract_xml_value(xml: &str, tags: &[&str]) -> Option<String> {
    let lower = xml.to_ascii_lowercase();

    for tag in tags {
        let tag_lower = tag.to_ascii_lowercase();
        let opening = format!("<{tag_lower}");
        let closing = format!("</{tag_lower}>");
        let mut offset = 0_usize;

        while let Some(relative_start) = lower[offset..].find(&opening) {
            let start = offset + relative_start;
            let after_name = start + opening.len();
            let next = lower.as_bytes().get(after_name).copied();
            if !matches!(
                next,
                Some(b'>')
                    | Some(b' ')
                    | Some(b'\t')
                    | Some(b'\r')
                    | Some(b'\n')
            ) {
                offset = after_name;
                continue;
            }

            let open_end = lower[after_name..].find('>')? + after_name;
            let value_start = open_end + 1;
            let close_relative = lower[value_start..].find(&closing)?;
            let value_end = value_start + close_relative;
            return Some(xml[value_start..value_end].to_string());
        }
    }

    None
}

fn sanitize_metadata_value(value: &str) -> String {
    let value = value
        .replace("<![CDATA[", "")
        .replace("]]>", "")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'");

    let without_tags = strip_xml_tags(&value);
    without_tags
        .split_whitespace()
        .collect::<Vec<_>>()
        .join(" ")
        .chars()
        .take(300)
        .collect()
}

fn strip_xml_tags(value: &str) -> String {
    let mut output = String::with_capacity(value.len());
    let mut in_tag = false;

    for character in value.chars() {
        match character {
            '<' => in_tag = true,
            '>' => in_tag = false,
            _ if !in_tag => output.push(character),
            _ => {}
        }
    }

    output
}

fn contains_unsafe_xml_declaration(xml: &str) -> bool {
    let lower = xml.to_ascii_lowercase();
    lower.contains("<!doctype") || lower.contains("<!entity")
}

fn calculate_safety_score(
    errors: &[String],
    warnings: &[String],
    has_descriptor: bool,
    metadata: &ThemeMetadata,
    modules: &[ModuleReport],
    entry_count: usize,
) -> u8 {
    let mut score = 100_i32;
    score -= (errors.len() as i32 * 28).min(85);
    score -= (warnings.len() as i32 * 5).min(30);

    if !has_descriptor {
        score -= 10;
    }
    if metadata.title.is_empty() {
        score -= 5;
    }
    if modules.iter().all(|module| !module.present) {
        score -= 12;
    }
    if entry_count > 10_000 {
        score -= 8;
    }

    score = score.clamp(0, 100);

    if !errors.is_empty() {
        score = score.min(49);
    } else if !warnings.is_empty() {
        score = score.min(89);
    } else {
        score = score.max(90);
    }

    score as u8
}

fn safety_level(score: u8) -> &'static str {
    match score {
        90..=100 => "excellent",
        75..=89 => "good",
        60..=74 => "caution",
        _ => "danger",
    }
}

fn constant_time_hex_equal(actual: &str, expected: &str) -> bool {
    if actual.len() != expected.len() {
        return false;
    }

    actual
        .as_bytes()
        .iter()
        .zip(expected.as_bytes())
        .fold(0_u8, |difference, (left, right)| {
            difference | (left ^ right)
        })
        == 0
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
        ".apk", ".dex", ".exe", ".dll", ".so", ".com", ".scr", ".bat",
        ".cmd", ".ps1", ".msi", ".jar", ".sh",
    ];

    BLOCKED
        .iter()
        .any(|extension| lower_name.ends_with(extension))
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

    #[test]
    fn metadata_is_extracted() {
        let xml = r#"<MIUI-Theme><title>M4X Dark</title><author>Minh Dan</author><version>2.0</version><uiVersion>15</uiVersion></MIUI-Theme>"#;
        let mut metadata = ThemeMetadata::default();
        merge_metadata(&mut metadata, xml, "description.xml");
        assert_eq!(metadata.title, "M4X Dark");
        assert_eq!(metadata.author, "Minh Dan");
        assert_eq!(metadata.version, "2.0");
        assert_eq!(metadata.ui_version, "15");
    }

    #[test]
    fn hash_compare_works() {
        assert!(constant_time_hex_equal("abcd", "abcd"));
        assert!(!constant_time_hex_equal("abcd", "abce"));
    }

    #[test]
    fn modules_are_detected() {
        assert!(
            detect_modules("lockscreen/advance/manifest.xml")
                .contains(&"lockscreen")
        );
        assert!(
            detect_modules("com.android.systemui/theme_values.xml")
                .contains(&"systemui")
        );
        assert!(detect_modules("icons").contains(&"icons"));
    }
}
