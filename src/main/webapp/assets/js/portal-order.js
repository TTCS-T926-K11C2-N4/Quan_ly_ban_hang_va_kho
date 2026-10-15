// Giỏ hàng của đại lý (S4-10): thêm / xoá dòng, nạp đơn vị tính theo sản phẩm đã chọn. Tổng tiền do server tính
// khi bấm "Xem tổng tiền" hoặc "Gửi đơn hàng", nên số hiển thị luôn khớp số lưu.
(function () {
    var lines = document.getElementById("portal-cart-lines");
    var template = document.getElementById("portal-line-template");
    var catalog = document.getElementById("portal-unit-catalog");
    if (!lines || !template || !catalog) {
        return;
    }

    function fillUnits(row, selectedUnit) {
        var productId = row.querySelector(".portal-cart__product").value;
        var unitSelect = row.querySelector(".portal-cart__unit");
        unitSelect.innerHTML = "";
        var source = productId ? catalog.querySelector('select[data-product="' + productId + '"]') : null;
        if (!source) {
            return;
        }
        Array.prototype.forEach.call(source.options, function (option) {
            unitSelect.appendChild(option.cloneNode(true));
        });
        if (selectedUnit) {
            unitSelect.value = selectedUnit;
        }
    }

    function addLine() {
        lines.appendChild(template.content.cloneNode(true));
        var row = lines.lastElementChild;
        row.querySelector(".portal-cart__product").focus();
    }

    lines.addEventListener("change", function (event) {
        if (event.target.classList.contains("portal-cart__product")) {
            fillUnits(event.target.closest(".portal-cart__line"), "");
        }
    });

    lines.addEventListener("click", function (event) {
        var remove = event.target.closest(".portal-cart__remove");
        if (remove) {
            remove.closest(".portal-cart__line").remove();
        }
    });

    document.getElementById("portal-add-line").addEventListener("click", addLine);

    // Dòng dựng sẵn từ server: nạp đơn vị và chọn lại đơn vị đã chọn
    lines.querySelectorAll(".portal-cart__line").forEach(function (row) {
        fillUnits(row, row.querySelector(".portal-cart__unit").getAttribute("data-selected"));
    });
    if (lines.children.length === 0) {
        addLine();
    }
})();
