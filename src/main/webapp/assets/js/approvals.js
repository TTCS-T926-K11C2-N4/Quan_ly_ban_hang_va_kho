// Màn duyệt đơn (S4-05): mở hộp Duyệt / Từ chối / Trả lại sửa, đếm ký tự ý kiến, bắt buộc ý kiến khi từ chối
// hoặc trả lại (server kiểm tra lại), khoá nút khi đang gửi để không bấm hai lần. Lỗi từ server thì hộp tự mở lại.
(function () {
    var dialogs = document.querySelectorAll(".approval-dialog");
    if (!dialogs.length) {
        return;
    }

    function open(dialog) {
        if (dialog && typeof dialog.showModal === "function") {
            dialog.showModal();
            var field = dialog.querySelector("textarea");
            if (field) {
                field.focus();
            }
        }
    }

    document.querySelectorAll("[data-open-dialog]").forEach(function (button) {
        button.addEventListener("click", function () {
            open(document.getElementById(button.getAttribute("data-open-dialog")));
        });
    });

    dialogs.forEach(function (dialog) {
        dialog.addEventListener("click", function (event) {
            if (event.target === dialog || event.target.closest("[data-close-dialog]")) {
                dialog.close();
            }
        });

        var comment = dialog.querySelector("textarea");
        var counter = comment && comment.getAttribute("data-counter")
            ? document.getElementById(comment.getAttribute("data-counter")) : null;
        function count() {
            if (counter) {
                counter.textContent = comment.value.length + "/" + comment.getAttribute("maxlength");
            }
        }
        if (comment) {
            comment.addEventListener("input", count);
            count();
        }

        var form = dialog.querySelector("form");
        form.addEventListener("submit", function (event) {
            if (comment && comment.required && !comment.value.trim()) {
                event.preventDefault();
                var field = comment.closest(".sales-field");
                var error = field.querySelector(".sales-field__error");
                if (!error) {
                    error = document.createElement("p");
                    error.className = "sales-field__error";
                    field.appendChild(error);
                }
                error.textContent = "Vui lòng nhập ý kiến trước khi xác nhận.";
                field.classList.add("sales-field--invalid");
                comment.focus();
                return;
            }
            var submit = form.querySelector("button[type=submit]");
            submit.disabled = true;
            submit.textContent = "Đang xử lý…";
        });

        if (dialog.getAttribute("data-open") === "1") {
            open(dialog);
        }
    });
})();
