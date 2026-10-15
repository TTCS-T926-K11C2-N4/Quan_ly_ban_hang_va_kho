<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="deliveries"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Xác nhận giao hàng"/>
<c:set var="breadcrumbParentUrl" value="/deliveries?date=${date}"/>
<c:set var="breadcrumbPage" value="Chi tiết điểm giao"/>
<c:url var="backUrl" value="/deliveries"><c:param name="date" value="${date}"/></c:url>
<c:set var="canAct" value="${canManage and point.pendingCount > 0}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Điểm giao <c:out value="${point.customerName}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/deliveries.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page delivery-page">
            <header class="inv-header">
                <div>
                    <a class="delivery-back" id="point-back" href="${fn:escapeXml(backUrl)}">
                        <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Xác nhận giao hàng
                    </a>
                    <h1 class="inv-header__title">Chi tiết điểm giao</h1>
                    <p class="inv-header__subtitle">Ngày giao ${dateText} • ${point.orderCount} đơn tại điểm giao</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="point-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="point-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <div class="delivery-detail">
                <section class="inv-card delivery-info" aria-label="Thông tin điểm giao">
                    <div class="delivery-info__top">
                        <span class="delivery-label">Điểm giao</span>
                        <span class="inv-pill delivery-pill delivery-pill--${point.tone}" id="point-status">${point.statusLabel}</span>
                    </div>
                    <h2 class="delivery-info__name"><c:out value="${point.customerName}"/>
                        <c:if test="${not empty point.label}"><span class="delivery-point__label"> · <c:out value="${point.label}"/></span></c:if></h2>
                    <p class="delivery-point__address"><span class="inv-icon inv-icon--map-pin" aria-hidden="true"></span><c:out value="${point.address}"/></p>
                    <hr class="delivery-divider">
                    <div class="delivery-contact">
                        <div>
                            <p class="delivery-label">Người nhận</p>
                            <p class="delivery-contact__name"><c:out value="${empty point.receiverName ? 'Chưa khai báo' : point.receiverName}"/></p>
                            <p class="delivery-contact__phone"><c:out value="${empty point.receiverPhone ? 'Chưa có số điện thoại' : point.receiverPhone}"/></p>
                        </div>
                        <c:if test="${not empty point.phoneLink}">
                            <a class="inv-button inv-button--outline delivery-call" id="point-call" href="tel:${point.phoneLink}">
                                <span class="inv-icon delivery-icon--phone" aria-hidden="true"></span>Gọi
                            </a>
                        </c:if>
                    </div>
                    <div class="delivery-route">
                        <span class="inv-icon delivery-icon--navigation" aria-hidden="true"></span>
                        <div>
                            <p class="delivery-label">Ghi chú đường đi</p>
                            <p class="delivery-route__text"><c:out value="${empty point.routeNote ? 'Không có chỉ dẫn thêm.' : point.routeNote}"/></p>
                        </div>
                    </div>
                </section>

                <section class="inv-card delivery-orders" aria-labelledby="point-orders-title">
                    <div class="delivery-orders__head">
                        <h2 class="delivery-card-title" id="point-orders-title">Danh sách đơn</h2>
                        <dl class="delivery-orders__totals">
                            <div><dt>Tổng đơn</dt><dd>${point.orderCount}</dd></div>
                            <div><dt>Tổng thu hộ</dt><dd><fmt:formatNumber value="${point.totalValue}" maxFractionDigits="0"/> ₫</dd></div>
                        </dl>
                    </div>
                    <ul class="delivery-stops" id="point-stops">
                        <c:forEach var="stop" items="${point.stops}">
                            <li class="delivery-stop${stop.pending ? '' : ' is-done'}">
                                <div class="delivery-stop__head">
                                    <c:if test="${canAct and stop.pending}">
                                        <input class="inv-checkbox delivery-stop__check" type="checkbox" name="note" value="${stop.noteId}" form="point-action-form"
                                               id="note-${stop.noteId}" checked data-scope-check>
                                    </c:if>
                                    <label class="delivery-stop__title" for="note-${stop.noteId}">
                                        <span class="delivery-stop__order"><c:out value="${stop.orderNo}"/></span>
                                        <span class="delivery-stop__note">Phiếu xuất <c:out value="${stop.deliveryNo}"/></span>
                                    </label>
                                    <span class="inv-pill delivery-pill delivery-pill--${stop.tone}">${stop.statusLabel}</span>
                                </div>
                                <ul class="delivery-stop__items" aria-label="Mặt hàng">
                                    <c:forEach var="item" items="${stop.items}">
                                        <li>
                                            <span class="delivery-stop__item-name"><c:out value="${item.productName}"/></span>
                                            <span class="delivery-stop__item-qty"><fmt:formatNumber value="${item.qty}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></span>
                                        </li>
                                    </c:forEach>
                                </ul>
                                <div class="delivery-stop__foot">
                                    <span>Thu hộ</span>
                                    <strong><fmt:formatNumber value="${stop.value}" maxFractionDigits="0"/> ₫</strong>
                                </div>
                                <c:if test="${stop.delivered}">
                                    <p class="delivery-stop__result delivery-stop__result--ok">Đã giao lúc ${stop.deliveredAtText}, người nhận <c:out value="${stop.recipientName}"/>.</p>
                                    <c:if test="${not empty stop.proofIds}">
                                        <div class="delivery-stop__proofs">
                                            <c:forEach var="proofId" items="${stop.proofIds}">
                                                <c:url var="proofUrl" value="/deliveries/proof"><c:param name="id" value="${proofId}"/></c:url>
                                                <a href="${fn:escapeXml(proofUrl)}" target="_blank" rel="noopener" title="Xem ảnh biên bản">
                                                    <img src="${fn:escapeXml(proofUrl)}" alt="Ảnh biên bản giao hàng ${stop.deliveryNo}" width="64" height="64" loading="lazy">
                                                </a>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                </c:if>
                                <c:if test="${stop.failed}">
                                    <p class="delivery-stop__result delivery-stop__result--failed">Không giao được: <c:out value="${stop.failureReason}"/><c:if test="${not empty stop.deliveryRemark}"> — <c:out value="${stop.deliveryRemark}"/></c:if>. Hàng đã quay về kho.</p>
                                </c:if>
                            </li>
                        </c:forEach>
                    </ul>
                </section>
            </div>

            <c:if test="${canAct}">
                <form class="inv-card delivery-actionbar" id="point-action-form" action="<c:url value='/deliveries/confirm'/>" method="get">
                    <input type="hidden" name="address" value="${point.addressId}">
                    <input type="hidden" name="date" value="${date}">
                    <p class="delivery-actionbar__scope">
                        <span class="delivery-label">Phạm vi hành động</span>
                        <span id="scope-text"><strong data-scope-count>${point.pendingCount}</strong>/${point.pendingCount} đơn chờ giao được chọn</span>
                    </p>
                    <div class="delivery-actionbar__buttons">
                        <button class="inv-button delivery-button delivery-button--fail" type="submit" name="result" value="fail" id="point-fail" data-scope-button>
                            <span class="inv-icon inv-icon--circle-x" aria-hidden="true"></span>Giao không thành công
                        </button>
                        <button class="inv-button delivery-button delivery-button--success" type="submit" name="result" value="success" id="point-success" data-scope-button>
                            <span class="inv-icon inv-icon--circle-check" aria-hidden="true"></span>Giao thành công
                        </button>
                    </div>
                </form>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/deliveries.js'/>"></script>
</body>
</html>
