# M4X Theme v3.8.7 — Theme Bottom Action Fix

## Sửa theo video người dùng

- Nút dưới cùng bị thanh điều hướng Android che.
- Nút **Tạo** không thuộc luồng mua theme và gây khó hiểu.

## Đã sửa

- Bỏ hoàn toàn nút **Tạo**.
- Chỉ giữ một nút hành động chính:
  - `Mua bằng ... coin`
  - `Nhận miễn phí`
  - `Tải theme`
- Thêm `navigationBarsPadding()` để nút luôn nằm trên thanh điều hướng.
- Nút được mở rộng toàn chiều ngang và tăng chiều cao tối thiểu.
- Không cần SQL mới.
