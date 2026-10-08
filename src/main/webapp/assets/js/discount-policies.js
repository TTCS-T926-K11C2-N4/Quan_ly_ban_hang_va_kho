// Chính sách chiết khấu (S3-01): đổi ô theo "Áp cho" (SKU / nhóm hàng) và "Cách tính" (% / đồng trên đơn vị),
// thêm / xoá bậc chiết khấu, hỏi lại trước khi xoá hoặc ngừng chính sách
(function () {
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.getAttribute("data-confirm"))) {
                event.preventDefault();
            }
        });
    });

    var form = document.getElementById("discount-form");
    if (!form) {
        return;
    }

    function checked(name) {
        var input = form.querySelector("input[name='" + name + "']:checked");
        return input ? input.value : null;
    }

    function refreshScope() {
        var scope = checked("scopeType");
        form.querySelectorAll("[data-scope]").forEach(function (block) {
            block.hidden = block.getAttribute("data-scope") !== scope;
        });
    }

    function refreshType() {
        var percent = checked("discountType") !== "AMOUNT_PER_UNIT";
        form.querySelectorAll("[data-type-unit]").forEach(function (label) {
            label.textContent = percent ? "(%)" : "(đ / đơn vị)";
        });
        form.querySelectorAll("input[name='tierValue']").forEach(function (input) {
            input.placeholder = percent ? "Vd 2,5" : "Vd 500";
        });
    }

    form.querySelectorAll("[data-scope-choice]").forEach(function (input) {
        input.addEventListener("change", refreshScope);
    });
    form.querySelectorAll("[data-type-choice]").forEach(function (input) {
        input.addEventListener("change", refreshType);
    });

    var rows = document.getElementById("discount-tier-rows");
    var add = document.getElementById("discount-tier-add");
    var maxTiers = 10;

    function renumber() {
        var all = rows.querySelectorAll("[data-tier-row]");
        all.forEach(function (row, index) {
            var inputs = row.querySelectorAll("input");
            inputs[0].setAttribute("aria-label", "Số lượng tối thiểu bậc " + (index + 1));
            inputs[1].setAttribute("aria-label", "Mức chiết khấu bậc " + (index + 1));
            row.querySelector("[data-tier-remove]").setAttribute("aria-label", "Xoá bậc " + (index + 1));
        });
        add.disabled = all.length >= maxTiers;
    }

    rows.addEventListener("click", function (event) {
        var button = event.target.closest("[data-tier-remove]");
        if (!button) {
            return;
        }
        var all = rows.querySelectorAll("[data-tier-row]");
        var row = button.closest("[data-tier-row]");
        if (all.length === 1) {
            // Giữ lại một bậc để luôn có chỗ nhập: chỉ xoá chữ trong ô
            row.querySelectorAll("input").forEach(function (input) { input.value = ""; });
        } else {
            row.remove();
        }
        renumber();
    });

    add.addEventListener("click", function () {
        var template = rows.querySelector("[data-tier-row]");
        var row = template.cloneNode(true);
        row.querySelectorAll("input").forEach(function (input) { input.value = ""; });
        rows.appendChild(row);
        renumber();
        row.querySelector("input").focus();
    });

    refreshScope();
    refreshType();
    renumber();
    var invalid = form.querySelector(".form-group--invalid .form-group__control");
    (invalid || form.querySelector("#discount-name")).focus();
})();
