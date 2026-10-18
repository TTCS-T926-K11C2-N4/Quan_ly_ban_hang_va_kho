<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="portal-orders"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Chi tiết đơn hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn <c:out value="${order.orderNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <div>
                    <h1 class="page-header__title">Đơn <c:out value="${order.orderNo}"/>
                        <span class="order-status order-status--${order.statusTone}" id="portal-order-status">${order.statusLabel}</span></h1>
                    <p class="page-header__subtitle">Đặt ngày <c:out value="${order.createdAtText}"/>
                        · Giao dự kiến <c:out value="${empty order.requestedDateText ? '—' : order.requestedDateText}"/></p>
                </div>
                <a class="sales-button" href="<c:url value='/portal/orders'/>">← Đơn hàng của tôi</a>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="portal-order-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="sales-flash sales-flash--error" id="portal-order-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>
            <c:if test="${order.shortDelivery}">
                <div class="sales-flash sales-flash--warning" id="portal-short-delivery" role="status">
                    Đơn này giao thiếu một số mặt hàng, xem cột "Thực giao" bên dưới. Liên hệ nhân viên phụ trách nếu cần bổ sung.
                </div>
            </c:if>

            <%-- S6-09: các mốc giao hàng để đại lý bố trí người nhận đúng lúc xe tới --%>
            <section class="sales-card portal-orders__card portal-timeline" id="portal-timeline" aria-labelledby="portal-timeline-title">
                <h2 class="portal-orders__title" id="portal-timeline-title">Tiến trình giao hàng</h2>
                <ol class="portal-steps">
                    <c:forEach var="step" items="${order.timeline}" varStatus="loop">
                        <li class="portal-step${step.reached ? ' portal-step--done' : ''}${loop.index == order.currentStep ? ' portal-step--current' : ''}"${loop.index == order.currentStep ? ' aria-current="step"' : ''}>
                            <span class="portal-step__dot" aria-hidden="true">${step.reached ? '✓' : loop.index + 1}</span>
                            <span class="portal-step__label"><c:out value="${step.label}"/></span>
                            <span class="portal-step__time"><c:out value="${empty step.atText ? 'Chưa tới' : step.atText}"/></span>
                        </li>
                    </c:forEach>
                </ol>
            </section>

            <section class="sales-card portal-orders__card" aria-labelledby="portal-items-title">
                <h2 class="portal-orders__title" id="portal-items-title">Hàng hoá (${order.items.size()})</h2>
                <p class="portal-orders__address"><strong>Giao tới:</strong> <c:out value="${order.deliveryAddress}"/></p>
                <c:set var="canReorder" value="${currentUser.can('ORDER_MANAGE') and order.status != 'CANCELLED' and order.status != 'REJECTED'}"/>
                <form method="post" action="<c:url value='/portal/orders/reorder'/>" id="portal-reorder-form">
                <input type="hidden" name="id" value="${order.id}">
                <table class="portal-orders__table" id="portal-order-items">
                    <thead>
                        <tr>
                            <c:if test="${canReorder}"><th scope="col"><span class="visually-hidden">Chọn đặt lại</span></th></c:if>
                            <th scope="col">STT</th>
                            <th scope="col">Sản phẩm</th>
                            <th scope="col" class="portal-orders__num">Đặt</th>
                            <th scope="col" class="portal-orders__num">Thực giao</th>
                            <th scope="col" class="portal-orders__num">Đơn giá</th>
                            <th scope="col" class="portal-orders__num">Chiết khấu</th>
                            <th scope="col" class="portal-orders__num">Thành tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${order.items}">
                            <tr class="${item.shortDelivered ? 'portal-orders__row--short' : ''}">
                                <c:if test="${canReorder}">
                                    <td data-label="Chọn đặt lại"><input type="checkbox" name="line" value="${item.lineNo}" aria-label="Chọn dòng ${item.lineNo} để đặt lại"></td>
                                </c:if>
                                <td data-label="STT">${item.lineNo}</td>
                                <td data-label="Sản phẩm"><c:out value="${item.productName}"/><span class="portal-orders__sku"><c:out value="${item.sku}"/></span></td>
                                <td data-label="Đặt" class="portal-orders__num"><fmt:formatNumber value="${item.qty}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                <td data-label="Thực giao" class="portal-orders__num">
                                    <c:choose>
                                        <c:when test="${not item.shippedKnown}">—</c:when>
                                        <c:otherwise><fmt:formatNumber value="${item.qtyActual}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/><c:if test="${item.shortDelivered}"> <strong class="portal-orders__short">(thiếu)</strong></c:if><c:if test="${not item.deliveredKnown}"><span class="portal-orders__sku">đã xuất, chưa giao xong</span></c:if></c:otherwise>
                                    </c:choose>
                                </td>
                                <td data-label="Đơn giá" class="portal-orders__num"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/> ₫</td>
                                <td data-label="Chiết khấu" class="portal-orders__num"><c:choose><c:when test="${item.discount.signum() == 0}">—</c:when><c:otherwise>−<fmt:formatNumber value="${item.discount}" maxFractionDigits="0"/> ₫</c:otherwise></c:choose></td>
                                <td data-label="Thành tiền" class="portal-orders__num"><fmt:formatNumber value="${item.lineTotal}" maxFractionDigits="0"/> ₫</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <c:if test="${canReorder}">
                    <div class="portal-orders__reorder">
                        <button class="sales-button" type="submit" id="portal-reorder-selected">Đặt lại các dòng đã chọn</button>
                        <button class="sales-button sales-button--primary" type="submit" name="all" value="1" id="portal-reorder-all">Đặt lại cả đơn</button>
                    </div>
                </c:if>
                </form>
                <dl class="order-totals">
                    <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${order.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Chiết khấu</dt><dd>${order.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${order.discount}" maxFractionDigits="0"/> ₫</dd></div>
                    <div class="order-totals__grand"><dt>Tổng phải thu</dt><dd><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
                <c:if test="${not empty order.note}">
                    <p class="portal-orders__note"><strong>Ghi chú:</strong> <c:out value="${order.note}"/></p>
                </c:if>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
