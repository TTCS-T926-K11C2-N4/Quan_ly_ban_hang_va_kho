// Phiếu nhập kho (S5-04): lọc danh sách, và trên form phiếu: chọn sản phẩm theo gợi ý, đổi đơn vị / vị trí,
// tính số lượng cơ sở, thành tiền, tổng phiếu ngay khi gõ. Server vẫn kiểm tra và tính lại khi lưu.
(function () {
    var filterForm = document.getElementById("receipt-filter-form");
    if (filterForm) {
        Array.prototype.forEach.call(filterForm.querySelectorAll("[data-auto-submit]"), function (control) {
            control.addEventListener("change", function () {
                filterForm.submit();
            });
        });
    }

    var form = document.getElementById("receipt-form");
    if (!form) {
        return;
    }
    var table = document.getElementById("receipt-lines");
    var template = document.getElementById("receipt-line-template");
    var data = document.getElementById("receipt-data");
    var datalist = document.getElementById("receipt-products");
    var warehouseInput = document.getElementById("receipt-warehouse");
    var adjustment = form.getAttribute("data-adjustment") === "true";
    var numberFormat = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 3 });
    var moneyFormat = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });

    function productIdOf(text) {
        var options = datalist.options;
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === text) {
                return options[i].getAttribute("data-id");
            }
        }
        return "";
    }

    function productData(productId) {
        return productId ? data.querySelector('select[data-product="' + productId + '"]') : null;
    }

    function parseQty(value) {
        var text = value.trim().replace(",", ".");
        var pattern = adjustment ? /^-?\d+(\.\d{1,3})?$/ : /^\d+(\.\d{1,3})?$/;
        return pattern.test(text) ? parseFloat(text) : NaN;
    }

    function parseMoney(value) {
        var text = value.trim();
        return /^(\d{1,3}(\.\d{3})+|\d+)$/.test(text) ? parseInt(text.replace(/\./g, ""), 10) : NaN;
    }

    function selectedFactor(line) {
        var unit = line.querySelector(".receipt-line__unit");
        var option = unit.options[unit.selectedIndex];
        return option ? parseFloat(option.getAttribute("data-factor")) : NaN;
    }

    // Đổ lại ô đơn vị theo sản phẩm; giữ đơn vị đang chọn nếu sản phẩm có đơn vị đó
    function fillUnits(line, productId) {
        var unit = line.querySelector(".receipt-line__unit");
        var source = productData(productId);
        var current = unit.value;
        unit.innerHTML = source ? source.innerHTML : '<option value="">—</option>';
        if (source && current && unit.querySelector('option[value="' + current + '"]')) {
            unit.value = current;
        }
    }

    function fillLocations(line) {
        var location = line.querySelector(".receipt-line__location");
        var source = warehouseInput && warehouseInput.value
            ? data.querySelector('select[data-warehouse="' + warehouseInput.value + '"]') : null;
        var current = location.value;
        location.innerHTML = '<option value="">Chưa xếp</option>' + (source ? source.innerHTML : "");
        if (current && location.querySelector('option[value="' + current + '"]')) {
            location.value = current;
        }
    }

    function updateProduct(line) {
        var productId = productIdOf(line.querySelector(".receipt-line__product").value);
        var source = productData(productId);
        line.querySelector(".receipt-line__product-id").value = productId;
        line.querySelector(".receipt-line__sku").textContent = source
            ? "SKU: " + source.getAttribute("data-sku") : "Chưa chọn sản phẩm";
        line.querySelector(".receipt-line__lot").hidden = !(source && source.getAttribute("data-lot") === "true");
        fillUnits(line, productId);
        recalc(line);
    }

    function recalc(line) {
        var factor = selectedFactor(line);
        var qty = parseQty(line.querySelector(".receipt-line__qty").value);
        var price = parseMoney(line.querySelector(".receipt-line__price").value);
        line.querySelector(".receipt-line__factor").value = isNaN(factor) ? "" : numberFormat.format(factor);
        var base = qty * factor;
        line.querySelector(".receipt-line__base").textContent = isNaN(base) ? "—" : numberFormat.format(base);
        var amount = qty * price;
        line.querySelector(".receipt-line__amount").textContent = isNaN(amount) ? "—" : moneyFormat.format(amount) + " đ";
        summarize();
    }

    function summarize() {
        var lines = 0;
        var base = 0;
        var total = 0;
        Array.prototype.forEach.call(table.querySelectorAll(".receipt-line"), function (line) {
            if (!line.querySelector(".receipt-line__product-id").value) {
                return;
            }
            lines++;
            var qty = parseQty(line.querySelector(".receipt-line__qty").value);
            var factor = selectedFactor(line);
            var price = parseMoney(line.querySelector(".receipt-line__price").value);
            if (!isNaN(qty * factor)) {
                base += qty * factor;
            }
            if (!isNaN(qty * price)) {
                total += qty * price;
            }
        });
        document.getElementById("summary-lines").textContent = lines;
        document.getElementById("summary-base").textContent = numberFormat.format(base);
        document.getElementById("summary-total").textContent = moneyFormat.format(total) + " đ";
    }

    function bind(line) {
        var product = line.querySelector(".receipt-line__product");
        product.addEventListener("change", function () { updateProduct(line); });
        product.addEventListener("input", function () {
            if (productIdOf(product.value)) {
                updateProduct(line);
            }
        });
        line.querySelector(".receipt-line__unit").addEventListener("change", function () { recalc(line); });
        line.querySelector(".receipt-line__qty").addEventListener("input", function () { recalc(line); });
        var price = line.querySelector(".receipt-line__price");
        price.addEventListener("input", function () { recalc(line); });
        // Gõ 120000 thì hiện lại 120.000 khi rời ô
        price.addEventListener("blur", function () {
            var value = parseMoney(price.value);
            if (!isNaN(value)) {
                price.value = moneyFormat.format(value);
                recalc(line);
            }
        });
        line.querySelector(".receipt-line__remove").addEventListener("click", function () {
            if (table.querySelectorAll(".receipt-line").length > 1) {
                line.remove();
                summarize();
            } else {
                Array.prototype.forEach.call(line.querySelectorAll("input:not([readonly])"), function (input) {
                    input.value = "";
                });
                updateProduct(line);
            }
        });
    }

    Array.prototype.forEach.call(table.querySelectorAll(".receipt-line"), function (line) {
        bind(line);
        recalc(line);
    });

    document.getElementById("receipt-add-line").addEventListener("click", function () {
        var line = template.content.firstElementChild.cloneNode(true);
        table.appendChild(line);
        fillLocations(line);
        bind(line);
        line.querySelector(".receipt-line__product").focus();
    });

    if (warehouseInput && warehouseInput.tagName === "SELECT") {
        warehouseInput.addEventListener("change", function () {
            Array.prototype.forEach.call(table.querySelectorAll(".receipt-line"), fillLocations);
        });
    }

    // Ghi sổ không hoàn tác được: hỏi lại trước khi gửi
    var postButton = document.getElementById("receipt-post");
    form.addEventListener("submit", function (event) {
        if (event.submitter === postButton && !window.confirm(postButton.getAttribute("data-confirm"))) {
            event.preventDefault();
        }
    });
})();
