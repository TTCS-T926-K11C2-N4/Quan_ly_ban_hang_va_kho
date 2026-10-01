(function () {
    var form = document.getElementById("supplier-form");

    // Cùng quy tắc với SupplierService.validateFields (server vẫn kiểm tra lại, kể cả trùng mã)
    var CODE_PATTERN = /^[A-Z0-9._-]{1,30}$/;
    var TAX_CODE_PATTERN = /^\d{10}(-\d{3})?$/;
    var PHONE_PATTERN = /^\+?\d{8,15}$/;
    var EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    var fields = [
        { input: form.elements.code, errorId: "code-error", check: function (value) {
            if (!value) {
                return "Vui lòng nhập mã nhà cung cấp.";
            }
            return CODE_PATTERN.test(value.toUpperCase()) ? ""
                : "Mã nhà cung cấp gồm tối đa 30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.";
        } },
        { input: form.elements.name, errorId: "name-error", check: function (value) {
            return value ? "" : "Vui lòng nhập tên nhà cung cấp.";
        } },
        { input: form.elements.taxCode, errorId: "tax-code-error", check: function (value) {
            return !value || TAX_CODE_PATTERN.test(value) ? ""
                : "Mã số thuế gồm 10 số, hoặc 10 số kèm mã đơn vị phụ thuộc (vd 0101234567-001).";
        } },
        { input: form.elements.phone, errorId: "phone-error", check: function (value) {
            return !value || PHONE_PATTERN.test(value.replace(/[\s.\-]/g, "")) ? "" : "Số điện thoại gồm 8–15 chữ số.";
        } },
        { input: form.elements.email, errorId: "email-error", check: function (value) {
            return !value || EMAIL_PATTERN.test(value) ? "" : "Email không đúng định dạng.";
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
