// Hồ sơ cá nhân (S2-03): chọn ảnh là kiểm tra định dạng, dung lượng rồi tải lên ngay
(function () {
    var form = document.getElementById("profile-avatar-form");
    var input = document.getElementById("profile-avatar-file");
    var error = document.getElementById("profile-avatar-error");
    var button = document.getElementById("profile-avatar-upload");
    var buttonText = document.getElementById("profile-avatar-upload-text");
    var maxBytes = Number(form.dataset.maxBytes);

    input.addEventListener("change", function () {
        var file = input.files[0];
        if (!file) {
            return;
        }
        // Server kiểm tra lại theo nội dung tệp; ở đây chỉ báo sớm cho người dùng
        var message = !/\.(jpe?g|png)$/i.test(file.name) ? "Chỉ nhận ảnh JPG hoặc PNG."
            : file.size > maxBytes ? "Ảnh vượt quá dung lượng tối đa 2MB." : "";
        error.textContent = message;
        error.hidden = !message;
        if (message) {
            input.value = "";
            return;
        }
        button.setAttribute("aria-disabled", "true");
        buttonText.textContent = "Đang tải ảnh…";
        form.submit();
    });
})();
