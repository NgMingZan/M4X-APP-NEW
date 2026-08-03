# Kích hoạt Firebase thật cho M4X Theme

Bản APK mặc định chạy bằng Room/local database để có thể build ngay mà không cần khóa bí mật.

1. Tạo Firebase project và Android app có package `com.aistudio.m4xtheme.app`.
2. Tải `google-services.json` vào thư mục `app/`.
3. Bật Authentication (Email/Password), Firestore, Storage và Cloud Messaging.
4. Bỏ comment các dependency Firebase Auth/Firestore trong `app/build.gradle.kts`.
5. Thay `M4xRepository` bằng implementation Firebase hoặc tạo `FirebaseM4xRepository` cùng interface.
6. Deploy luật bằng Firebase CLI: `firebase deploy --only firestore:rules,storage`.
7. Gán custom claim `admin: true` cho tài khoản quản trị bằng Admin SDK.

Không commit `google-services.json`, keystore hay khóa dịch vụ công khai lên repository public.
