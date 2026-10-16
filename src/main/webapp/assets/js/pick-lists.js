// Phiếu soạn hàng (S6-03).
// Danh sách: tự lọc khi đổi ô chọn; đánh dấu đơn (cùng kho) để bật nút "Tạo phiếu soạn".
// Màn soạn hàng: bấm nút tròn đánh dấu đã soạn (điền sẵn SL lấy = SL cần), SL lấy nhỏ hơn SL cần thì hiện ô lý do,
// cập nhật tiến độ ngay; in phiếu; hỏi lại trước khi huỷ phiếu. Server vẫn kiểm tra lại khi lưu.
(function () {
    var filterForm = document.getElementById("pick-filter-form");
    if (filterForm) {
        Array.prototype.forEach.call(filterForm.querySelectorAll("[data-auto-submit]"), function (control) {
            control.addEventListener("change", function () {
                filterForm.submit();
            });
        });
    }

    // ----- Chọn đơn để gộp phiếu soạn -----
    var createButton = document.getElementById("pick-create");
    var hint = document.getElementById("pick-selection");
    var defaultHint = hint ? hint.textContent : "";
    var boxes = document.querySelectorAll("input[name='orderId'][form='pick-create-form']");

    function refreshSelection() {
        var checked = [];
        Array.prototype.forEach.call(boxes, function (box) {
            var row = box.closest("tr");
            if (row) {
                row.classList.toggle("is-selected", box.checked);
            }
            if (box.checked) {
                checked.push(box);
            }
        });
        var warehouses = {};
        checked.forEach(function (box) {
            warehouses[box.getAttribute("data-warehouse")] = box.getAttribute("data-warehouse-name");
        });
        var names = Object.keys(warehouses).map(function (key) {
            return warehouses[key];
        });
        var mixed = names.length > 1;
        if (createButton) {
            createButton.disabled = checked.length === 0 || mixed;
        }
        if (hint) {
            hint.classList.toggle("pick-section__hint--warning", mixed);
            if (mixed) {
                hint.textContent = "Các đơn đã chọn thuộc nhiều kho (" + names.join(", ")
                    + "). Một phiếu soạn chỉ gộp đơn cùng kho xuất.";
            } else if (checked.length > 0) {
                hint.textContent = "Đã chọn " + checked.length + " đơn tại " + names[0] + ".";
            } else {
                hint.textContent = defaultHint;
            }
        }
    }

    if (boxes.length) {
        Array.prototype.forEach.call(boxes, function (box) {
            box.addEventListener("change", refreshSelection);
        });
        Array.prototype.forEach.call(document.querySelectorAll("[data-check-all]"), function (all) {
            var group = all.getAttribute("data-check-all");
            all.addEventListener("change", function () {
                Array.prototype.forEach.call(boxes, function (box) {
                    if (box.getAttribute("data-group") === group) {
                        box.checked = all.checked;
                    }
                });
                refreshSelection();
            });
        });
        refreshSelection();
    }

    // ----- Màn soạn hàng -----
    var printButton = document.querySelector("[data-print]");
    if (printButton) {
        printButton.addEventListener("click", function () {
            window.print();
        });
    }

    Array.prototype.forEach.call(document.querySelectorAll("[data-confirm]"), function (button) {
        button.addEventListener("click", function (event) {
            if (!window.confirm(button.getAttribute("data-confirm"))) {
                event.preventDefault();
            }
        });
    });

    var list = document.getElementById("pick-lines");
    if (!list) {
        return;
    }
    var lines = list.querySelectorAll(".pick-line");
    var pickedCount = document.querySelector("[data-picked-count]");
    var progressBar = document.querySelector(".pick-progress__bar");
    var progressFill = document.querySelector(".pick-progress__fill");

    function parseQty(text) {
        var value = (text || "").trim().replace(",", ".");
        return /^\d{1,12}(\.\d{1,3})?$/.test(value) ? parseFloat(value) : null;
    }

    function refreshLine(line) {
        var check = line.querySelector("[data-pick-check]");
        var qty = line.querySelector("[data-pick-qty]");
        var reason = line.querySelector("[data-pick-reason]");
        if (!check || !qty) {
            return;
        }
        var required = parseFloat(line.getAttribute("data-required"));
        var picked = check.checked;
        var value = parseQty(qty.value);
        line.classList.toggle("is-picked", picked);
        if (reason) {
            reason.classList.toggle("is-hidden", !(picked && value !== null && value < required));
        }
    }

    function refreshProgress() {
        if (!pickedCount) {
            return;
        }
        var done = 0;
        Array.prototype.forEach.call(lines, function (line) {
            var check = line.querySelector("[data-pick-check]");
            if (check && check.checked) {
                done++;
            }
        });
        pickedCount.textContent = done;
        if (progressBar && progressFill) {
            progressBar.setAttribute("aria-valuenow", done);
            progressFill.style.width = (lines.length ? Math.round(done * 100 / lines.length) : 0) + "%";
        }
    }

    Array.prototype.forEach.call(lines, function (line) {
        var check = line.querySelector("[data-pick-check]");
        var qty = line.querySelector("[data-pick-qty]");
        if (!check || !qty) {
            return;
        }
        check.addEventListener("change", function () {
            if (check.checked && qty.value.trim() === "") {
                qty.value = line.getAttribute("data-required");
            }
            if (!check.checked) {
                qty.value = "";
            }
            refreshLine(line);
            refreshProgress();
        });
        qty.addEventListener("input", function () {
            // Gõ số lượng là đã soạn dòng đó
            if (qty.value.trim() !== "" && !check.checked) {
                check.checked = true;
                refreshProgress();
            }
            refreshLine(line);
        });
        refreshLine(line);
    });
    refreshProgress();
})();
