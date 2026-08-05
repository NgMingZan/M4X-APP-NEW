# M4X Theme v4.1.1 — Fishing Shop Refresh Fix

## Nguyên nhân

Danh mục Fishing chỉ được tải một lần khi mở dịch vụ. Nếu Admin thêm cần mới
trên Supabase khi app đang chạy, trang Cửa hàng vẫn dùng dữ liệu cũ.

## Đã sửa

- Mỗi lần mở **Cửa hàng cần câu** sẽ tải lại dữ liệu Supabase.
- Mỗi lần mở **Bản đồ** hoặc **Kho cá** cũng tải lại dữ liệu online.
- Kèm SQL kích hoạt lại 5 cần mới.
- Không thay đổi giá, sức mạnh hoặc quyền sở hữu cần câu.
