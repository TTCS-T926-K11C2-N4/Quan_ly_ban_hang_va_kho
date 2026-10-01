// Import Excel (S2-01): chọn/kéo thả file ở bước 1, chặn bấm Import hai lần ở bước 2
(function () {
    var uploadForm = document.getElementById('import-upload-form');
    if (uploadForm) {
        initUpload(uploadForm);
    }

    var confirmForm = document.getElementById('import-confirm-form');
    if (confirmForm) {
        confirmForm.addEventListener('submit', function () {
            var button = document.getElementById('import-confirm');
            // Gửi email cho từng tài khoản nên có thể mất vài phút; khoá nút để không gửi lại
            button.disabled = true;
            button.textContent = button.dataset.busyText;
        });
    }

    function initUpload(form) {
        var maxBytes = Number(form.dataset.maxBytes);
        var input = document.getElementById('import-file');
        var dropzone = document.getElementById('import-dropzone');
        var fileRow = document.getElementById('import-selected-file');
        var fileName = document.getElementById('import-file-name');
        var fileSize = document.getElementById('import-file-size');
        var removeButton = document.getElementById('import-file-remove');
        var continueButton = document.getElementById('import-continue');
        var clientError = document.getElementById('import-client-error');
        var serverError = document.getElementById('import-upload-error');

        input.addEventListener('change', function () {
            showFile(input.files[0]);
        });

        ['dragenter', 'dragover'].forEach(function (type) {
            dropzone.addEventListener(type, function (event) {
                event.preventDefault();
                dropzone.classList.add('import-dropzone--active');
            });
        });
        ['dragleave', 'drop'].forEach(function (type) {
            dropzone.addEventListener(type, function (event) {
                event.preventDefault();
                dropzone.classList.remove('import-dropzone--active');
            });
        });
        dropzone.addEventListener('drop', function (event) {
            var files = event.dataTransfer.files;
            if (files.length === 0) {
                return;
            }
            input.files = files;
            showFile(files[0]);
        });

        removeButton.addEventListener('click', function () {
            input.value = '';
            showFile(null);
            input.focus();
        });

        form.addEventListener('submit', function (event) {
            if (!showFile(input.files[0])) {
                event.preventDefault();
                return;
            }
            continueButton.disabled = true;
            continueButton.textContent = 'Đang kiểm tra dữ liệu…';
        });

        // Trả về true nếu file hợp lệ để gửi lên
        function showFile(file) {
            var error = file ? validate(file) : '';
            clientError.textContent = error;
            clientError.hidden = !error;
            if (serverError && file) {
                serverError.hidden = true;
            }
            var ok = Boolean(file) && !error;
            fileRow.hidden = !ok;
            continueButton.disabled = !ok;
            if (ok) {
                fileName.textContent = file.name;
                fileSize.textContent = formatSize(file.size);
            } else if (file) {
                input.value = '';
            }
            return ok;
        }

        function validate(file) {
            if (!/\.(xlsx|xls)$/i.test(file.name)) {
                return 'Chỉ hỗ trợ file .xlsx hoặc .xls.';
            }
            if (file.size > maxBytes) {
                return 'File vượt quá dung lượng tối đa 10 MB.';
            }
            if (file.size === 0) {
                return 'File rỗng, hãy chọn file khác.';
            }
            return '';
        }

        function formatSize(bytes) {
            if (bytes < 1024 * 1024) {
                return Math.max(1, Math.round(bytes / 1024)) + ' KB';
            }
            return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        }
    }
})();
