// Hoá đơn (S7-01): một hoá đơn chỉ gộp phiếu của cùng một đại lý; chọn phiếu của đại lý khác thì bỏ chọn các phiếu cũ
(function () {
    var form = document.getElementById("invoice-issue-form");
    if (!form) {
        return;
    }
    form.addEventListener("change", function (event) {
        var box = event.target;
        if (!box.matches('input[name="note_id"]') || !box.checked) {
            return;
        }
        form.querySelectorAll('input[name="note_id"]:checked').forEach(function (other) {
            if (other !== box && other.getAttribute("data-customer") !== box.getAttribute("data-customer")) {
                other.checked = false;
            }
        });
    });
})();
