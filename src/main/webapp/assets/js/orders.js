// Màn Tạo đơn hàng (S3-09): thêm/xoá dòng hàng, chọn hàng theo gợi ý mã/tên, đổi đơn vị tính,
// đổi đại lý thì nạp lại điểm giao; mỗi lần sửa thì hỏi server tính lại tiền (/orders/quote).
// S4-01: ô Đơn giá để trống là giá bảng giá (hiện mờ), gõ giá khác là sửa tay; giá thực bán dưới giá sàn thì báo cần duyệt.
// S4-02: khối công nợ dưới ô Đại lý tính lại theo tổng đơn; vượt hạn mức thì cần duyệt, nợ quá hạn thì khoá nút lưu.
// S4-03: dưới ô Số lượng hiện tồn khả dụng ở kho phục vụ đại lý; vượt tồn thì báo đỏ, có nút điền nhanh số tối đa.
// S4-04: hộp lịch sử mua hàng 3 tháng của đại lý, thêm nhanh từng mặt hàng hoặc cả lần mua trước vào đơn.
(function () {
    var form = document.getElementById("order-form");
    if (!form) {
        return;
    }
    var tbody = document.getElementById("order-lines");
    var template = document.getElementById("order-line-template");
    var emptyNote = document.getElementById("order-lines-empty");
    var customerSelect = document.getElementById("order-customer");
    var addressSelect = document.getElementById("order-address");
    var unitCatalog = document.getElementById("order-unit-catalog");
    var blockedWarning = document.getElementById("blocked-warning");
    var blockedWarningText = document.getElementById("blocked-warning-text");
    var overdueWarning = document.getElementById("overdue-warning");
    var overdueWarningText = document.getElementById("overdue-warning-text");
    var creditBox = document.getElementById("order-credit");
    var saveButtons = [document.getElementById("save-draft"), document.getElementById("submit-order")];
    var alertsBox = document.getElementById("order-alerts");
    var alertsList = document.getElementById("order-alerts-list");
    var alertsNote = document.getElementById("order-alerts-note");
    var draftCustomer = form.getAttribute("data-draft-customer");

    // Chữ gợi ý "SP001 - Tên hàng" -> id sản phẩm
    var productIds = {};
    document.querySelectorAll("#order-products option").forEach(function (option) {
        productIds[option.value] = option.getAttribute("data-id");
    });

    function rows() {
        return tbody.querySelectorAll(".order-line");
    }

    function renumber() {
        rows().forEach(function (row, index) {
            var number = String(index + 1).padStart(2, "0");
            row.querySelector(".order-line__index").textContent = number;
            row.querySelector(".order-line__product").setAttribute("aria-label", "Mã hàng hoặc tên hàng dòng " + (index + 1));
            row.querySelector(".order-line__unit").setAttribute("aria-label", "Đơn vị tính dòng " + (index + 1));
            row.querySelector(".order-line__qty").setAttribute("aria-label", "Số lượng dòng " + (index + 1));
            row.querySelector(".order-line__price").setAttribute("aria-label", "Đơn giá dòng " + (index + 1));
            row.querySelector(".order-line__reset").setAttribute("aria-label", "Về giá bảng giá dòng " + (index + 1));
            row.querySelector(".order-line__remove").setAttribute("aria-label", "Xoá dòng " + (index + 1));
        });
        emptyNote.hidden = rows().length > 0;
    }

    function fillUnits(row, productId) {
        var unitSelect = row.querySelector(".order-line__unit");
        unitSelect.innerHTML = "";
        var source = productId ? unitCatalog.querySelector('select[data-product="' + productId + '"]') : null;
        if (source) {
            Array.prototype.forEach.call(source.options, function (option) {
                unitSelect.appendChild(option.cloneNode(true));
            });
        }
    }

    function setLineError(row, message) {
        var error = row.querySelector(".order-line__error");
        error.textContent = message || "";
        error.hidden = !message;
        row.classList.toggle("order-line--invalid", Boolean(message));
    }

    function onProductChange(row) {
        var input = row.querySelector(".order-line__product");
        var hidden = row.querySelector(".order-line__product-id");
        var id = productIds[input.value.trim()] || "";
        if (id !== hidden.value) {
            hidden.value = id;
            fillUnits(row, id);
            clearPrice(row);
        }
        scheduleQuote();
    }

    // Giá sửa tay tính theo đơn vị đang chọn nên đổi hàng / đổi đơn vị thì quay về giá bảng giá
    function clearPrice(row) {
        row.querySelector(".order-line__price").value = "";
        row.querySelector(".order-line__reset").hidden = true;
    }

    function showPrice(row, line) {
        var priced = line && !line.error;
        row.querySelector(".order-line__price").placeholder = priced ? line.listPrice : "";
        row.querySelector(".order-line__reset").hidden = !row.querySelector(".order-line__price").value.trim();
        row.querySelector(".order-line__tag--manual").hidden = !(priced && line.manual && !line.belowFloor);
        row.querySelector(".order-line__tag--below").hidden = !(priced && line.belowFloor);
        row.classList.toggle("order-line--below-floor", Boolean(priced && line.belowFloor));
    }

    function alertItem(number, line) {
        var item = document.createElement("li");
        item.className = "order-alerts__item order-alerts__item--" + (line.belowFloor ? "warning" : "info");
        var label = document.createElement("strong");
        label.textContent = "Dòng " + number + " · " + line.sku + ":";
        item.appendChild(label);
        item.appendChild(document.createTextNode(line.belowFloor
            ? " giá thực bán sau chiết khấu " + line.netPrice + " thấp hơn giá sàn " + line.floorPrice + "."
            : " đã sửa giá tay " + line.unitPrice + " (bảng giá " + line.listPrice + " ₫)."));
        return item;
    }

    // Dòng lỗi giá/số lượng (line.stock = null) thì ẩn; kho không còn hàng (kể cả chưa từng nhập) thì báo hết hàng
    function showStock(row, line) {
        var box = row.querySelector(".order-line__stock");
        var text = box.querySelector(".order-line__stock-text");
        var max = box.querySelector(".order-line__stock-max");
        var stock = line && !line.error ? line.stock : null;
        box.hidden = !stock;
        max.hidden = true;
        row.classList.toggle("order-line--short", Boolean(stock && stock.shortage));
        if (!stock) {
            return;
        }
        box.classList.toggle("order-line__stock--short", stock.shortage);
        if (stock.soldOut) {
            text.textContent = "Hết hàng ở kho này";
        } else if (!stock.shortage) {
            text.textContent = "Khả dụng: " + stock.available;
        } else {
            text.textContent = "Vượt tồn · khả dụng " + stock.available;
            if (stock.maxQty !== "0") {
                max.hidden = false;
                max.textContent = "Đặt tối đa " + stock.max;
                max.setAttribute("data-qty", stock.maxQty);
            }
        }
    }

    function creditItem(credit) {
        var item = document.createElement("li");
        item.className = "order-alerts__item order-alerts__item--warning";
        var label = document.createElement("strong");
        label.textContent = "Hạn mức công nợ:";
        item.appendChild(label);
        item.appendChild(document.createTextNode(" tổng đơn cộng công nợ hiện tại vượt hạn mức "
            + credit.exceededBy + "."));
        return item;
    }

    function showCredit(credit) {
        creditBox.hidden = !credit;
        if (!credit) {
            return;
        }
        creditBox.classList.toggle("order-credit--over", credit.overLimit);
        setText("credit-terms", credit.maxDebtDays + " ngày");
        setText("credit-limit", credit.granted ? credit.limit : "Chưa cấp hạn mức");
        setText("credit-ledger", credit.ledger);
        setText("credit-open", credit.openOrders);
        setText("credit-available", credit.available);
        document.getElementById("credit-available").classList.toggle("order-credit__value--negative", credit.short);
        var bar = document.getElementById("credit-bar");
        bar.value = credit.usedPercent;
        bar.textContent = credit.usedPercent + "%";
        var after = document.getElementById("credit-after");
        var amount = document.createElement("strong");
        amount.textContent = credit.overLimit ? credit.exceededBy : credit.after;
        after.innerHTML = "";
        after.appendChild(document.createTextNode(credit.overLimit ? "Vượt hạn mức " : "Sau đơn này còn "));
        after.appendChild(amount);
        after.appendChild(document.createTextNode(credit.overLimit
            ? " nếu gửi đơn này: đơn sẽ được đánh dấu cần duyệt." : "."));
    }

    function addLine() {
        tbody.appendChild(template.content.cloneNode(true));
        renumber();
        var row = rows()[rows().length - 1];
        row.querySelector(".order-line__product").focus();
    }

    // S4-04: thêm một dòng hàng đã mua (bỏ qua nếu đơn đã có đúng hàng + đơn vị đó). Trả về true nếu đã thêm.
    function addHistoryLine(item, qty) {
        var exists = Array.prototype.some.call(rows(), function (row) {
            return row.querySelector(".order-line__product-id").value === String(item.productId)
                && row.querySelector(".order-line__unit").value === String(item.unitId);
        });
        if (exists) {
            return false;
        }
        var last = rows()[rows().length - 1];
        var blank = last && !last.querySelector(".order-line__product").value.trim()
            && !last.querySelector(".order-line__qty").value.trim();
        if (!blank) {
            tbody.appendChild(template.content.cloneNode(true));
            last = rows()[rows().length - 1];
        }
        last.querySelector(".order-line__product").value = item.text;
        last.querySelector(".order-line__product-id").value = item.productId;
        fillUnits(last, String(item.productId));
        last.querySelector(".order-line__unit").value = String(item.unitId);
        last.querySelector(".order-line__qty").value = qty;
        clearPrice(last);
        renumber();
        scheduleQuote();
        return true;
    }

    // Số lượng gợi ý khi thêm một mặt hàng: bình quân làm tròn, tối thiểu 1
    function suggestedQty(avg) {
        return String(Math.max(1, Math.round(parseFloat(avg) || 1)));
    }

    var history = {
        box: document.getElementById("order-history"),
        count: document.getElementById("order-history-count"),
        body: document.getElementById("order-history-body"),
        toggle: document.getElementById("order-history-toggle"),
        last: document.getElementById("order-history-last"),
        empty: document.getElementById("order-history-empty"),
        table: document.getElementById("order-history-table"),
        rows: document.getElementById("order-history-rows"),
        data: null
    };

    function cell(text, className) {
        var td = document.createElement("td");
        td.textContent = text;
        if (className) {
            td.className = className;
        }
        return td;
    }

    function showHistory(data) {
        history.data = data;
        history.box.hidden = !data;
        history.rows.innerHTML = "";
        if (!data) {
            return;
        }
        var hasItems = data.items.length > 0;
        history.count.textContent = hasItems ? data.items.length + " mặt hàng" : "Chưa có lần mua nào";
        history.empty.hidden = hasItems;
        history.table.hidden = !hasItems;
        history.toggle.hidden = false;
        history.last.hidden = !hasItems;
        if (hasItems) {
            history.last.textContent = "Thêm hàng lần mua trước (" + data.lastOrderNo + ", " + data.lastOrderDate + ")";
        }
        data.items.forEach(function (item, index) {
            var tr = document.createElement("tr");
            tr.appendChild(cell(item.text, "order-history__name"));
            tr.appendChild(cell(item.orders));
            tr.appendChild(cell(item.avgQty + " " + item.unitName));
            tr.appendChild(cell(item.lastDate));
            var action = document.createElement("td");
            var add = document.createElement("button");
            add.type = "button";
            add.className = "sales-button sales-button--small order-history__add";
            add.setAttribute("data-index", String(index));
            add.textContent = "+ Thêm";
            action.appendChild(add);
            tr.appendChild(action);
            history.rows.appendChild(tr);
        });
    }

    function loadHistory() {
        showHistory(null);
        if (!customerSelect.value) {
            return;
        }
        var url = form.getAttribute("data-history-url") + "?customerId=" + encodeURIComponent(customerSelect.value);
        fetch(url, {headers: {"Accept": "application/json"}, credentials: "same-origin"})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("history " + response.status);
                }
                return response.json();
            })
            .then(showHistory)
            .catch(function () {
                // Lịch sử chỉ là gợi ý: lỗi thì để ẩn, không chặn việc gõ đơn
            });
    }

    history.toggle.addEventListener("click", function () {
        var open = history.body.hidden;
        history.body.hidden = !open;
        history.toggle.setAttribute("aria-expanded", String(open));
        history.toggle.textContent = open ? "Thu gọn" : "Xem chi tiết";
    });

    history.rows.addEventListener("click", function (event) {
        var button = event.target.closest(".order-history__add");
        if (!button || !history.data) {
            return;
        }
        var item = history.data.items[Number(button.getAttribute("data-index"))];
        var added = addHistoryLine(item, suggestedQty(item.avgQty));
        button.textContent = added ? "Đã thêm" : "Đã có trong đơn";
        button.disabled = true;
    });

    history.last.addEventListener("click", function () {
        var added = 0;
        history.data.lastLines.forEach(function (line) {
            if (addHistoryLine(line, line.qty)) {
                added++;
            }
        });
        history.last.textContent = added > 0
            ? "Đã thêm " + added + " dòng từ " + history.data.lastOrderNo
            : "Các hàng của " + history.data.lastOrderNo + " đã có trong đơn";
    });

    // Tính tiền: gom các lần gõ liên tiếp thành một lần gọi server
    var quoteTimer = null;
    var quoteRequest = 0;

    function scheduleQuote() {
        clearTimeout(quoteTimer);
        quoteTimer = setTimeout(requestQuote, 250);
    }

    function setText(id, text) {
        document.getElementById(id).textContent = text;
    }

    function requestQuote() {
        if (!customerSelect.value) {
            return;
        }
        var requestId = ++quoteRequest;
        var body = new URLSearchParams(new FormData(form));
        fetch(form.getAttribute("data-quote-url"), {
            method: "POST",
            headers: {"Accept": "application/json"},
            body: body,
            credentials: "same-origin"
        }).then(function (response) {
            if (!response.ok || response.headers.get("Content-Type").indexOf("application/json") === -1) {
                throw new Error("quote " + response.status);
            }
            return response.json();
        }).then(function (data) {
            if (requestId !== quoteRequest) {
                return;
            }
            // Dòng để trống hoàn toàn không gửi lên server: ghép kết quả theo các dòng có dữ liệu
            var index = 0;
            alertsList.innerHTML = "";
            rows().forEach(function (row) {
                var blank = !row.querySelector(".order-line__product").value.trim()
                    && !row.querySelector(".order-line__qty").value.trim();
                var line = blank ? null : data.lines[index++];
                row.querySelector(".order-line__amount").textContent = line ? line.amount : "—";
                row.querySelector(".order-line__discount").textContent = line ? line.discount : "—";
                row.querySelector(".order-line__total").textContent = line ? line.total : "—";
                // Chưa gõ xong thì chưa báo lỗi, chỉ báo khi đã chọn hàng và nhập số lượng
                var ready = row.querySelector(".order-line__product-id").value
                    && row.querySelector(".order-line__qty").value.trim();
                setLineError(row, line && ready ? line.error : null);
                showPrice(row, line);
                showStock(row, line);
                if (line && !line.error && (line.manual || line.belowFloor)) {
                    alertsList.appendChild(alertItem(row.querySelector(".order-line__index").textContent, line));
                }
            });
            showCredit(data.credit);
            document.getElementById("order-stock-warehouse").hidden = !data.warehouse;
            setText("order-stock-warehouse-name", data.warehouse || "");
            if (data.credit && data.credit.overLimit) {
                alertsList.insertBefore(creditItem(data.credit), alertsList.firstChild);
            }
            alertsBox.hidden = alertsList.children.length === 0;
            alertsBox.classList.toggle("order-alerts--warning", data.needsApproval);
            alertsNote.hidden = !data.needsApproval;
            setText("order-subtotal", data.subtotal);
            setText("order-discount", data.discount);
            setText("order-total", data.total);
        }).catch(function () {
            // Mất kết nối hoặc hết phiên: giữ số cũ, server vẫn tính lại khi bấm lưu
        });
    }

    // overdue: câu báo nợ quá hạn từ server (null nếu không có). Đơn nháp đã có của đại lý quá hạn: lưu được, không gửi được
    function showBlocked(blocked, reason, overdue) {
        var editingOwnDraft = draftCustomer && draftCustomer === customerSelect.value;
        blockedWarning.hidden = !blocked;
        blockedWarningText.textContent = !blocked ? "" : "Lý do: " + reason + ". " + (editingOwnDraft
            ? "Đơn nháp này vẫn được xử lý tiếp, nhưng không tạo được đơn mới cho đại lý."
            : "Không tạo được đơn mới cho đại lý này.");
        overdueWarning.hidden = !overdue;
        overdueWarningText.textContent = !overdue ? "" : overdue + ". " + (editingOwnDraft
            ? "Đơn nháp này vẫn lưu được nhưng chưa gửi được cho tới khi đại lý thanh toán khoản quá hạn."
            : "Không tạo được đơn mới cho đại lý này.");
        saveButtons[0].disabled = Boolean(blocked || overdue) && !editingOwnDraft;
        saveButtons[1].disabled = blocked && !editingOwnDraft || Boolean(overdue);
    }

    function onCustomerChange() {
        addressSelect.innerHTML = "";
        var placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Chọn điểm giao";
        addressSelect.appendChild(placeholder);
        showBlocked(false, "", null);
        loadHistory();
        if (!customerSelect.value) {
            showCredit(null);
            alertsBox.hidden = true;
            return;
        }
        var url = form.getAttribute("data-customer-url") + "?customerId=" + encodeURIComponent(customerSelect.value);
        fetch(url, {headers: {"Accept": "application/json"}, credentials: "same-origin"})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("customer " + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                data.addresses.forEach(function (address) {
                    var option = document.createElement("option");
                    option.value = address.id;
                    option.textContent = address.text;
                    option.selected = address.isDefault;
                    addressSelect.appendChild(option);
                });
                showBlocked(data.blocked, data.blockReason || "", data.overdue);
                scheduleQuote();
            })
            .catch(function () {
                // Server kiểm tra lại khi lưu; ô điểm giao để trống sẽ báo lỗi rõ ràng lúc đó
            });
    }

    document.getElementById("add-order-line").addEventListener("click", addLine);

    tbody.addEventListener("input", function (event) {
        var row = event.target.closest(".order-line");
        if (event.target.classList.contains("order-line__product")) {
            onProductChange(row);
        } else if (event.target.classList.contains("order-line__qty")
            || event.target.classList.contains("order-line__price")) {
            scheduleQuote();
        }
    });

    // Enter trong ô dòng hàng (vd khi chọn gợi ý) không được gửi form: nút submit đầu tiên là Lưu nháp
    tbody.addEventListener("keydown", function (event) {
        if (event.key === "Enter" && event.target.classList.contains("order-line__control")) {
            event.preventDefault();
        }
    });

    tbody.addEventListener("change", function (event) {
        if (event.target.classList.contains("order-line__unit")) {
            clearPrice(event.target.closest(".order-line"));
            scheduleQuote();
        }
    });

    tbody.addEventListener("click", function (event) {
        var fill = event.target.closest(".order-line__stock-max");
        if (fill) {
            var qty = fill.closest(".order-line").querySelector(".order-line__qty");
            qty.value = fill.getAttribute("data-qty");
            qty.focus();
            scheduleQuote();
            return;
        }
        var reset = event.target.closest(".order-line__reset");
        if (reset) {
            var priceRow = reset.closest(".order-line");
            clearPrice(priceRow);
            priceRow.querySelector(".order-line__price").focus();
            scheduleQuote();
            return;
        }
        var button = event.target.closest(".order-line__remove");
        if (!button) {
            return;
        }
        var row = button.closest(".order-line");
        var next = row.nextElementSibling || row.previousElementSibling;
        row.remove();
        renumber();
        scheduleQuote();
        (next ? next.querySelector(".order-line__remove") : document.getElementById("add-order-line")).focus();
    });

    customerSelect.addEventListener("change", onCustomerChange);

    // Hộp "Mở lại" đơn nháp
    var dialog = document.getElementById("order-drafts");
    document.getElementById("open-drafts").addEventListener("click", function () {
        dialog.showModal();
    });
    document.getElementById("close-drafts").addEventListener("click", function () {
        dialog.close();
    });
    dialog.addEventListener("click", function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });

    // Mở lại đơn nháp đã có đại lý: nạp luôn lịch sử mua hàng
    if (customerSelect.value) {
        loadHistory();
    }

    // Đơn mới chưa có dòng nào: thêm sẵn một dòng trống để gõ ngay
    if (rows().length === 0) {
        tbody.appendChild(template.content.cloneNode(true));
    }
    renumber();
})();

