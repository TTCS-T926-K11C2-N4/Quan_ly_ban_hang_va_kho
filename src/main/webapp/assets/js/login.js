(function () {
    var form = document.getElementById("login-form");
    var passwordInput = document.getElementById("password");
    var toggleButton = document.getElementById("toggle-password");
    var toggleText = document.getElementById("toggle-password-text");

    toggleButton.addEventListener("click", function () {
        var isHidden = passwordInput.type === "password";
        passwordInput.type = isHidden ? "text" : "password";
        toggleText.textContent = isHidden ? "Ẩn" : "Hiện";
        toggleButton.setAttribute("aria-pressed", String(isHidden));
    });

    // Server chỉ gửi số giây còn lại (không gửi mốc giờ) để không phụ thuộc đồng hồ máy người dùng
    var lockSeconds = parseInt(form.getAttribute("data-lock-seconds"), 10);
    if (lockSeconds > 0) {
        setTimeout(unlockForm, lockSeconds * 1000);
    }

    function unlockForm() {
        document.getElementById("login-card").classList.remove("auth-card--locked");

        var error = document.getElementById("login-error");
        if (error) {
            error.remove();
        }

        form.querySelectorAll(".form-field__control").forEach(function (control) {
            control.classList.remove("form-field__control--invalid", "form-field__control--disabled");
        });

        form.querySelectorAll(":disabled").forEach(function (element) {
            element.disabled = false;
        });

        form.querySelectorAll(".form-field__input").forEach(function (input) {
            input.setAttribute("aria-invalid", "false");
            input.removeAttribute("aria-describedby");
        });

        passwordInput.focus();
    }
})();
