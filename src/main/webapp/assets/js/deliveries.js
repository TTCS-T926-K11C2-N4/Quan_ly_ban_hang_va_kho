// Xác nhận giao hàng (S6-08).
// Danh sách: đổi ngày / bộ lọc là tải lại. Chi tiết điểm giao: đếm số đơn trong phạm vi, phải chọn ít nhất một đơn.
// Giao thành công: chọn nhiều lần để gom ảnh biên bản (tối đa 5 ảnh JPG/PNG, 5MB mỗi ảnh), xem trước, xoá ảnh; nút
// xác nhận mở khi có ảnh và tên người nhận. Giao không thành công: mở nút khi đã chọn lý do. Server kiểm tra lại hết.
(function () {
    var filterForm = document.getElementById("delivery-filter-form");
    if (filterForm) {
        Array.prototype.forEach.call(filterForm.querySelectorAll("[data-auto-submit]"), function (control) {
            control.addEventListener("change", function () {
                filterForm.submit();
            });
        });
    }

    // ----- Phạm vi hành động ở chi tiết điểm giao -----
    var scopeChecks = document.querySelectorAll("[data-scope-check]");
    var scopeCount = document.querySelector("[data-scope-count]");
    var scopeButtons = document.querySelectorAll("[data-scope-button]");
    function refreshScope() {
        var count = 0;
        Array.prototype.forEach.call(scopeChecks, function (check) {
            if (check.checked) {
                count++;
            }
        });
        if (scopeCount) {
            scopeCount.textContent = count;
        }
        Array.prototype.forEach.call(scopeButtons, function (button) {
            button.disabled = count === 0;
        });
    }
    Array.prototype.forEach.call(scopeChecks, function (check) {
        check.addEventListener("change", refreshScope);
    });
    if (scopeChecks.length) {
        refreshScope();
    }

    // ----- Ghi nhận giao -----
    var form = document.getElementById("delivery-confirm-form");
    if (!form) {
        return;
    }
    var submit = form.querySelector("[data-confirm-submit]");
    var lockIcon = form.querySelector("[data-lock-icon]");
    var readyIcon = form.querySelector("[data-ready-icon]");
    var condition = form.querySelector("[data-condition]");
    var success = form.getAttribute("data-mode") === "success";

    function setReady(ready, message) {
        submit.disabled = !ready;
        lockIcon.hidden = ready;
        readyIcon.hidden = !ready;
        if (message) {
            condition.textContent = message;
        }
    }

    if (!success) {
        var refreshReason = function () {
            var checked = form.querySelector("[data-reason]:checked");
            var remark = document.getElementById("delivery-remark");
            if (!checked) {
                setReady(false, "Chọn một lý do giao không thành công.");
            } else if (checked.hasAttribute("data-other-reason") && !remark.value.trim()) {
                setReady(false, "Ghi rõ lý do khác ở ghi chú bổ sung.");
            } else {
                setReady(true, "Đã chọn lý do: " + checked.value + ".");
            }
        };
        form.addEventListener("change", refreshReason);
        form.addEventListener("input", refreshReason);
        refreshReason();
        return;
    }

    var maxProofs = parseInt(form.getAttribute("data-max-proofs"), 10) || 5;
    var maxBytes = 5 * 1024 * 1024;
    var input = form.querySelector("[data-proof-input]");
    var list = form.querySelector("[data-proof-list]");
    var empty = form.querySelector("[data-proof-empty]");
    var countLabel = form.querySelector("[data-proof-count]");
    var recipient = document.getElementById("recipient-name");
    var deliveredAt = document.getElementById("delivered-at");
    var files = [];
    var notice = "";
    var canAssign = typeof DataTransfer === "function";

    function syncInput() {
        if (!canAssign) {
            return;
        }
        var transfer = new DataTransfer();
        files.forEach(function (file) {
            transfer.items.add(file);
        });
        input.files = transfer.files;
    }

    function render() {
        list.textContent = "";
        files.forEach(function (file, index) {
            var item = document.createElement("li");
            item.className = "delivery-proof";
            var image = document.createElement("img");
            image.alt = "Ảnh biên bản " + (index + 1);
            image.src = URL.createObjectURL(file);
            image.onload = function () {
                URL.revokeObjectURL(image.src);
            };
            var caption = document.createElement("p");
            caption.className = "delivery-proof__caption";
            var check = document.createElement("span");
            check.className = "inv-icon inv-icon--circle-check";
            check.setAttribute("aria-hidden", "true");
            var name = document.createElement("span");
            name.className = "delivery-proof__name";
            name.textContent = "Phiếu giao hàng có chữ ký · " + file.name;
            caption.appendChild(check);
            caption.appendChild(name);
            var remove = document.createElement("button");
            remove.type = "button";
            remove.className = "delivery-proof__remove";
            remove.title = "Xóa ảnh";
            remove.setAttribute("aria-label", "Xóa ảnh " + file.name);
            var trash = document.createElement("span");
            trash.className = "inv-icon delivery-icon--trash";
            trash.setAttribute("aria-hidden", "true");
            remove.appendChild(trash);
            remove.addEventListener("click", function () {
                files.splice(index, 1);
                notice = "";
                syncInput();
                render();
            });
            item.appendChild(image);
            item.appendChild(caption);
            item.appendChild(remove);
            list.appendChild(item);
        });
        list.hidden = files.length === 0;
        empty.hidden = files.length > 0;
        countLabel.textContent = files.length;
        refresh();
    }

    // notice: ảnh vừa bị bỏ qua (sai định dạng, quá dung lượng, quá số ảnh) - chỉ báo, không chặn
    function refresh() {
        var count = canAssign ? files.length : input.files.length;
        var ready = false;
        var message;
        if (count === 0) {
            message = "Cần ít nhất 1 ảnh biên bản có chữ ký.";
        } else if (!recipient.value.trim()) {
            message = "Nhập tên người nhận hàng.";
        } else if (!deliveredAt.value) {
            message = "Chọn thời điểm giao hàng.";
        } else {
            ready = true;
            message = "Đủ điều kiện: " + count + " ảnh biên bản, người nhận " + recipient.value.trim() + ".";
        }
        setReady(ready, notice ? notice + " " + message : message);
    }

    input.addEventListener("change", function () {
        notice = "";
        if (!canAssign) {
            refresh();
            return;
        }
        Array.prototype.forEach.call(input.files, function (file) {
            if (!/^image\/(jpeg|png)$/.test(file.type)) {
                notice = "Ảnh \"" + file.name + "\" không phải JPG hoặc PNG, đã bỏ qua.";
            } else if (file.size > maxBytes) {
                notice = "Ảnh \"" + file.name + "\" vượt 5MB, đã bỏ qua.";
            } else if (files.length >= maxProofs) {
                notice = "Tối đa " + maxProofs + " ảnh biên bản.";
            } else {
                files.push(file);
            }
        });
        syncInput();
        render();
    });
    recipient.addEventListener("input", refresh);
    deliveredAt.addEventListener("input", refresh);
    render();
})();
