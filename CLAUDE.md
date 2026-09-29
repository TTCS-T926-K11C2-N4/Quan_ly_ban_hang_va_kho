# CLAUDE.md

File này hướng dẫn Claude Code cách làm việc trong repo này. Hãy đọc kỹ trước khi bắt đầu bất kỳ tác vụ nào.

## Tổng quan dự án

- **Tên dự án:** DU_AN_TTCS
- **Mục tiêu:** _(TODO: mô tả ngắn dự án làm gì, cho ai)_
- **Trạng thái:** mới khởi tạo, chưa có mã nguồn. Cập nhật mục này khi có công nghệ và cấu trúc chính thức.

## Công nghệ

- **Backend:** Java Servlet (Jakarta/Java EE), chạy trên servlet container như Tomcat.
- **Frontend:** HTML + CSS (có thể thêm JavaScript thuần khi cần tương tác; không đưa framework vào nếu chưa được yêu cầu).
- **Database:** MySQL, truy cập qua JDBC.
- _(TODO: điền phiên bản JDK, Tomcat, MySQL, công cụ build Maven/Gradle khi chốt.)_

### Hướng dẫn theo tầng

**Backend (Java Servlet)**
- Tách lớp rõ ràng: Servlet (nhận request, trả response) → Service (logic nghiệp vụ) → DAO (truy cập DB) → Model/Entity.
- Servlet không chứa SQL và không chứa logic nghiệp vụ phức tạp.
- Luôn đặt encoding UTF-8 cho request và response để hiển thị đúng tiếng Việt.
- Xử lý lỗi rõ ràng: trả mã HTTP phù hợp, không để lộ stack trace ra người dùng.
- Đóng `Connection`, `PreparedStatement`, `ResultSet` bằng try-with-resources.

**Frontend (HTML + CSS)**
- Dùng HTML ngữ nghĩa (`header`, `nav`, `main`, `section`, `footer`, `form`, `label`...).
- CSS đặt trong file riêng, không viết style inline. Đặt tên class nhất quán (ví dụ BEM hoặc kebab-case).
- Giao diện phải responsive, kiểm tra được trên màn hình nhỏ.
- Khai báo `<meta charset="UTF-8">` và `<meta name="viewport" ...>` ở mọi trang.
- Nếu dùng JSP để render dữ liệu động, không nhúng logic nghiệp vụ hay SQL vào JSP.

**Database (MySQL)**
- Dùng `utf8mb4` cho database và bảng để hỗ trợ tiếng Việt và emoji.
- Đặt tên bảng và cột theo `snake_case`; mỗi bảng có khóa chính rõ ràng.
- Khai báo khóa ngoại và index cho các cột hay tra cứu.
- Lưu script tạo schema và dữ liệu mẫu trong thư mục riêng (ví dụ `database/`), không sửa DB thủ công mà không ghi lại.
- **Bắt buộc dùng `PreparedStatement`** với tham số `?`, không nối chuỗi SQL để tránh SQL injection.
- Không hardcode thông tin kết nối DB trong code; đọc từ file cấu hình không commit hoặc biến môi trường.

## Cơ sở dữ liệu

Thiết kế CSDL: xem OMS_schema_mysql.sql (69 bảng, MySQL 8).

## Môi trường

- Hệ điều hành: Windows 11. Shell chính là PowerShell; Bash (Git Bash) cũng dùng được.
- Dùng cú pháp đúng với shell đang chạy. PowerShell không có `&&`, `head`, `tail`, `rm -rf`...
- Đường dẫn dùng dấu `\` trong PowerShell, `/` trong Bash.
- Thư mục làm việc: `d:\DU_AN_TTCS`.

## Lệnh thường dùng

_(TODO: điền khi có project.)_

```powershell
# Build (nếu dùng Maven)
mvn clean package

# Chạy test
mvn test

