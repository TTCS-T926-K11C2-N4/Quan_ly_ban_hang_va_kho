(function () {
    var form = document.getElementById("avatar-form");
    var input = document.getElementById("avatar-file");
    var submit = document.getElementById("avatar-submit");
    var error = document.getElementById("avatar-error");
    var image = document.getElementById("avatar-image");
    var initials = document.getElementById("avatar-initials");

    // Cùng giới hạn với AvatarService (server vẫn kiểm tra lại theo nội dung tệp)
    var MAX_BYTES = 2 * 1024 * 1024;
    var ALLOWED_TYPES = ["image/jpeg", "image/png"];
    var previewUrl = null;

    function check(file) {
        if (!file) {
            return "Vui lòng chọn ảnh.";
        }
        if (ALLOWED_TYPES.indexOf(file.type) < 0) {
            return "Chỉ chấp nhận ảnh JPG hoặc PNG.";
        }
        return file.size > MAX_BYTES ? "Ảnh tối đa 2MB." : "";
    }

    function showError(message) {
        error.textContent = message;
        error.hidden = !message;
    }

    // Xem trước ảnh vừa chọn; ảnh đã lưu sẽ được cắt vuông ở giữa giống object-fit: cover
    input.addEventListener("change", function () {
        var file = input.files[0];
        var message = check(file);
        showError(message);
        submit.hidden = Boolean(message);
        if (message) {
            return;
        }
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
        }
        previewUrl = URL.createObjectURL(file);
        image.src = previewUrl;
        image.alt = "Ảnh xem trước";
        image.hidden = false;
        if (initials) {
            initials.hidden = true;
        }
        submit.focus();
    });

    form.addEventListener("submit", function (event) {
        var message = check(input.files[0]);
        if (message) {
            event.preventDefault();
            showError(message);
            return;
        }
        // Chặn bấm nhiều lần khi đang tải ảnh lên
        submit.disabled = true;
        submit.textContent = "Đang tải lên...";
    });
})();
