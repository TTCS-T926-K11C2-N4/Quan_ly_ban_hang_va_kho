(function () {
    var form = document.getElementById("account-form");
    var roleOptions = document.getElementById("role-options");
    var allowNoRoles = form.hasAttribute("data-allow-no-roles");

    // Cùng quy tắc với AccountService.validateNewAccount / validateAccountUpdate
    // (server vẫn kiểm tra lại, kể cả trùng tên/email)
    var USERNAME_PATTERN = /^[A-Za-z0-9._-]{3,50}$/;
    var EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    var PHONE_PATTERN = /^\d{10}$/;
    var WAREHOUSE_ROLES = ["WH_MANAGER", "WAREHOUSE"];
    var SALES_REP_ROLE = "SALES_REP";

    var fields = [
        { input: form.elements.fullName, errorId: "full-name-error", check: function (value) {
            return value ? "" : "Vui lòng nhập họ và tên.";
        } },
        { input: form.elements.username, errorId: "username-error", check: function (value) {
            // Màn Sửa: tên đăng nhập chỉ xem, không kiểm tra
            if (form.elements.username.readOnly) {
                return "";
            }
            if (!value) {
                return "Vui lòng nhập tên đăng nhập.";
            }
            return USERNAME_PATTERN.test(value) ? ""
                : "Tên đăng nhập gồm 3–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.";
        } },
        { input: form.elements.email, errorId: "email-error", check: function (value) {
            if (!value) {
                return "Vui lòng nhập email.";
            }
            return EMAIL_PATTERN.test(value) ? "" : "Email không đúng định dạng.";
        } },
        { input: form.elements.phone, errorId: "phone-error", check: function (value) {
            if (!value) {
                return "Vui lòng nhập số điện thoại.";
            }
            return PHONE_PATTERN.test(value) ? "" : "Số điện thoại phải gồm đúng 10 chữ số.";
        } },
        { input: form.elements.warehouseId, errorId: "warehouse-error", check: function (value) {
            var needsWarehouse = selectedRoles().some(function (code) {
                return WAREHOUSE_ROLES.indexOf(code) >= 0;
            });
            return needsWarehouse && !value ? "Vui lòng chọn kho phụ trách." : "";
        } },
        { input: form.elements.regionId, errorId: "region-error", check: function (value) {
            return selectedRoles().indexOf(SALES_REP_ROLE) >= 0 && !value ? "Vui lòng chọn địa bàn phụ trách." : "";
        } }
    ];

    function selectedRoles() {
        return Array.prototype.filter.call(form.elements.roleCodes, function (checkbox) {
            return checkbox.checked;
        }).map(function (checkbox) {
            return checkbox.value;
        });
    }

    function showError(errorId, message, group) {
        var error = document.getElementById(errorId);
        error.textContent = message;
        error.hidden = !message;
        group.classList.toggle(group === roleOptions ? "role-options--invalid" : "form-group--invalid", Boolean(message));
    }

    function validateField(field) {
        var message = field.check(field.input.value.trim());
        showError(field.errorId, message, field.input.closest(".form-group"));
        field.input.setAttribute("aria-invalid", String(Boolean(message)));
        return !message;
    }

    function validateRoles() {
        var message = selectedRoles().length || allowNoRoles ? "" : "Vui lòng chọn ít nhất một vai trò.";
        showError("role-codes-error", message, roleOptions);
        return !message;
    }

    form.addEventListener("submit", function (event) {
        var firstInvalid = null;
        fields.forEach(function (field) {
            if (!validateField(field) && !firstInvalid) {
                firstInvalid = field.input;
            }
        });
        if (!validateRoles() && !firstInvalid) {
            firstInvalid = form.elements.roleCodes[0];
        }
        if (firstInvalid) {
            event.preventDefault();
            firstInvalid.focus();
        }
    });

    // Sửa xong một ô đang báo lỗi thì ẩn lỗi ngay; đổi vai trò thì kiểm tra lại kho/địa bàn đang báo lỗi
    fields.forEach(function (field) {
        field.input.addEventListener(field.input.tagName === "SELECT" ? "change" : "input", function () {
            if (field.input.getAttribute("aria-invalid") === "true") {
                validateField(field);
            }
        });
    });

    roleOptions.addEventListener("change", function () {
        if (roleOptions.classList.contains("role-options--invalid")) {
            validateRoles();
        }
        fields.slice(-2).forEach(function (field) {
            if (field.input.getAttribute("aria-invalid") === "true") {
                validateField(field);
            }
        });
    });
})();
