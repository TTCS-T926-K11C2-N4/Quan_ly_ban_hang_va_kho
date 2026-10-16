// Danh sách đơn hàng (S4-07): trên điện thoại các ô lọc thu gọn sau nút "Bộ lọc"; đổi cách sắp xếp hoặc số dòng
// mỗi trang thì tải lại ngay, không cần bấm Tìm kiếm (vẫn giữ nguyên các điều kiện lọc đang chọn trong form).
(function () {
    var form = document.getElementById("order-filter-form");
    if (!form) {
        return;
    }
    var toggle = document.getElementById("order-filter-toggle");
    if (toggle) {
        toggle.addEventListener("click", function () {
            var open = form.classList.toggle("customer-filters--open");
            toggle.setAttribute("aria-expanded", String(open));
        });
    }
    document.querySelectorAll("[data-autosubmit]").forEach(function (select) {
        select.addEventListener("change", function () {
            form.submit();
        });
    });
})();