// Chi tiết đơn hàng (S4-06): mở hộp Huỷ đơn / Ý nghĩa trạng thái, đóng bằng nút hoặc bấm ra ngoài;
// huỷ đơn bắt buộc lý do (server kiểm tra lại). Lưu lỗi lý do thì hộp huỷ tự mở lại (data-open).
(function () {
    var cancelDialog = document.getElementById("cancel-order-dialog");
    var legendDialog = document.getElementById("status-legend-dialog");
    if (!cancelDialog && !legendDialog) {
        return;
    }
    function open(dialog, focus) {
        if (dialog && typeof dialog.showModal === "function") {
            dialog.showModal();
            if (focus) {
                focus.focus();
            }
        }
    }
    document.querySelectorAll(".order-dialog").forEach(function (dialog) {
        dialog.addEventListener("click", function (event) {
            if (event.target === dialog || event.target.closest("[data-close-dialog]")) {
                dialog.close();
            }
        });
    });
    var legendButton = document.getElementById("open-status-legend");
    if (legendButton) {
        legendButton.addEventListener("click", function () { open(legendDialog); });
    }
    if (!cancelDialog) {
        return;
    }
    var reason = document.getElementById("cancel-reason");
    var openButton = document.getElementById("open-cancel-order");
    if (openButton) {
        openButton.addEventListener("click", function () { open(cancelDialog, reason); });
    }
    if (cancelDialog.getAttribute("data-open") === "1") {
        open(cancelDialog, reason);
    }
    var field = reason.closest(".sales-field");
    cancelDialog.querySelector("form").addEventListener("submit", function (event) {
        if (reason.value.trim()) {
            return;
        }
        event.preventDefault();
        var error = field.querySelector(".sales-field__error");
        if (!error) {
            error = document.createElement("p");
            error.className = "sales-field__error";
            field.appendChild(error);
        }
        error.textContent = "Vui lòng nhập lý do huỷ đơn.";
        field.classList.add("sales-field--invalid");
        reason.focus();
    });
})();
