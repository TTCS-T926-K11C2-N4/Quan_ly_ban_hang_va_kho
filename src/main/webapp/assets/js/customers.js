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
