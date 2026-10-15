// Kiểm kê kho (S5-08): hiện nhóm hàng khi chọn phạm vi theo nhóm; tính chênh lệch ngay khi gõ số đếm;
// chốt phiếu bắt buộc lý do (server kiểm tra lại)
(function () {
    var scope = document.getElementById("stocktake-scope");
    var categoryField = document.getElementById("stocktake-category-field");
    if (scope && categoryField) {
        scope.addEventListener("change", function () {
            categoryField.hidden = scope.value !== "CATEGORY";
        });
    }

    function formatVariance(value) {
        var text = String(Math.round(value * 1000) / 1000).replace(".", ",");
        return value > 0 ? "+" + text : text;
    }

    document.querySelectorAll(".stocktake-items__count").forEach(function (input) {
        input.addEventListener("input", function () {
            var cell = input.closest("tr").querySelector("[data-variance]");
            var text = input.value.trim().replace(",", ".");
            if (text === "" || isNaN(Number(text))) {
                cell.textContent = "—";
                input.closest("tr").classList.remove("stocktake-items__row--variance");
                return;
            }
            var variance = Number(text) - Number(input.getAttribute("data-system"));
            cell.textContent = variance === 0 ? "0" : formatVariance(variance);
            input.closest("tr").classList.toggle("stocktake-items__row--variance", variance !== 0);
        });
    });

    var finalizeForm = document.getElementById("stocktake-finalize-form");
    if (finalizeForm) {
        finalizeForm.addEventListener("submit", function (event) {
            var reason = document.getElementById("stocktake-reason");
            var error = document.getElementById("stocktake-reason-error");
            if (reason.value.trim() === "") {
                event.preventDefault();
                error.hidden = false;
                reason.focus();
                return;
            }
            if (!window.confirm("Chốt kiểm kê và điều chỉnh tồn về số đếm? Thao tác này không hoàn tác được.")) {
                event.preventDefault();
            }
        });
    }
})();
