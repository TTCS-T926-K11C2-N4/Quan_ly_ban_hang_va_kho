// Nút mắt: hiện/ẩn ô mật khẩu mà nó trỏ tới (data-password-toggle = id của ô).
// Dùng chung cho trang Đổi mật khẩu, Đặt lại mật khẩu.
(function () {
    document.querySelectorAll("[data-password-toggle]").forEach(function (button) {
        var input = document.getElementById(button.getAttribute("data-password-toggle"));
        button.addEventListener("click", function () {
            var isHidden = input.type === "password";
            input.type = isHidden ? "text" : "password";
            button.setAttribute("aria-pressed", String(isHidden));
        });
    });
})();
