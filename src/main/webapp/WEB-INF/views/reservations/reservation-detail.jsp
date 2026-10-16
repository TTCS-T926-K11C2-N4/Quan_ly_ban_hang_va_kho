<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="reservations"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Chi tiết giữ chỗ tồn"/>
<c:set var="breadcrumbParentUrl" value="/stock/reservations"/>
<c:set var="breadcrumbPage" value="${order.orderNo}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Giữ chỗ đơn <c:out value="${order.orderNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/reservations.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Chi tiết đơn đang giữ chỗ</h1>
                    <p class="inv-header__subtitle">Kiểm tra số lượng và trạng thái giữ chỗ trên từng dòng hàng của đơn đã được duyệt.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="reservation-back" href="<c:url value='/stock/reservations'/>">
                        <span class="inv-icon inv-icon--chevron-left" aria-hidden="true"></span>Quay lại danh sách
                    </a>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="order-info-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="order-info-title">Thông tin đơn hàng</h2>
                        <p class="inv-card__subtitle">Mã đơn <c:out value="${order.orderNo}"/> · <c:out value="${order.customerName}"/></p>
                    </div>
                    <span class="inv-pill inv-pill--dot ${order.orderLive ? 'inv-pill--success' : 'inv-pill--muted'}">${order.orderStatusLabel}</span>
                </div>
                <dl class="reservation-info">
                    <div>
                        <dt>Mã đơn hàng</dt>
                        <dd class="reservation-info__value"><c:out value="${order.orderNo}"/></dd>
                        <dd class="reservation-info__note">Đơn bán hàng</dd>
                    </div>
                    <div>
                        <dt>Đại lý</dt>
                        <dd class="reservation-info__value"><c:out value="${order.customerName}"/></dd>
                        <dd class="reservation-info__note">Mã đại lý: <c:out value="${order.customerCode}"/></dd>
                    </div>
                    <div>
                        <dt>Ngày duyệt</dt>
                        <dd class="reservation-info__value">${order.approvedAtText}</dd>
                        <dd class="reservation-info__note">Ngày ghi nhận duyệt đơn</dd>
                    </div>
                    <div>
                        <dt>Tình trạng giữ chỗ</dt>
                        <dd class="reservation-info__value"><span class="inv-pill inv-pill--dot inv-pill--${order.state.tone}" id="reservation-state">${order.state.label}</span></dd>
                        <dd class="reservation-info__note">Đã ghi nhận cho từng dòng hàng</dd>
                    </div>
                </dl>
            </section>

            <section class="inv-stats" aria-label="Tổng quan dòng hàng">
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Số dòng hàng</p>
                    <span class="inv-stat__icon tone-blue"><span class="inv-icon inv-icon--file-text" aria-hidden="true"></span></span>
                    <p class="inv-stat__value">${fn:length(order.lines)}</p>
                    <p class="inv-stat__note">Dòng hàng trong đơn <c:out value="${order.orderNo}"/></p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Tổng lượng giữ chỗ</p>
                    <span class="inv-stat__icon tone-green"><span class="inv-icon inv-icon--package" aria-hidden="true"></span></span>
                    <p class="inv-stat__value"><fmt:formatNumber value="${order.reservedTotal}" maxFractionDigits="3"/></p>
                    <p class="inv-stat__note">Tổng đơn vị cơ sở của đơn</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Dòng hàng đã giữ chỗ</p>
                    <span class="inv-stat__icon tone-green"><span class="inv-icon inv-icon--circle-check" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="reservation-held-lines">${order.heldLineCount}/${fn:length(order.lines)}</p>
                    <p class="inv-stat__note">Các dòng hàng có trạng thái xác nhận</p>
                </article>
            </section>

            <div class="inv-note">
                <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                <p><strong>Quy tắc giữ chỗ:</strong> Mỗi dòng hàng của đơn được giữ chỗ tồn tại kho phục vụ của đơn và
                    chuyển sang “Đã giữ chỗ”; người khác không đặt được phần tồn đó nữa. Huỷ đơn, hoặc đơn đã duyệt quá ${holdHours} giờ
                    chưa xuất kho thì tồn được nhả tự động. Số lượng tính theo đơn vị cơ sở.</p>
            </div>

            <section class="inv-card" aria-labelledby="reservation-lines-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="reservation-lines-title">Chi tiết giữ chỗ theo dòng hàng</h2>
                        <p class="inv-card__subtitle">Trạng thái giữ chỗ của từng sản phẩm trong đơn <c:out value="${order.orderNo}"/>.</p>
                    </div>
                    <span class="inv-pill inv-pill--success">${fn:length(order.lines)} dòng hàng</span>
                </div>
                <div class="inv-table-scroll reservation-table-wrap">
                    <table class="inv-table reservation-lines" id="reservation-lines">
                        <thead>
                            <tr>
                                <th scope="col">Mã SKU</th>
                                <th scope="col">Tên sản phẩm</th>
                                <th scope="col">Kho</th>
                                <th scope="col" class="is-number">Số lượng đặt</th>
                                <th scope="col" class="is-number">Số lượng giữ chỗ</th>
                                <th scope="col">Trạng thái giữ chỗ</th>
                                <th scope="col">Ngày duyệt</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="line" items="${order.lines}">
                                <tr>
                                    <td class="reservation-lines__sku"><c:out value="${line.sku}"/></td>
                                    <td>
                                        <span class="inv-table__main"><c:out value="${line.productName}"/></span>
                                        <span class="inv-table__sub">Đơn vị: <c:out value="${fn:toLowerCase(line.baseUnitName)}"/></span>
                                    </td>
                                    <td>
                                        <span class="reservation-lines__warehouse"><c:out value="${line.warehouseName}"/></span>
                                        <span class="inv-table__sub"><c:out value="${line.warehouseCode}"/></span>
                                    </td>
                                    <td class="is-number">
                                        <span class="reservation-lines__qty"><fmt:formatNumber value="${line.orderedQty}" maxFractionDigits="3"/></span>
                                        <span class="inv-table__sub"><c:out value="${fn:toLowerCase(line.baseUnitName)}"/></span>
                                    </td>
                                    <td class="is-number">
                                        <span class="reservation-lines__qty"><fmt:formatNumber value="${line.reservedQty}" maxFractionDigits="3"/></span>
                                        <span class="inv-table__sub"><c:out value="${fn:toLowerCase(line.baseUnitName)}"/></span>
                                    </td>
                                    <td>
                                        <span class="inv-pill inv-pill--dot inv-pill--${line.state.tone}">${line.state.label}</span>
                                        <c:if test="${line.state == 'HELD' and not empty line.expiresAtText}">
                                            <span class="inv-table__sub">Giữ đến ${line.expiresAtText}</span>
                                        </c:if>
                                    </td>
                                    <td>${order.approvedAtText}</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="inv-footer">
                    <p class="inv-footer__info">Hiển thị <strong>${fn:length(order.lines)}</strong> dòng hàng · Tổng số lượng giữ chỗ
                        <strong><fmt:formatNumber value="${order.reservedTotal}" maxFractionDigits="3"/></strong></p>
                    <span class="inv-pill inv-pill--dot inv-pill--${order.state.tone}">${order.state.label}</span>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
