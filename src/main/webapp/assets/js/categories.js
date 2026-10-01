// Quản lý nhóm hàng (S2-06): thu gọn/mở nhánh, lọc trạng thái, menu ⋮, xác nhận xoá/ngừng, chọn sản phẩm để chuyển nhóm
(function () {
    var filterForm = document.getElementById("category-filter-form");
    if (filterForm) {
        // Giống trang tài khoản: đổi trạng thái là lọc ngay, ô tìm kiếm lọc khi nhấn Enter
        document.getElementById("category-status-filter").addEventListener("change", function () {
            filterForm.requestSubmit();
        });
    }

    initTreeToggles();
    initRowMenus();
    initConfirmForms();
    initProductSelection();

    // Mỗi dòng có data-path (vd /1/6/11/). Dòng bị ẩn khi có nhóm tổ tiên đang thu gọn.
    function initTreeToggles() {
        var table = document.getElementById("category-table");
        if (!table) {
            return;
        }
        var rows = Array.prototype.slice.call(table.querySelectorAll("tbody tr[data-path]"));
        var collapsed = {};

        table.addEventListener("click", function (event) {
            var button = event.target.closest(".category-toggle");
            if (!button) {
                return;
            }
            var path = button.closest("tr").getAttribute("data-path");
            var expanded = button.getAttribute("aria-expanded") === "true";
            button.setAttribute("aria-expanded", String(!expanded));
            button.setAttribute("aria-label", button.getAttribute("aria-label")
                .replace(expanded ? "Thu gọn" : "Mở rộng", expanded ? "Mở rộng" : "Thu gọn"));
            collapsed[path] = expanded;
            refresh();
        });

        function refresh() {
            rows.forEach(function (row) {
                var path = row.getAttribute("data-path");
                row.hidden = Object.keys(collapsed).some(function (ancestor) {
                    return collapsed[ancestor] && path !== ancestor && path.indexOf(ancestor) === 0;
                });
            });
        }
    }

    function initRowMenus() {
        var rowMenus = document.querySelectorAll(".row-menu");
        if (rowMenus.length === 0) {
            return;
        }

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
    }

    // Form có data-confirm: hỏi lại trước khi gửi (xoá, ngừng hoạt động)
    function initConfirmForms() {
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.getAttribute("data-confirm"))) {
                    event.preventDefault();
                }
            });
        });
    }

    function initProductSelection() {
        var form = document.getElementById("move-products-form");
        var submit = document.getElementById("move-products-submit");
        if (!form || !submit) {
            return;
        }
        var selectAll = document.getElementById("select-all-products");
        var counter = document.getElementById("move-selected-count");
        var boxes = Array.prototype.slice.call(form.querySelectorAll("input[name='productIds']"));

        function update() {
            var checked = boxes.filter(function (box) { return box.checked; }).length;
            submit.disabled = checked === 0;
            counter.textContent = checked === 0 ? "Chưa chọn sản phẩm nào." : "Đã chọn " + checked + " sản phẩm.";
            selectAll.checked = checked === boxes.length;
            selectAll.indeterminate = checked > 0 && checked < boxes.length;
        }

        selectAll.addEventListener("change", function () {
            boxes.forEach(function (box) {
                box.checked = selectAll.checked;
            });
            update();
        });
        boxes.forEach(function (box) {
            box.addEventListener("change", update);
        });
    }
})();
