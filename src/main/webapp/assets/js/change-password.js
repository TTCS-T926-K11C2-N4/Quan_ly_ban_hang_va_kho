(function () {
    var form = document.getElementById("change-password-form");
    var currentPassword = document.getElementById("currentPassword");
    var newPassword = document.getElementById("newPassword");
    var confirmPassword = document.getElementById("confirmPassword");

    var errorIds = {
        currentPassword: "current-password-error",
        newPassword: "new-password-error",
        confirmPassword: "confirm-password-error"
    };

    // \p{L} để chữ có dấu tiếng Việt cũng được tính là chữ
    var HAS_LETTER = /\p{L}/u;
    var HAS_DIGIT = /\d/;

    function getMessage(input) {
        var value = input.value;
        if (input === currentPassword) {
            return value ? "" : "Vui lòng nhập mật khẩu hiện tại.";
        }
        if (input === newPassword) {
            if (!value) {
                return "Vui lòng nhập mật khẩu mới.";
            }
            if (value.length < 8 || value.length > 64 || !HAS_LETTER.test(value) || !HAS_DIGIT.test(value)) {
                return "Mật khẩu mới phải có tối thiểu 8 ký tự, gồm cả chữ và số.";
            }
            return "";
        }
        if (!value) {
            return "Vui lòng xác nhận mật khẩu mới.";
        }
        return value === newPassword.value ? "" : "Mật khẩu xác nhận không khớp.";
    }

    function showError(input, message) {
        var error = document.getElementById(errorIds[input.id]);
        error.textContent = message;
        error.hidden = !message;
        input.closest(".form-field__control").classList.toggle("form-field__control--invalid", Boolean(message));
        input.setAttribute("aria-invalid", String(Boolean(message)));
    }

    function validate(input) {
        var message = getMessage(input);
        showError(input, message);
        return !message;
    }

    function isInvalid(input) {
        return input.getAttribute("aria-invalid") === "true";
    }

    form.addEventListener("submit", function (event) {
        var firstInvalid = null;
        [currentPassword, newPassword, confirmPassword].forEach(function (input) {
            if (!validate(input) && !firstInvalid) {
                firstInvalid = input;
            }
        });
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus();
        }
    });

    // Chỉ kiểm tra lại khi đang có lỗi, để không báo lỗi lúc người dùng mới bắt đầu gõ
    [currentPassword, newPassword, confirmPassword].forEach(function (input) {
        input.addEventListener("input", function () {
            if (isInvalid(input)) {
                validate(input);
            }
            if (input === newPassword && isInvalid(confirmPassword)) {
                validate(confirmPassword);
            }
        });
    });
})();
