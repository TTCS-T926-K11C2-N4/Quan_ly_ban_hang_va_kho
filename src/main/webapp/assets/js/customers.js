// Ô tiền (data-money): tự thêm dấu chấm ngăn nghìn khi gõ, server vẫn nhận cả số có hoặc không có dấu chấm
(function () {
    function groupThousands(digits) {
        return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    }

    document.querySelectorAll("[data-money]").forEach(function (input) {
        input.addEventListener("input", function () {
            var digits = input.value.replace(/\D/g, "");
            var formatted = digits === "" ? "" : groupThousands(digits.replace(/^0+(?=\d)/, ""));
            if (formatted !== input.value) {
                input.value = formatted;
            }
        });
    });

    // Vừa bấm Chỉnh sửa hoặc lưu bị lỗi: đưa con trỏ vào ô cần sửa
    var form = document.getElementById("credit-form");
    var invalid = form && form.querySelector(".sales-field--invalid .sales-field__control");
    if (invalid) {
        invalid.focus();
    } else if (form && window.location.hash === "#credit-form") {
        form.querySelector("#credit-limit").focus();
    }
})();

// Danh sách đại lý (S3-08): trên điện thoại các ô lọc thu gọn sau nút "Bộ lọc"; đang có ô lọc được chọn thì mở sẵn
(function () {
    var form = document.getElementById("customer-filter-form");
    var toggle = document.getElementById("customer-filter-toggle");
    if (!form || !toggle) {
        return;
    }
    toggle.addEventListener("click", function () {
        var open = form.classList.toggle("customer-filters--open");
        toggle.setAttribute("aria-expanded", String(open));
    });
})();

// Điểm giao hàng (S3-04): hỏi lại trước khi xoá / ngừng dùng; vừa bấm Sửa hoặc lưu lỗi thì đưa con trỏ vào ô cần sửa
(function () {
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.getAttribute("data-confirm"))) {
                event.preventDefault();
            }
        });
    });
    var deliveryForm = document.getElementById("delivery-form");
    if (deliveryForm) {
        var invalid = deliveryForm.querySelector(".sales-field--invalid .sales-field__control");
        (invalid || deliveryForm.querySelector("#delivery-address")).focus();
    }
})();

// Phân công nhân viên kinh doanh (S3-06): tick đại lý, ô chọn tất cả trên trang, đếm số đã chọn và chỉ bật nút
// Phân công khi đã chọn ít nhất một đại lý
(function () {
    var boxes = Array.prototype.slice.call(document.querySelectorAll("[data-assign-checkbox]"));
    var selectAll = document.getElementById("assign-select-all");
    var count = document.getElementById("assign-selected-count");
    var submit = document.getElementById("assign-submit");
    if (!boxes.length || !count || !submit) {
        return;
    }
    function refresh() {
        var checked = boxes.filter(function (box) { return box.checked; }).length;
        count.textContent = String(checked);
        submit.disabled = checked === 0;
        if (selectAll) {
            selectAll.checked = checked === boxes.length;
            selectAll.indeterminate = checked > 0 && checked < boxes.length;
        }
    }
    boxes.forEach(function (box) { box.addEventListener("change", refresh); });
    if (selectAll) {
        selectAll.addEventListener("change", function () {
            boxes.forEach(function (box) { box.checked = selectAll.checked; });
            refresh();
        });
    }
    refresh();
})();
