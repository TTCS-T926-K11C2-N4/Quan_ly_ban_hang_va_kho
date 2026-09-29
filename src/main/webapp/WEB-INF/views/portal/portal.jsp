<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="dashboard"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Trang chủ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Trang chủ đại lý | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/dashboard.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content">
            <header class="page-header">
                <h1 class="page-header__title">Xin chào, <c:out value="${currentUser.fullName}"/></h1>
                <p class="page-header__subtitle">Cổng đặt hàng dành cho đại lý: đặt hàng, theo dõi đơn và công nợ của bạn.</p>
            </header>

            <div class="portal-grid">
                <c:set var="canOrder" value="${currentUser.can('ORDER_MANAGE')}"/>
                <c:set var="canViewProducts" value="${currentUser.can('PRODUCT_VIEW')}"/>
                <c:if test="${canOrder or canViewProducts}">
                    <section class="panel panel--compact" aria-labelledby="portal-actions-title">
                        <h2 class="panel__title" id="portal-actions-title">Thao tác nhanh</h2>
                        <ul class="quick-actions">
                            <c:if test="${canOrder}">
                                <li>
                                    <a class="quick-action tone--light-blue" id="portal-create-order" href="<c:url value='/sales'/>">
                                        Đặt hàng<span class="quick-action__arrow" aria-hidden="true">→</span>
                                    </a>
                                </li>
                            </c:if>
                            <c:if test="${canViewProducts}">
                                <li>
                                    <a class="quick-action tone--light-green" id="portal-products" href="<c:url value='/products'/>">
                                        Xem sản phẩm và bảng giá<span class="quick-action__arrow" aria-hidden="true">→</span>
                                    </a>
                                </li>
                            </c:if>
                        </ul>
                    </section>
                </c:if>

                <section class="panel panel--compact" aria-labelledby="access-scope-title">
                    <h2 class="panel__title" id="access-scope-title">Phạm vi truy cập</h2>
                    <div class="access-scope">
                        <img class="access-scope__icon" src="<c:url value='/assets/img/icons/shield.svg'/>" alt="" width="24" height="24">
                        <div>
                            <p class="access-scope__role">Vai trò: <c:out value="${currentUser.roleName}"/></p>
                        </div>
                    </div>
                    <p class="access-scope__note">
                        Bạn chỉ xem được đơn hàng, hoá đơn và công nợ của chính đại lý mình.
                    </p>
                </section>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
