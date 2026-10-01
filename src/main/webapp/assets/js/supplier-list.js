(function () {
    var filterForm = document.getElementById("supplier-filter-form");

    // Giống trang tài khoản: đổi trạng thái là lọc ngay, ô tìm kiếm lọc khi nhấn Enter
    document.getElementById("status-filter").addEventListener("change", function () {
        filterForm.requestSubmit();
    });

    // Ngừng giao dịch / xoá: hỏi lại trước khi gửi
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.getAttribute("data-confirm"))) {
                event.preventDefault();
            }
        });
    });

    var rowMenus = document.querySelectorAll(".row-menu");

    function closeRowMenus(except) {
        rowMenus.forEach(function (menu) {
            if (menu !== except) {
                menu.open = false;
            }
        });
    }

    rowMenus.forEach(function (menu) {
        menu.addEventListener("toggle", function () {
            if (menu.open) {
                closeRowMenus(menu);
            }
        });
    });

    document.addEventListener("click", function (event) {
        if (!event.target.closest(".row-menu")) {
            closeRowMenus(null);
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeRowMenus(null);
        }
    });
})();
