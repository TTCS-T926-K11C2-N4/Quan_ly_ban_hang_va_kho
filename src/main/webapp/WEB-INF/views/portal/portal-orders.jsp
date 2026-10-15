<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="portal-orders"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Đơn hàng của tôi"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn hàng của tôi | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/portal.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content portal-orders">
            <header class="page-header">
                <h1 class="page-header__title">Đơn hàng của tôi</h1>
                <p class="page-header__subtitle">Theo dõi trạng thái, ngày giao dự kiến và tổng tiền các đơn hàng của đại lý bạn.</p>
            </header>

            <c:choose>
                <c:when test="${empty orders}">
                    <p class="sales-empty" id="portal-orders-empty">Bạn chưa có đơn hàng nào.</p>
                </c:when>
                <c:otherwise>
                    <section class="sales-card portal-orders__card" aria-label="Danh sách đơn hàng">
                        <table class="portal-orders__table" id="portal-orders-table">
                            <thead>
                                <tr>
                                    <th scope="col">Mã đơn</th>
                                    <th scope="col">Ngày đặt</th>
                                    <th scope="col">Ngày giao dự kiến</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col" class="portal-orders__num">Tổng tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="order" items="${orders}">
                                    <tr>
                                        <td data-label="Mã đơn"><a class="portal-orders__link" href="<c:url value='/portal/orders/view'><c:param name='id' value='${order.id}'/></c:url>"><c:out value="${order.orderNo}"/></a></td>
                                        <td data-label="Ngày đặt"><c:out value="${order.createdAtText}"/></td>
                                        <td data-label="Ngày giao dự kiến"><c:out value="${empty order.requestedDateText ? '—' : order.requestedDateText}"/></td>
                                        <td data-label="Trạng thái"><span class="order-status order-status--${order.statusTone}">${order.statusLabel}</span></td>
                                        <td data-label="Tổng tiền" class="portal-orders__num"><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </section>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
