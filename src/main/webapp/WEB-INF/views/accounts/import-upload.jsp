<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="account-list"/>
<c:set var="breadcrumbSection" value="Quản lý tài khoản"/>
<c:set var="breadcrumbPage" value="Import Excel"/>
<c:set var="importStep" value="${1}"/>
<c:set var="importSubtitle" value="Tải lên file Excel để thêm mới hoặc cập nhật nhiều dữ liệu trong hệ thống."/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Import người dùng từ Excel | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/import.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content import-page">
            <%@ include file="/WEB-INF/views/accounts/import-header.jspf" %>

            <form class="import-card import-upload" id="import-upload-form" method="post" enctype="multipart/form-data"
                  action="<c:url value='/accounts/import'/>" data-max-bytes="10485760" novalidate>
                <div class="import-template">
                    <span class="import-template__icon" aria-hidden="true">▧</span>
                    <div class="import-template__text">
                        <h2 class="import-template__title">Tải file mẫu</h2>
                        <p class="import-template__desc">Sử dụng đúng cấu trúc file mẫu để tránh lỗi khi nhập dữ liệu.</p>
                    </div>
                    <a class="import-button import-button--outline" id="download-template-link"
                       href="<c:url value='/accounts/import/template'/>">↓ Tải xuống file mẫu</a>
                </div>

                <c:if test="${not empty error}">
                    <p class="import-alert" id="import-upload-error" role="alert"><c:out value="${error}"/></p>
                </c:if>
                <p class="import-alert" id="import-client-error" role="alert" hidden></p>

                <div class="import-dropzone" id="import-dropzone">
                    <span class="import-dropzone__icon" aria-hidden="true">↥</span>
                    <h3 class="import-dropzone__title">Kéo và thả file Excel vào đây</h3>
                    <p class="import-dropzone__desc">Hoặc chọn file từ máy tính của bạn</p>
                    <input class="visually-hidden" type="file" id="import-file" name="file"
                           accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel">
                    <label class="import-button import-button--primary" for="import-file">Chọn file</label>
                    <p class="import-dropzone__hint">Hỗ trợ định dạng .xlsx, .xls — dung lượng tối đa 10 MB</p>
                </div>

                <div class="import-file" id="import-selected-file" hidden>
                    <span class="import-file__icon" aria-hidden="true">X</span>
                    <div class="import-file__text">
                        <strong class="import-file__name" id="import-file-name"></strong>
                        <span class="import-file__size" id="import-file-size"></span>
                    </div>
                    <span class="import-file__status">✓ Đã chọn</span>
                    <button class="import-file__remove" type="button" id="import-file-remove" aria-label="Bỏ chọn file">×</button>
                </div>

                <div class="import-actions">
                    <a class="import-button import-button--secondary" id="cancel-import" href="<c:url value='/accounts'/>">Hủy</a>
                    <button class="import-button import-button--primary" type="submit" id="import-continue" disabled>Tiếp tục xem trước</button>
                </div>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/account-import.js'/>"></script>
</body>
</html>
