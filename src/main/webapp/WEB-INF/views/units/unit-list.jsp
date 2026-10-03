<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="units"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Đơn vị tính"/>
<c:set var="canManage" value="${currentUser.can('PRODUCT_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn vị tính | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Đơn vị tính</h1>
                <p class="page-header__subtitle">Danh mục tên đơn vị (lon, lốc, thùng, kg...). Hệ số quy đổi khai báo riêng cho từng sản phẩm ở form sản phẩm.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <c:if test="${canManage}">
                <div class="account-filters">
                    <a class="account-filters__create" id="create-unit-link" href="<c:url value='/units/new'/>">
                        <span aria-hidden="true">+</span> Thêm đơn vị
                    </a>
                </div>
            </c:if>

            <section class="account-table-card" aria-label="Danh sách đơn vị tính">
                <div class="account-table-scroll">
                    <table class="account-table" id="unit-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Mã đơn vị</th>
                                <th scope="col">Tên đơn vị</th>
                                <th scope="col">Số sản phẩm đang dùng</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="unit" items="${units}" varStatus="loop">
                                <tr>
                                    <td class="account-table__index">${loop.index + 1}</td>
                                    <td><c:out value="${unit.code}"/></td>
                                    <td class="account-table__name"><c:out value="${unit.name}"/></td>
                                    <td>${unit.productCount}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${canManage}">
                                                <div class="row-actions">
                                                    <a class="row-actions__link" href="<c:url value='/units/edit'><c:param name='id' value='${unit.id}'/></c:url>">Sửa</a>
                                                    <form action="<c:url value='/units/delete'/>" method="post"
                                                          data-confirm="Xóa đơn vị &quot;${fn:escapeXml(unit.name)}&quot;?">
                                                        <input type="hidden" name="id" value="${unit.id}">
                                                        <button class="row-actions__link row-actions__button" type="submit">Xóa</button>
                                                    </form>
                                                </div>
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty units}">
                                <tr><td class="category-table__empty" colspan="5">Chưa có đơn vị tính nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/categories.js'/>"></script>
</body>
</html>