# Deploy: copy file .war vào thư mục webapps của Tomcat rồi khởi động Tomcat
```

_(TODO: cập nhật lại nếu dùng Gradle hoặc chạy qua IDE.)_

## Cấu trúc thư mục

_(TODO: mô tả các thư mục chính khi có.)_

## Quy ước code

- Code mới phải giống phong cách của code xung quanh: cách đặt tên, mật độ comment, idiom.
- Ưu tiên giải pháp đơn giản nhất đáp ứng yêu cầu. Không thêm abstraction, tính năng hay cấu hình mà chưa ai yêu cầu.
- Chỉ thêm comment khi giải thích *tại sao*, không giải thích *cái gì*.
- Không để lại code chết, `console.log`/print debug hay TODO không rõ chủ.
- Không sửa những phần không liên quan đến tác vụ (không refactor, format lại hàng loạt).

## Quy trình làm việc

1. **Hiểu trước, sửa sau:** đọc file liên quan và tìm code có sẵn để tái sử dụng trước khi viết mới.
2. **Việc mơ hồ hoặc cần quyết định:** dừng lại và hỏi người dùng trước khi làm (xem mục "Hỏi lại người dùng" bên dưới). Việc nhỏ, rõ ràng thì làm luôn.
3. **Kiểm chứng:** sau khi sửa, chạy build/test/lint (khi có) để xác nhận. Nếu không chạy được thì nói rõ, không khẳng định "đã xong" khi chưa kiểm tra.
4. **Báo cáo trung thực:** test fail, bước bị bỏ qua hay điều chưa chắc chắn đều phải nói thẳng.

## Hỏi lại người dùng

**Luôn hỏi trước khi làm** khi gặp một trong các tình huống sau. Không tự đoán rồi làm tiếp.

- **Yêu cầu mơ hồ:** có nhiều cách hiểu hợp lý, thiếu thông tin cần thiết (ví dụ chưa rõ trường dữ liệu, luồng nghiệp vụ, giao diện mong muốn).
- **Cần đưa ra quyết định:** chọn giữa nhiều hướng thiết kế hoặc thư viện, thay đổi cấu trúc DB, thêm phụ thuộc mới, đổi kiến trúc, hay bất kỳ lựa chọn nào ảnh hưởng đến phần còn lại của dự án.
- **Thao tác khó hoàn tác:** xóa file hoặc dữ liệu, sửa/xóa bảng MySQL, ghi đè code đang có.

Cách hỏi:
- Hỏi ngắn gọn, cụ thể. Đưa ra 2–3 phương án kèm ưu nhược điểm ngắn và **nêu phương án bạn khuyên dùng**.
- Gom các câu hỏi liên quan vào một lần hỏi, không hỏi lắt nhắt từng câu.
- Không hỏi những việc có thể tự xác minh bằng cách đọc code, hoặc những việc đã có quy ước sẵn trong file này.
- Nhận được câu trả lời thì làm theo, không hỏi lại điều người dùng đã quyết.

## Git

- Chỉ commit hoặc push khi người dùng yêu cầu.
- Không dùng `--no-verify`, `--force`, `reset --hard` hay các thao tác phá hủy khác nếu chưa được đồng ý.
- Commit message ngắn gọn, nêu rõ *tại sao* thay đổi.
- Không commit file bí mật (`.env`, khóa API, token).

## Bảo mật

- Không hardcode secret, mật khẩu, token vào code hoặc file cấu hình được commit.
- Không gửi dữ liệu dự án hoặc thông tin cá nhân tới dịch vụ bên ngoài nếu chưa được yêu cầu.
- Validate dữ liệu đầu vào ở các ranh giới hệ thống (input người dùng, API ngoài).
- Chống SQL injection (dùng `PreparedStatement`) và XSS (escape dữ liệu khi in ra HTML/JSP, ví dụ `<c:out>`).
- Mật khẩu phải băm (bcrypt hoặc tương đương) trước khi lưu, không lưu dạng thô.

## Figma (MCP)

Repo có cấu hình MCP Figma trong [.mcp.json](.mcp.json).

- Khi người dùng gửi link `figma.com` hoặc yêu cầu triển khai thiết kế thành code, dùng các tool Figma (`get_design_context`, `get_screenshot`...) thay vì đoán.
- Bám sát design token, khoảng cách, màu và component có sẵn trong thiết kế và design system của dự án.

## Giao tiếp

- Trả lời bằng **tiếng Việt**, trừ khi người dùng viết bằng ngôn ngữ khác.
- Giữ nguyên thuật ngữ kỹ thuật, tên hàm/biến, đường dẫn và câu lệnh bằng tiếng Anh.
- Trả lời ngắn gọn, đi thẳng vào kết quả. Không lặp lại những gì đã nói.
- Khi nhắc tới file hoặc dòng code, dùng link markdown dạng [tên file](đường/dẫn) để bấm được.
