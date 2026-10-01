(function () {
    var form = document.getElementById("register-form");

    // Cùng quy tắc với AccountService.validateRegistration (server vẫn kiểm tra lại, kể cả trùng tên/email)
    var USERNAME_PATTERN = /^[A-Za-z0-9._-]{3,50}$/;
    var EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    // \p{L} để chữ có dấu tiếng Việt cũng được tính là chữ
    var HAS_LETTER = /\p{L}/u;
    var HAS_DIGIT = /\d/;

    var fullName = form.elements.fullName;
    var email = form.elements.email;
    var username = form.elements.username;
    var password = form.elements.password;
    var confirmPassword = form.elements.confirmPassword;
    var acceptTerms = form.elements.acceptTerms;

    var rules = [
        { input: fullName, errorId: "full-name-error", check: function () {
            return fullName.value.trim() ? "" : "Vui lòng nhập họ và tên.";
        } },
        { input: email, errorId: "email-error", check: function () {
            var value = email.value.trim();
            if (!value) {
                return "Vui lòng nhập email.";
            }
            return EMAIL_PATTERN.test(value) ? "" : "Email không đúng định dạng.";
        } },
        { input: username, errorId: "username-error", check: function () {
            var value = username.value.trim();
            if (!value) {
                return "Vui lòng nhập tên đăng nhập.";
            }
            return USERNAME_PATTERN.test(value) ? ""
                : "Tên đăng nhập gồm 3–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.";
        } },
        { input: password, errorId: "password-error", check: function () {
            var value = password.value;
            if (!value) {
                return "Vui lòng nhập mật khẩu.";
            }
            var valid = value.length >= 8 && value.length <= 64 && HAS_LETTER.test(value) && HAS_DIGIT.test(value);
            return valid ? "" : "Mật khẩu phải có 8–64 ký tự, gồm cả chữ và số.";
        } },
        { input: confirmPassword, errorId: "confirm-password-error", check: function () {
            if (!confirmPassword.value) {
                return "Vui lòng nhập lại mật khẩu.";
            }
            return confirmPassword.value === password.value ? "" : "Mật khẩu xác nhận không khớp.";
        } },
        { input: acceptTerms, errorId: "accept-terms-error", check: function () {
            return acceptTerms.checked ? "" : "Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật.";
        } }
    ];

    function validate(rule) {
        var message = rule.check();
        var error = document.getElementById(rule.errorId);
        error.textContent = message;
        error.hidden = !message;
        var wrapper = rule.input === acceptTerms ? rule.input.closest(".form-check") : rule.input.closest(".form-field__control");
        wrapper.classList.toggle(rule.input === acceptTerms ? "form-check--invalid" : "form-field__control--invalid", Boolean(message));
        rule.input.setAttribute("aria-invalid", String(Boolean(message)));
        return !message;
    }

    form.addEventListener("submit", function (event) {
        var firstInvalid = null;
        rules.forEach(function (rule) {
            if (!validate(rule) && !firstInvalid) {
                firstInvalid = rule.input;
            }
        });
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus();
        }
    });

    // Chỉ kiểm tra lại ô đang báo lỗi, để không báo lỗi lúc người dùng mới bắt đầu gõ
    rules.forEach(function (rule) {
        rule.input.addEventListener(rule.input === acceptTerms ? "change" : "input", function () {
            if (rule.input.getAttribute("aria-invalid") === "true") {
                validate(rule);
            }
            if (rule.input === password && confirmPassword.getAttribute("aria-invalid") === "true") {
                validate(rules[4]);
            }
        });
    });
})();
