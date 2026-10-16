// Sổ tồn kho (S5-09): mở hộp đặt tồn tối thiểu cho một SKU tại một kho; số hợp lệ được server kiểm tra lại
(function () {
    var dialog = document.getElementById("min-stock-dialog");
    if (!dialog || typeof dialog.showModal !== "function") {
        return;
    }
    var qty = document.getElementById("min-stock-qty");
    var error = document.getElementById("min-stock-error");

    document.querySelectorAll(".stock-table__min-edit").forEach(function (button) {
        button.addEventListener("click", function () {
            document.getElementById("min-stock-product").value = button.getAttribute("data-product");
            document.getElementById("min-stock-warehouse").value = button.getAttribute("data-warehouse");
            document.getElementById("min-stock-target").textContent = button.getAttribute("data-sku") + " - "
                + button.getAttribute("data-name") + " · Kho " + button.getAttribute("data-warehouse-name");
            document.getElementById("min-stock-unit").textContent = button.getAttribute("data-unit");
            qty.value = button.getAttribute("data-min") === "0" ? "" : button.getAttribute("data-min");
            error.hidden = true;
            dialog.showModal();
            qty.focus();
            qty.select();
        });
    });

    document.getElementById("min-stock-cancel").addEventListener("click", function () {
        dialog.close();
    });
    dialog.addEventListener("click", function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });
    dialog.querySelector("form").addEventListener("submit", function (event) {
        if (qty.value.trim() === "") {
            qty.value = "0";
        }
        if (!/^\d{1,12}([.,]\d{1,3})?$/.test(qty.value.trim())) {
            event.preventDefault();
            error.textContent = "Nhập số không âm, tối đa 3 chữ số thập phân (vd 120 hoặc 12,5).";
            error.hidden = false;
            qty.focus();
        }
    });
})();
