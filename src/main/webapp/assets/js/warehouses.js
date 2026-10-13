// Danh sách kho và vị trí trong kho (S5-03)
(function () {
    // Đổi loại kho / trạng thái thì lọc ngay, không cần bấm nút
    var filterForm = document.getElementById("warehouse-filter-form");
    if (filterForm) {
        Array.prototype.forEach.call(filterForm.querySelectorAll("[data-auto-submit]"), function (select) {
            select.addEventListener("change", function () {
                filterForm.submit();
            });
        });
    }

    // Nút "Bộ lọc" ở đầu bảng ẩn/hiện thanh lọc
    var filterToggle = document.getElementById("warehouse-filter-toggle");
    if (filterToggle && filterForm) {
        filterToggle.addEventListener("click", function () {
            var open = filterForm.hidden;
            filterForm.hidden = !open;
            filterToggle.setAttribute("aria-expanded", String(open));
            if (open) {
                document.getElementById("warehouse-keyword").focus();
            }
        });
    }

    // Chọn tất cả; "Xuất danh sách" chỉ xuất các kho đang tích (không tích kho nào thì xuất theo bộ lọc)
    var checkAll = document.getElementById("warehouse-check-all");
    var rowChecks = document.querySelectorAll(".warehouse-table__select");
    if (checkAll) {
        checkAll.addEventListener("change", function () {
            Array.prototype.forEach.call(rowChecks, function (box) {
                box.checked = checkAll.checked;
            });
        });
        Array.prototype.forEach.call(rowChecks, function (box) {
            box.addEventListener("change", function () {
                var checked = Array.prototype.filter.call(rowChecks, function (item) { return item.checked; }).length;
                checkAll.checked = checked === rowChecks.length;
                checkAll.indeterminate = checked > 0 && checked < rowChecks.length;
            });
        });
    }
    var exportLink = document.getElementById("warehouse-export");
    if (exportLink) {
        exportLink.addEventListener("click", function (event) {
            var ids = Array.prototype.filter.call(rowChecks, function (box) { return box.checked; })
                .map(function (box) { return "id=" + encodeURIComponent(box.value); });
            if (ids.length) {
                event.preventDefault();
                window.location.href = exportLink.href + (exportLink.href.indexOf("?") < 0 ? "?" : "&") + ids.join("&");
            }
        });
    }

    // Form vị trí: chỉ kệ mới chọn khu chứa
    var parentField = document.getElementById("location-parent-field");
    var typeInputs = document.querySelectorAll(".location-form__type");
    function syncParent() {
        var checked = document.querySelector(".location-form__type:checked");
        var isRack = checked && checked.value === "RACK";
        parentField.hidden = !isRack;
        document.getElementById("location-parent").disabled = !isRack;
    }
    if (parentField && typeInputs.length) {
        Array.prototype.forEach.call(typeInputs, function (input) {
            input.addEventListener("change", syncParent);
        });
        syncParent();
    }
})();
