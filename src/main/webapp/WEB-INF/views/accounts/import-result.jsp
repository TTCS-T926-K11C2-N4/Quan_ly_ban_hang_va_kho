<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="account-list"/>
<c:set var="breadcrumbSection" value="Quản lý tài khoản"/>
<c:set var="breadcrumbPage" value="Import Excel"/>
<c:set var="importStep" value="${3}"/>
<c:set var="importSubtitle" value="Xem kết quả xử lý dữ liệu sau khi hoàn tất quá trình import."/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Kết quả import | Hệ thống quản lý bán hàng &amp; kho</title>
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

            <section class="import-card import-result" aria-labelledby="import-result-title">
                <span class="import-result__icon" aria-hidden="true">✓</span>
                <h2 class="import-result__title" id="import-result-title">Import dữ liệu hoàn tất</h2>
                <p class="import-result__desc">Hệ thống đã xử lý xong file <strong><c:out value="${result.fileName}"/></strong></p>

                <dl class="import-result__stats">
                    <div class="import-result__stat">
                        <dt>Tổng số dòng</dt>
                        <dd>${result.totalCount}</dd>
                    </div>
                    <div class="import-result__stat import-result__stat--created">
                        <dt>Thêm mới thành công</dt>
                        <dd>${result.createdCount}</dd>
                    </div>
                    <div class="import-result__stat import-result__stat--updated">
                        <dt>Cập nhật thành công</dt>
                        <dd>${result.updateCount}</dd>
                    </div>
                    <div class="import-result__stat import-result__stat--error">
                        <dt>Dòng bị lỗi</dt>
                        <dd>${result.errorCount}</dd>
                    </div>
                </dl>

                <c:if test="${result.errorCount > 0}">
                    <div class="import-result__warning" role="status">
                        <p class="import-result__warning-title">⚠ ${result.errorCount} dòng chưa được nhập do dữ liệu không hợp lệ.</p>
                        <p>Bạn có thể tải báo cáo lỗi, chỉnh sửa dữ liệu và thực hiện import lại.</p>
                    </div>
                </c:if>

                <div class="import-result__actions">
                    <c:if test="${result.errorCount > 0}">
                        <a class="import-button import-button--secondary" id="download-error-report"
                           href="<c:url value='/accounts/import/errors'/>">↓ Tải báo cáo lỗi</a>
                    </c:if>
                    <a class="import-button import-button--secondary" id="import-another"
                       href="<c:url value='/accounts/import'/>">Import file khác</a>
                    <a class="import-button import-button--primary" id="back-to-accounts"
                       href="<c:url value='/accounts'/>">Quay về danh sách</a>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
