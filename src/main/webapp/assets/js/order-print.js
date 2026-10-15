// Phiếu in đơn hàng (S4-08): nút In / Lưu PDF mở hộp in của trình duyệt
(function () {
    var button = document.getElementById("print-order");
    if (button) {
        button.addEventListener("click", function () {
            window.print();
        });
    }
})();
