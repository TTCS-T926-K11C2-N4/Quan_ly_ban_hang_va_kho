// Nhà cung cấp (S2-09): lưu lỗi thì trang tải lại ở đầu, cuộn xuống form và đặt con trỏ vào ô lỗi đầu tiên.
// Nút xoá / ngừng giao dịch hỏi xác nhận dùng chung categories.js.
(function () {
    var form = document.getElementById("supplier-form");
    if (!form || form.getAttribute("data-has-errors") !== "true") {
        return;
    }
    form.scrollIntoView({ block: "start" });
    var invalid = form.querySelector(".form-group--invalid .form-group__control");
    if (invalid) {
        invalid.focus({ preventScroll: true });
    }
})();
