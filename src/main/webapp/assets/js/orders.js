// Màn Tạo đơn hàng (S3-09): thêm/xoá dòng hàng, chọn hàng theo gợi ý mã/tên, đổi đơn vị tính,
// đổi đại lý thì nạp lại điểm giao; mỗi lần sửa thì hỏi server tính lại tiền (/orders/quote).
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
    var saveButtons = [document.getElementById("save-draft"), document.getElementById("submit-order")];
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
        }
        scheduleQuote();
    }

    function addLine() {
        tbody.appendChild(template.content.cloneNode(true));
        renumber();
        var row = rows()[rows().length - 1];
        row.querySelector(".order-line__product").focus();
    }

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
            });
            setText("order-subtotal", data.subtotal);
            setText("order-discount", data.discount);
            setText("order-total", data.total);
        }).catch(function () {
            // Mất kết nối hoặc hết phiên: giữ số cũ, server vẫn tính lại khi bấm lưu
        });
    }

    function showBlocked(blocked, reason) {
        var editingOwnDraft = draftCustomer && draftCustomer === customerSelect.value;
        blockedWarning.hidden = !blocked;
        blockedWarningText.textContent = !blocked ? "" : "Lý do: " + reason + ". " + (editingOwnDraft
            ? "Đơn nháp này vẫn được xử lý tiếp, nhưng không tạo được đơn mới cho đại lý."
            : "Không tạo được đơn mới cho đại lý này.");
        saveButtons.forEach(function (button) {
            button.disabled = blocked && !editingOwnDraft;
        });
    }

    function onCustomerChange() {
        addressSelect.innerHTML = "";
        var placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Chọn điểm giao";
        addressSelect.appendChild(placeholder);
        showBlocked(false, "");
        if (!customerSelect.value) {
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
                showBlocked(data.blocked, data.blockReason || "");
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
        } else if (event.target.classList.contains("order-line__qty")) {
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
            scheduleQuote();
        }
    });

    tbody.addEventListener("click", function (event) {
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

    // Đơn mới chưa có dòng nào: thêm sẵn một dòng trống để gõ ngay
    if (rows().length === 0) {
        tbody.appendChild(template.content.cloneNode(true));
    }
    renumber();
})();
