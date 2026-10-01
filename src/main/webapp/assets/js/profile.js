(function () {
    var form = document.getElementById("profile-form");

    // Cùng quy tắc với ProfileService.validate và PhoneUtil (server vẫn kiểm tra lại)
    var VIETNAM_MOBILE = /^0[35789]\d{8}$/;

    function normalizePhone(value) {
        var digits = value.replace(/[\s.\-]/g, "");
        return digits.indexOf("+84") === 0 ? "0" + digits.substring(3) : digits;
    }

    var fields = [
        { input: form.elements.fullName, errorId: "full-name-error", check: function (value) {
            return value ? "" : "Vui lòng nhập họ và tên.";
        } },
        { input: form.elements.phone, errorId: "phone-error", check: function (value) {
            if (!value) {
                return "Vui lòng nhập số điện thoại.";
            }
            return VIETNAM_MOBILE.test(normalizePhone(value)) ? ""
                : "Số điện thoại không đúng định dạng Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).";
        } }
    ];

    function validateField(field) {
        var message = field.check(field.input.value.trim());
        var error = document.getElementById(field.errorId);
        error.textContent = message;
        error.hidden = !message;
        field.input.closest(".form-group").classList.toggle("form-group--invalid", Boolean(message));
        field.input.setAttribute("aria-invalid", String(Boolean(message)));
        return !message;
    }

    form.addEventListener("submit", function (event) {
        var firstInvalid = null;
        fields.forEach(function (field) {
            if (!validateField(field) && !firstInvalid) {
                firstInvalid = field.input;
            }
        });
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus();
        }
    });

    // Sửa xong một ô đang báo lỗi thì ẩn lỗi ngay
    fields.forEach(function (field) {
        field.input.addEventListener("input", function () {
            if (field.input.getAttribute("aria-invalid") === "true") {
                validateField(field);
            }
        });
    });
})();
