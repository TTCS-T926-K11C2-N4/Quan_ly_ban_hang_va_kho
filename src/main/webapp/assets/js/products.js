// Danh mục sản phẩm (S2-05): lọc ngay khi đổi ô chọn; form thêm/sửa xem trước ảnh và chặn ảnh quá 2MB.
// Menu ••• và hỏi xác nhận xoá/ngừng kinh doanh dùng chung categories.js.
(function () {
    var filterForm = document.getElementById("product-filter-form");
    if (filterForm) {
        ["product-category-filter", "product-status-filter", "product-warehouse-filter"].forEach(function (id) {
            document.getElementById(id).addEventListener("change", function () {
                filterForm.requestSubmit();
            });
        });
    }

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

    imageInput.addEventListener("change", function () {
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
    });
})();
