<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="units"/>
<c:set var="breadcrumbSection" value="Đơn vị tính"/>
<%-- Dùng chung cho Thêm và Sửa đơn vị; unit = null khi thêm mới --%>
<c:set var="pageTitle" value="${empty unit ? 'Thêm đơn vị tính' : 'Sửa đơn vị tính'}"/>
<c:set var="breadcrumbPage" value="${pageTitle}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">
                    ${empty unit ? 'Thêm tên đơn vị để chọn làm đơn vị cơ sở hoặc đơn vị quy đổi của sản phẩm.' : 'Đổi mã hoặc tên không ảnh hưởng số liệu đã ghi.'}
                </p>
            </header>

            <form class="account-form" id="unit-form" method="post" action="<c:url value='${empty unit ? "/units/new" : "/units/edit"}'/>">
                <c:if test="${not empty unit}">
                    <input type="hidden" name="id" value="${unit.id}">
                </c:if>
                <section class="account-form__section" aria-labelledby="unit-info-title">
                    <h2 class="account-form__section-title" id="unit-info-title">Thông tin đơn vị</h2>
                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.code ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="unit-code">Mã đơn vị *</label>
                            <input class="form-group__control" type="text" id="unit-code" name="code" maxlength="20" required
                                   value="<c:out value='${code}'/>" placeholder="Ví dụ: KET" autocomplete="off"
                                   aria-describedby="unit-code-hint unit-code-error" aria-invalid="${not empty errors.code}">
                            <p class="form-group__hint" id="unit-code-hint">1–20 ký tự: chữ không dấu, số, gạch dưới, gạch ngang. Tự đổi sang chữ hoa.</p>
                            <p class="form-group__error" id="unit-code-error"${empty errors.code ? ' hidden' : ''}><c:out value="${errors.code}"/></p>
                        </div>
                        <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="unit-name">Tên đơn vị *</label>
                            <input class="form-group__control" type="text" id="unit-name" name="name" maxlength="50" required
                                   value="<c:out value='${name}'/>" placeholder="Ví dụ: Két" autocomplete="off"
                                   aria-describedby="unit-name-error" aria-invalid="${not empty errors.name}">
                            <p class="form-group__error" id="unit-name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                        </div>
                    </div>
                </section>
                <footer class="account-form__footer">
                    <a class="button button--secondary" href="<c:url value='/units'/>">Hủy</a>
                    <button class="button button--primary" type="submit" id="save-unit">${empty unit ? 'Thêm đơn vị' : 'Lưu thay đổi'}</button>
                </footer>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
