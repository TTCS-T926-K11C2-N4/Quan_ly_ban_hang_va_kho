# Quy tắc áp dụng chiết khấu theo sản lượng (S3-01)

Tài liệu này mô tả cách hệ thống tính chiết khấu khi tạo đơn hàng (S3-09) từ các chính sách khai báo ở
**Sản phẩm → Chính sách chiết khấu**.

## 1. Một chính sách gồm những gì

| Thành phần | Ý nghĩa |
|---|---|
| Áp cho | **Một SKU**, hoặc **một nhóm hàng**. Chính sách theo nhóm hàng áp cho mọi sản phẩm trong nhóm đó và trong các nhóm con của nó. |
| Nhóm khách hàng | Chỉ đại lý thuộc nhóm này được hưởng. Để trống thì áp cho mọi nhóm. |
| Cách tính | **Phần trăm** trên tiền hàng của dòng, hoặc **số tiền trên một đơn vị cơ sở** (VNĐ / lon, VNĐ / kg...). |
| Bậc chiết khấu | Mỗi bậc: *mua từ X (đơn vị cơ sở) trở lên thì được Y*. Một chính sách có 1–10 bậc. |
| Thời gian áp dụng | Từ ngày bắt đầu đến ngày kết thúc (để trống ngày kết thúc là không thời hạn). |
| Trạng thái | Chính sách đang tắt ("Ngừng áp dụng") thì không được tính. |

Số lượng so với bậc luôn tính theo **đơn vị cơ sở**: 2 Thùng × 24 Lon = 48 Lon (S2-07).

## 2. Cách tính chiết khấu cho một dòng hàng

Hệ thống xét từng dòng hàng của đơn, theo ngày tạo đơn và nhóm khách hàng của đại lý:

1. Lấy các chính sách **đang áp dụng** (đang bật, hôm nay nằm trong thời gian áp dụng), đúng nhóm khách hàng
   của đại lý hoặc áp cho mọi nhóm.
2. Giữ lại chính sách khớp dòng hàng: đúng SKU, hoặc sản phẩm thuộc nhóm hàng (kể cả nhóm con) của chính sách.
3. Với mỗi chính sách còn lại, chọn **bậc cao nhất mà số lượng của dòng đạt tới**. Chưa đạt bậc thấp nhất thì
   chính sách đó không áp cho dòng này.
4. Tính số tiền chiết khấu theo bậc đã chọn:
   - Phần trăm: tiền hàng của dòng × Y%.
   - Số tiền trên đơn vị: Y × số lượng theo đơn vị cơ sở.
   - Làm tròn tới đồng, và không vượt quá tiền hàng của dòng.

## 3. Nhiều chính sách cùng áp dụng: lấy chính sách có lợi nhất cho khách

Nếu sau bước 2–4 có **nhiều chính sách** cùng áp được cho một dòng hàng, hệ thống **chọn chính sách cho số tiền
chiết khấu lớn nhất** cho dòng đó. Các chính sách **không cộng dồn**. Mỗi dòng hàng chọn riêng, nên hai dòng
trong cùng một đơn có thể dùng hai chính sách khác nhau.

Dòng đơn hàng lưu lại chính sách đã dùng và số tiền chiết khấu lúc tạo đơn. Sửa hoặc ngừng chính sách sau đó
**không làm thay đổi** đơn đã tạo.

### Ví dụ

Đại lý cấp 1 mua **3 Thùng bia lon** (1 Thùng = 24 Lon → 72 Lon), giá 240.000 đ / Thùng, tiền hàng 720.000 đ.

| Chính sách | Bậc đạt tới | Chiết khấu |
|---|---|---|
| CK-0001: SKU bia lon, %: từ 48 lon 2%, từ 96 lon 4% | Từ 48 lon: 2% | 720.000 × 2% = **14.400 đ** |
| CK-0002: nhóm "Bia" (gồm Bia lon), tiền/đơn vị: từ 24 lon 250 đ/lon | Từ 24 lon: 250 đ | 250 × 72 = **18.000 đ** |

Hệ thống chọn **CK-0002**: chiết khấu 18.000 đ, tổng phải thu của dòng là 702.000 đ.

## 4. Xoá và ngừng chính sách

- Chính sách **chưa có đơn hàng dùng** thì xoá được.
- Chính sách **đã có đơn hàng dùng** thì không xoá được, chỉ chuyển sang "Ngừng áp dụng" để đơn cũ vẫn tra được
  chính sách đã áp. Muốn dùng lại thì bấm "Áp dụng lại".
- Mọi thao tác thêm, sửa, xoá, ngừng, áp dụng lại đều được ghi vào Nhật ký thao tác.

Mã nguồn liên quan: `SalesOrderService.priceLine` / `discountOf` (tính và chọn chiết khấu),
`SalesOrderDao.findPricingRules` (đọc chính sách đang áp dụng), `DiscountPolicyService` (khai báo chính sách).
