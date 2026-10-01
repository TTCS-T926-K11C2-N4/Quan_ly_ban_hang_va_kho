(function () {
    var filterForm = document.getElementById("audit-filter-form");

    // Giống trang tài khoản: đổi một ô lọc là lọc ngay, không cần nút Lọc
    ["actor-filter", "entity-type-filter", "from-date", "to-date"].forEach(function (id) {
        document.getElementById(id).addEventListener("change", function () {
            filterForm.requestSubmit();
        });
    });
})();
