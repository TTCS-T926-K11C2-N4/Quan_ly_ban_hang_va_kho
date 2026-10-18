<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="portal-debts"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Công nợ của tôi"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Công nợ của tôi | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/portal.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page portal-orders">
            <header class="page-header">
                <h1 class="page-header__title">Công nợ của tôi</h1>
                <p class="page-header__subtitle">Hoá đơn đã phát hành, số tiền còn nợ và hạn mức còn được lấy hàng.</p>
            </header>

            <section class="sales-card portal-debt-summary" id="portal-debt-summary" aria-label="Tổng hợp công nợ">
                <dl class="portal-debt-summary__list">
                    <div><dt>Tổng còn nợ hoá đơn</dt><dd id="portal-debt-outstanding"><fmt:formatNumber value="${outstanding}" maxFractionDigits="0"/> ₫</dd></div>
                    <c:if test="${credit != null}">
                        <div><dt>Hạn mức công nợ</dt><dd id="portal-debt-limit"><c:choose><c:when test="${credit.limitGranted}"><fmt:formatNumber value="${credit.creditLimit}" maxFractionDigits="0"/> ₫</c:when><c:otherwise>Chưa cấp hạn mức</c:otherwise></c:choose></dd></div>
                        <c:if test="${credit.limitGranted}">
                            <div><dt>Hạn mức còn lại</dt><dd id="portal-debt-available"><fmt:formatNumber value="${credit.available}" maxFractionDigits="0"/> ₫</dd></div>
                        </c:if>
                        <c:if test="${credit.overdue}">
                            <div><dt>Quá hạn thanh toán</dt><dd id="portal-debt-overdue"><fmt:formatNumber value="${credit.overdueAmount}" maxFractionDigits="0"/> ₫</dd></div>
                        </c:if>
                    </c:if>
                </dl>
            </section>

            <c:choose>
                <c:when test="${empty invoices}">
                    <p class="sales-empty" id="portal-debts-empty">Bạn chưa có hoá đơn nào.</p>
                </c:when>
                <c:otherwise>
                    <section class="sales-card portal-orders__card" aria-label="Danh sách hoá đơn">
                        <table class="portal-orders__table" id="portal-debts-table">
                            <thead>
                                <tr>
                                    <th scope="col">Số hoá đơn</th>
                                    <th scope="col">Ngày lập</th>
                                    <th scope="col">Hạn thanh toán</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col" class="portal-orders__num">Tổng tiền</th>
                                    <th scope="col" class="portal-orders__num">Còn nợ</th>
                                    <th scope="col"><span class="visually-hidden">Hoá đơn</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="invoice" items="${invoices}">
                                    <tr>
                                        <td data-label="Số hoá đơn"><c:out value="${invoice.invoiceNo}"/></td>
                                        <td data-label="Ngày lập"><c:out value="${invoice.issueDateText}"/></td>
                                        <td data-label="Hạn thanh toán"><c:out value="${invoice.dueDateText}"/><c:if test="${invoice.overdue}"> <span class="order-status order-status--danger">Quá hạn</span></c:if></td>
                                        <td data-label="Trạng thái"><c:out value="${invoice.statusLabel}"/></td>
                                        <td data-label="Tổng tiền" class="portal-orders__num"><fmt:formatNumber value="${invoice.total}" maxFractionDigits="0"/> ₫</td>
                                        <td data-label="Còn nợ" class="portal-orders__num"><strong><fmt:formatNumber value="${invoice.outstanding}" maxFractionDigits="0"/> ₫</strong></td>
                                        <td><a class="portal-orders__link" target="_blank" rel="noopener" href="<c:url value='/portal/invoices/print'><c:param name='id' value='${invoice.id}'/></c:url>">Xem / tải hoá đơn</a></td>
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
