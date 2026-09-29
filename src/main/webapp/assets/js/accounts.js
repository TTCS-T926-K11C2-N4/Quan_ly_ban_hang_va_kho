(function () {
    var filterForm = document.getElementById("account-filter-form");

    // Figma không có nút Lọc: đổi vai trò/trạng thái là lọc ngay, ô tìm kiếm lọc khi nhấn Enter (submit mặc định)
    ["role-filter", "status-filter"].forEach(function (id) {
        document.getElementById(id).addEventListener("change", function () {
            filterForm.requestSubmit();
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
