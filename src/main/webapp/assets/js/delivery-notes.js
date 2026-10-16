// Xác nhận xuất kho & giao thiếu (S6-05): tính thiếu, tổng cần xuất / thực xuất / thiếu ngay khi gõ; dòng thiếu mới
// chọn được lý do; khoá nút xác nhận khi còn dòng thiếu chưa chọn lý do; hộp thoại kiểm tra lại trước khi gửi.
// Server vẫn kiểm tra lại toàn bộ khi xác nhận.
(function () {
    var form = document.getElementById("issue-form");
    if (!form) {
        return;
    }
    var rows = form.querySelectorAll("[data-issue-line]");
    var submit = document.getElementById("issue-submit");
    var shortPill = document.getElementById("issue-short-pill");
    var shortCard = document.getElementById("issue-shortage");
    var schedule = document.getElementById("issue-schedule");
    var warning = document.getElementById("issue-warning");
    var warningText = document.getElementById("issue-warning-text");
    var dialog = document.getElementById("issue-dialog");
    var numberFormat = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 3 });
    var confirmed = false;

    function parseQty(text) {
        var value = (text || "").trim().replace(",", ".");
        return /^\d{1,12}(\.\d{1,3})?$/.test(value) ? parseFloat(value) : null;
    }

    function round(value) {
        return Math.round(value * 1000) / 1000;
    }

    function selectedAction() {
        var checked = form.querySelector("[data-shortage-action]:checked");
        return checked ? checked.value : null;
    }

    function actionLabel() {
        var checked = form.querySelector("[data-shortage-action]:checked");
        return checked ? checked.parentNode.textContent.trim() : "Chưa chọn cách xử lý";
    }

    // Trạng thái hiện tại của form: tổng số, các dòng thiếu, lỗi chặn xác nhận
    function evaluate() {
        var state = { required: 0, issued: 0, shortLines: [], problem: null };
        Array.prototype.forEach.call(rows, function (row) {
            var required = parseFloat(row.getAttribute("data-required"));
            var picked = parseFloat(row.getAttribute("data-picked"));
            var input = row.querySelector("[data-issue-qty]");
            var reason = row.querySelector("[data-issue-reason]");
            var shortCell = row.querySelector("[data-issue-short]");
            var pill = row.querySelector("[data-issue-state]");
            var sku = row.getAttribute("data-sku");
            var qty = parseQty(input.value);
            state.required += required;
            var valid = qty !== null && qty <= picked;
            var shortQty = valid ? round(required - qty) : 0;
            if (valid) {
                state.issued += qty;
            } else if (!state.problem) {
                state.problem = qty === null ? "Dòng " + sku + ": nhập số thực xuất hợp lệ."
                    : "Dòng " + sku + ": thực xuất không vượt số đã soạn (" + numberFormat.format(picked) + ").";
            }
            input.classList.toggle("is-invalid", !valid);
            shortCell.textContent = numberFormat.format(shortQty);
            reason.disabled = !(shortQty > 0);
            reason.classList.toggle("is-missing", shortQty > 0 && !reason.value);
            pill.textContent = shortQty > 0 ? "Thiếu" : "Đủ";
            pill.className = "inv-pill issue-pill " + (shortQty > 0 ? "issue-pill--short" : "issue-pill--ok");
            if (shortQty > 0) {
                state.shortLines.push({ sku: sku, name: row.getAttribute("data-name"), qty: shortQty, reason: reason.value });
                if (!reason.value && !state.problem) {
                    state.problem = "Dòng " + sku + " thiếu " + numberFormat.format(shortQty)
                        + " sản phẩm, bắt buộc chọn lý do trước khi xác nhận.";
                }
            }
        });
        if (!state.problem && round(state.issued) === 0) {
            state.problem = "Phiếu xuất phải có ít nhất một sản phẩm.";
        }
        if (!state.problem && state.shortLines.length > 0) {
            var action = selectedAction();
            if (!action) {
                state.problem = "Chọn cách xử lý phần thiếu.";
            } else if (action === "BACKORDER" && !document.getElementById("backorder-date").value) {
                state.problem = "Chọn ngày giao bổ sung.";
            }
        }
        return state;
    }

    function refresh() {
        var state = evaluate();
        var shortTotal = round(state.required - state.issued);
        document.getElementById("total-issued").textContent = numberFormat.format(round(state.issued));
        document.getElementById("total-short").textContent = numberFormat.format(shortTotal > 0 ? shortTotal : 0);
        var hasShort = state.shortLines.length > 0;
        shortPill.hidden = !hasShort;
        shortPill.querySelector("[data-short-lines]").textContent = state.shortLines.length;
        shortCard.classList.toggle("is-inactive", !hasShort);
        schedule.classList.toggle("is-hidden", selectedAction() !== "BACKORDER");
        warning.hidden = !state.problem;
        warningText.textContent = state.problem || "";
        submit.disabled = !!state.problem;
        return state;
    }

    form.addEventListener("input", refresh);
    form.addEventListener("change", refresh);

    function fillDialog(state) {
        dialog.querySelector("[data-dialog-required]").textContent = numberFormat.format(round(state.required));
        dialog.querySelector("[data-dialog-issued]").textContent = numberFormat.format(round(state.issued));
        dialog.querySelector("[data-dialog-short]").textContent = numberFormat.format(round(state.required - state.issued));
        var shortage = dialog.querySelector("[data-dialog-shortage]");
        var list = dialog.querySelector("[data-dialog-list]");
        shortage.hidden = state.shortLines.length === 0;
        list.textContent = "";
        state.shortLines.forEach(function (line) {
            var item = document.createElement("li");
            var name = document.createElement("span");
            name.textContent = line.sku + " · " + line.name + " — " + line.reason;
            var qty = document.createElement("strong");
            qty.textContent = "Thiếu " + numberFormat.format(line.qty);
            item.appendChild(name);
            item.appendChild(qty);
            list.appendChild(item);
        });
        var handling = actionLabel();
        if (selectedAction() === "BACKORDER") {
            var date = document.getElementById("backorder-date").value;
            var time = document.getElementById("backorder-time").value || "08:00";
            if (date) {
                var parts = date.split("-");
                handling += " lúc " + time + " ngày " + parts[2] + "/" + parts[1] + "/" + parts[0];
            }
        }
        var note = document.getElementById("shortage-note").value.trim();
        dialog.querySelector("[data-dialog-handling]").textContent = handling + (note ? ". " + note : ".");
    }

    form.addEventListener("submit", function (event) {
        if (confirmed || !dialog || typeof dialog.showModal !== "function") {
            return;
        }
        var state = refresh();
        event.preventDefault();
        if (state.problem) {
            return;
        }
        fillDialog(state);
        dialog.showModal();
    });

    if (dialog) {
        dialog.querySelector("[data-dialog-close]").addEventListener("click", function () {
            dialog.close();
        });
        dialog.querySelector("[data-dialog-confirm]").addEventListener("click", function () {
            confirmed = true;
            dialog.querySelector("[data-dialog-confirm]").disabled = true;
            dialog.close();
            if (typeof form.requestSubmit === "function") {
                form.requestSubmit();
            } else {
                form.submit();
            }
        });
    }

    refresh();
})();
