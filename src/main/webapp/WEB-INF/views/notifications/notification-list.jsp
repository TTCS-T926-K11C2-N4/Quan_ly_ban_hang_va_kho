<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="notifications"/>
<c:set var="breadcrumbSection" value="Tài khoản"/>
<c:set var="breadcrumbPage" value="Thông báo"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thông báo | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/invoices.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Thông báo</h1>
                    <p class="inv-header__subtitle">Nhắc hạn thanh toán và các thông báo khác gửi cho tài khoản của bạn.</p>
                </div>
                <c:if test="${unreadNotificationCount > 0}">
                    <form class="inv-header__actions" action="<c:url value='/notifications'/>" method="post">
                        <button class="inv-button" type="submit" id="notifications-read-all">Đánh dấu đã đọc tất cả</button>
                    </form>
                </c:if>
            </header>

            <section class="inv-card" aria-label="Danh sách thông báo">
                <ul class="notification-list" id="notification-list">
                    <c:forEach var="item" items="${notifications}">
                        <li class="notification-list__item${item.unread ? ' notification-list__item--unread' : ''}">
                            <p class="notification-list__title"><c:out value="${item.title}"/></p>
                            <p class="notification-list__body"><c:out value="${item.body}"/></p>
                            <p class="notification-list__time"><c:out value="${item.createdAtText}"/></p>
                        </li>
                    </c:forEach>
                </ul>
                <c:if test="${empty notifications}">
                    <p class="inv-table__empty" id="notification-empty">Bạn chưa có thông báo nào.</p>
                </c:if>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
