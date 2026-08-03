# M4X Theme v3.0.3

- Thêm icon ứng dụng M4X mới.
- Admin tạo Giftcode có giới hạn số lượt nhập.
- Admin đặt thời hạn Giftcode theo số giờ.
- Giftcode tự hết hạn qua cột `expires_at`.
- Mỗi tài khoản chỉ nhận một Giftcode một lần nhờ ràng buộc `giftcode_claims`.
- Không cần chạy lại SQL nếu đã cài Full Supabase V3.
