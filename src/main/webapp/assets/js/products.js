// Danh mục sản phẩm (S2-05): lọc ngay khi đổi ô chọn; form thêm/sửa xem trước ảnh (chọn hoặc kéo thả), chặn ảnh
// quá 2MB, công tắc trạng thái, bảng đơn vị quy đổi (S2-07). Menu ••• và hỏi xác nhận dùng chung categories.js.
(function () {
    var filterForm = document.getElementById("product-filter-form");
    if (filterForm) {
        ["product-category-filter", "product-status-filter", "product-warehouse-filter"].forEach(function (id) {
            document.getElementById(id).addEventListener("change", function () {
                filterForm.requestSubmit();
            });
        });
    }

    initImage();
    initStatusSwitch();
    initConversions();

    function initImage() {
        var imageInput = document.getElementById("product-image");
        if (!imageInput) {
            return;
        }
        var preview = document.getElementById("product-image-preview");
        var error = document.getElementById("product-image-error");
        var group = imageInput.closest(".form-group");
        var maxBytes = Number(imageInput.getAttribute("data-max-bytes"));
        var originalSrc = preview.src;

        function showError(message) {
            error.textContent = message;
            error.hidden = !message;
            group.classList.toggle("form-group--invalid", Boolean(message));
        }

        function check() {
            var file = imageInput.files[0];
            if (!file) {
                preview.src = originalSrc;
                showError("");
                return;
            }
            if (!/^image\/(jpeg|png)$/.test(file.type)) {
                imageInput.value = "";
                preview.src = originalSrc;
                showError("Chỉ nhận ảnh JPG hoặc PNG.");
                return;
            }
            if (file.size > maxBytes) {
                imageInput.value = "";
                preview.src = originalSrc;
                showError("Ảnh vượt quá dung lượng tối đa 2MB.");
                return;
            }
            showError("");
            preview.src = URL.createObjectURL(file);
        }

        imageInput.addEventListener("change", check);

        // Kéo thả: gán tệp thả vào cho ô file để gửi kèm form như khi chọn bằng nút
        var drop = document.getElementById("product-image-drop");
        ["dragenter", "dragover"].forEach(function (type) {
            drop.addEventListener(type, function (event) {
                event.preventDefault();
                drop.classList.add("product-image-field--dragover");
            });
        });
        ["dragleave", "drop"].forEach(function (type) {
            drop.addEventListener(type, function () {
                drop.classList.remove("product-image-field--dragover");
            });
        });
        drop.addEventListener("drop", function (event) {
            event.preventDefault();
            if (event.dataTransfer.files.length > 0) {
                imageInput.files = event.dataTransfer.files;
                check();
            }
        });
    }

    function initStatusSwitch() {
        var input = document.getElementById("product-status");
        if (!input || input.type !== "checkbox") {
            return;
        }
        var text = document.getElementById("product-status-text");
        input.addEventListener("change", function () {
            text.textContent = text.getAttribute(input.checked ? "data-on" : "data-off");
        });
    }

    // Cùng quy tắc với ProductService.validateConversions (server vẫn kiểm tra lại)
    function initConversions() {
        var table = document.getElementById("conversion-table");
        var template = document.getElementById("conversion-template");
        if (!table || !template) {
            return;
        }
        var body = table.tBodies[0];
        var empty = document.getElementById("conversion-empty");
        var baseUnit = document.getElementById("product-unit");

        function selectedText(select) {
            var option = select.options[select.selectedIndex];
            return option && option.value ? option.text : "";
        }

        function parseFactor(text) {
            var value = (text || "").trim();
            if (!/^\d{1,14}([.,]\d{1,4})?$/.test(value)) {
                return null;
            }
            var factor = Number(value.replace(",", "."));
            return factor > 0 && factor !== 1 ? factor : null;
        }

        // strict = true khi bấm Lưu: dòng còn trống cũng báo lỗi (lúc đang nhập thì chưa báo để khỏi rối)
        function refresh(strict) {
            var rows = Array.prototype.slice.call(body.rows);
            var invalid = null;
            var chosen = {};
            rows.forEach(function (row) {
                var unit = row.querySelector(".conversion-row__unit").value;
                if (unit) {
                    chosen[unit] = (chosen[unit] || 0) + 1;
                }
            });
            var baseName = selectedText(baseUnit) || "đơn vị cơ sở";
            rows.forEach(function (row) {
                var select = row.querySelector(".conversion-row__unit");
                var input = row.querySelector(".conversion-row__factor");
                var option = select.options[select.selectedIndex];
                row.querySelector(".conversion-row__code").textContent = option && option.value
                    ? option.getAttribute("data-code") : "—";

                var factor = parseFactor(input.value);
                var message = "";
                var hasFactor = Boolean(input.value.trim());
                if (strict === true && !select.value && !hasFactor) {
                    message = "Chọn đơn vị và nhập hệ số, hoặc bấm × để xoá dòng.";
                } else if (strict === true && !select.value) {
                    message = "Chọn đơn vị.";
                } else if (strict === true && !hasFactor) {
                    message = "Vui lòng nhập hệ số quy đổi.";
                } else if (select.value && select.value === baseUnit.value) {
                    message = "Trùng đơn vị tính cơ sở.";
                } else if (select.value && chosen[select.value] > 1) {
                    message = "Đơn vị này đã có ở dòng khác.";
                } else if (input.value.trim() && factor === null) {
                    message = "Hệ số là số dương khác 1, tối đa 4 chữ số thập phân.";
                }
                row.querySelector(".conversion-row__hint").textContent = select.value && factor !== null && !message
                    ? "1 " + selectedText(select) + " = " + input.value.trim() + " " + baseName : "";

                var error = row.querySelector(".conversion-row__client-error");
                if (!error) {
                    error = document.createElement("p");
                    error.className = "form-group__error conversion-row__client-error";
                    select.closest("td").appendChild(error);
                }
                error.textContent = message;
                error.hidden = !message;
                row.classList.toggle("conversion-row--invalid", Boolean(message));
                if (message && !invalid) {
                    invalid = row;
                }
            });
            empty.hidden = rows.length > 0;
            return invalid;
        }

        table.closest("form").addEventListener("submit", function (event) {
            var invalid = refresh(true);
            if (invalid) {
                event.preventDefault();
                invalid.querySelector(".conversion-row__unit").focus();
            }
        });

        function refreshTyping() {
            refresh(false);
        }

        refreshTyping();
        baseUnit.addEventListener("change", refreshTyping);
        body.addEventListener("change", refreshTyping);
        body.addEventListener("input", refreshTyping);
        body.addEventListener("click", function (event) {
            var button = event.target.closest(".conversion-row__remove");
            if (button) {
                button.closest(".conversion-row").remove();
                refreshTyping();
            }
        });
        document.getElementById("add-conversion").addEventListener("click", function () {
            var row = template.content.firstElementChild.cloneNode(true);
            body.appendChild(row);
            refreshTyping();
            row.querySelector(".conversion-row__unit").focus();
        });
    }
})();
