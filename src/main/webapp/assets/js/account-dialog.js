(function () {
    // Hộp thoại Khóa / Mở khóa được render từ server nên "đóng" = chuyển về trang chi tiết
    var dialogForm = document.querySelector("form[data-cancel-url]");
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            window.location.href = dialogForm.getAttribute("data-cancel-url");
        }
    });

    var form = document.getElementById("lock-account-form");
    if (!form) {
        return;
    }
    var reason = form.elements.lockReason;
    var handover = form.elements.handoverUserId;
    var minLength = parseInt(form.getAttribute("data-min-reason-length"), 10);

    // Cùng quy tắc với AccountService.validateLock (server vẫn kiểm tra lại)
    var rules = [
        { input: handover, errorId: "handover-error", check: function () {
            return handover.value ? "" : "Vui lòng chọn người nhận bàn giao.";
        } },
        { input: reason, errorId: "lock-reason-error", check: function () {
            var value = reason.value.trim();
            if (!value) {
                return "Vui lòng nhập lý do khóa.";
            }
            return value.length >= minLength ? "" : "Lý do khóa tối thiểu " + minLength + " ký tự.";
        } }
    ].filter(function (rule) {
        // Ô Bàn giao chỉ có khi người bị khóa đang phụ trách địa bàn/đại lý
        return Boolean(rule.input);
    });

    function validate(rule) {
        var message = rule.check();
        var error = document.getElementById(rule.errorId);
        error.textContent = message;
        error.hidden = !message;
        rule.input.closest(".form-group").classList.toggle("form-group--invalid", Boolean(message));
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

    rules.forEach(function (rule) {
        rule.input.addEventListener(rule.input.tagName === "SELECT" ? "change" : "input", function () {
            if (rule.input.getAttribute("aria-invalid") === "true") {
                validate(rule);
            }
        });
    });
})();
