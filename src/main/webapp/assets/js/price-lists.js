// Form bảng giá (S2-10): thêm/xoá dòng giá, hiện đơn vị và giá vốn của sản phẩm đã chọn, báo ngay khi giá sàn
// cao hơn giá bán hoặc chọn trùng sản phẩm. Server vẫn kiểm tra lại toàn bộ (PriceListService.validate).
(function () {
    var table = document.getElementById("price-lines");
    var template = document.getElementById("price-line-template");
    if (!table || !template) {
        return;
    }
    var body = table.tBodies[0];
    var empty = document.getElementById("price-lines-empty");

    function toNumber(text) {
        var digits = (text || "").replace(/[.,\s]/g, "");
        return /^\d+$/.test(digits) ? Number(digits) : null;
    }

    function refreshRow(row) {
        var select = row.querySelector(".price-line__product");
        var option = select.options[select.selectedIndex];
        row.querySelector(".price-line__unit").textContent = option && option.value ? option.getAttribute("data-unit") : "—";
        var cost = row.querySelector(".price-line__cost");
        if (cost) {
            cost.textContent = option && option.value ? (option.getAttribute("data-cost") || "0") : "—";
        }
    }

    function validateRow(row, chosen) {
        var select = row.querySelector(".price-line__product");
        var inputs = row.querySelectorAll(".price-line__money");
        var price = toNumber(inputs[0].value);
        var floor = toNumber(inputs[1].value);
        var message = "";
        if (select.value && chosen[select.value] > 1) {
            message = "Sản phẩm này đã có ở dòng khác.";
        } else if (price !== null && floor !== null && floor > price) {
            message = "Giá sàn không được cao hơn giá bán.";
        }
        var error = row.querySelector(".price-line__client-error");
        if (!error) {
            error = document.createElement("p");
            error.className = "form-group__error price-line__client-error";
            select.closest("td").appendChild(error);
        }
        error.textContent = message;
        error.hidden = !message;
        row.classList.toggle("price-line--invalid", Boolean(message));
    }

    function refreshAll() {
        var rows = Array.prototype.slice.call(body.rows);
        var chosen = {};
        rows.forEach(function (row, index) {
            row.querySelector(".price-line__index").textContent = index + 1;
            var value = row.querySelector(".price-line__product").value;
            if (value) {
                chosen[value] = (chosen[value] || 0) + 1;
            }
        });
        rows.forEach(function (row) {
            validateRow(row, chosen);
        });
        empty.hidden = rows.length > 0;
    }

    Array.prototype.forEach.call(body.rows, refreshRow);
    refreshAll();

    document.getElementById("add-price-line").addEventListener("click", function () {
        var row = template.content.firstElementChild.cloneNode(true);
        body.appendChild(row);
        refreshAll();
        row.querySelector(".price-line__product").focus();
    });

    body.addEventListener("change", function (event) {
        var row = event.target.closest(".price-line");
        if (row && event.target.classList.contains("price-line__product")) {
            refreshRow(row);
        }
        refreshAll();
    });
    body.addEventListener("input", function (event) {
        if (event.target.classList.contains("price-line__money")) {
            refreshAll();
        }
    });
    body.addEventListener("click", function (event) {
        var button = event.target.closest(".price-line__remove");
        if (button) {
            button.closest(".price-line").remove();
            refreshAll();
        }
    });
})();
