# M4X Theme v3.8.9 — Fishing Win Result Fix

## Lỗi
Boss đã về `0 máu` nhưng ứng dụng vẫn hiện **Lỗi trận câu**.

## Nguyên nhân
Kết quả thắng/thua và RPC xác nhận máy chủ có thể chạy sát nhau. Thời gian
gửi lên máy chủ cũng có thể lệch nhẹ khỏi giới hạn của lượt câu.

## Đã sửa
- Thêm trạng thái `RESOLVING` để khóa trận ngay khi xác nhận kết quả.
- Boss 0 máu luôn được ưu tiên xử thắng trước timeout.
- Chuẩn hóa thời gian gửi lên máy chủ trong khoảng `minReelMs..maxReelMs`.
- Tự thử lại một lần khi máy chủ báo thời gian kéo cá quá nhanh/không hợp lệ.
- Nếu lỗi khác xảy ra, hiển thị đúng lý do thay vì câu thông báo chung.
- Không cần SQL mới.
