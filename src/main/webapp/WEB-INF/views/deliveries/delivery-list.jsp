<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="deliveries"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Xác nhận giao hàng"/>
<c:url var="syncUrl" value="/deliveries">
    <c:param name="date" value="${day.date}"/>
    <c:param name="keyword" value="${keyword}"/>
    <c:param name="status" value="${status}"/>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận giao hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Xác nhận giao hàng</h1>
                    <p class="inv-header__subtitle">Phiếu xuất kho ngày ${day.dateText} gom theo điểm giao<c:if test="${day.today}">, kèm phiếu ngày trước chưa giao</c:if>.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--outline" id="delivery-sync" href="${fn:escapeXml(syncUrl)}" title="Tải lại số liệu mới nhất">
                        <span class="inv-icon delivery-icon--refresh" aria-hidden="true"></span>Đồng bộ
                    </a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="delivery-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="delivery-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <form class="inv-card delivery-toolbar" id="delivery-filter-form" action="<c:url value='/deliveries'/>" method="get" role="search">
                <div class="delivery-toolbar__date">
                    <label class="delivery-label" for="delivery-date">Ngày giao</label>
                    <span class="delivery-date">
                        <span class="inv-icon delivery-icon--calendar" aria-hidden="true"></span>
                        <input class="inv-control" type="date" id="delivery-date" name="date" value="${day.date}" data-auto-submit>
                    </span>
                </div>
                <div class="delivery-progress" aria-label="Tiến độ giao">
                    <div class="delivery-progress__row">
                        <span>Tổng đơn: <strong id="delivery-total">${day.totalNotes}</strong></span>
                        <span>Tiến độ <strong id="delivery-handled">${day.handledNotes}</strong>/${day.totalNotes}</span>
                    </div>
                    <div class="delivery-progress__bar" role="progressbar" aria-valuemin="0" aria-valuemax="${day.totalNotes}" aria-valuenow="${day.handledNotes}">
                        <span class="delivery-progress__fill" style="width: ${day.progressPercent}%"></span>
                    </div>
                    <p class="delivery-progress__legend">
                        <span class="delivery-dot delivery-dot--delivered"></span>Đã giao ${day.deliveredNotes}
                        <span class="delivery-dot delivery-dot--failed"></span>Không thành công ${day.failedNotes}
                        <span class="delivery-dot delivery-dot--pending"></span>Chờ giao ${day.pendingNotes}
                    </p>
                </div>
                <div class="delivery-toolbar__search">
                    <label class="visually-hidden" for="delivery-keyword">Tìm điểm giao</label>
                    <span class="inv-search">
                        <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                        <input class="inv-control" type="search" id="delivery-keyword" name="keyword" maxlength="100"
                               value="<c:out value='${keyword}'/>" placeholder="Tìm đại lý, địa chỉ, mã đơn..." autocomplete="off">
                    </span>
                </div>
                <fieldset class="delivery-chips" aria-label="Lọc điểm giao">
                    <label class="delivery-chip">
                        <input type="radio" name="status" value=""${empty status ? ' checked' : ''} data-auto-submit><span>Tất cả</span>
                    </label>
                    <c:forEach var="option" items="${statusLabels}">
                        <label class="delivery-chip">
                            <input type="radio" name="status" value="${option.key}"${option.key == status ? ' checked' : ''} data-auto-submit><span>${option.value}</span>
                        </label>
                    </c:forEach>
                    <button class="visually-hidden" type="submit">Lọc</button>
                </fieldset>
            </form>

            <section aria-labelledby="delivery-points-title">
                <div class="delivery-section-head">
                    <h2 class="delivery-section-head__title" id="delivery-points-title">Danh sách theo điểm giao</h2>
                    <p class="delivery-section-head__count" id="delivery-point-count">${fn:length(day.points)} điểm giao</p>
                </div>
                <c:choose>
                    <c:when test="${empty day.points}">
                        <div class="inv-card delivery-empty" id="delivery-empty">
                            <span class="inv-icon delivery-icon--truck" aria-hidden="true"></span>
                            <p>
                                <c:choose>
                                    <c:when test="${not empty keyword or not empty status}">Không có điểm giao phù hợp bộ lọc.</c:when>
                                    <c:otherwise>Ngày ${day.dateText} chưa có phiếu xuất nào cần giao.</c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <ul class="delivery-points" id="delivery-points">
                            <c:forEach var="point" items="${day.points}">
                                <c:url var="pointUrl" value="/deliveries/point">
                                    <c:param name="address" value="${point.addressId}"/>
                                    <c:param name="date" value="${day.date}"/>
                                </c:url>
                                <li class="inv-card delivery-point">
                                    <div class="delivery-point__head">
                                        <h3 class="delivery-point__name">
                                            <c:out value="${point.customerName}"/>
                                            <c:if test="${not empty point.label}"><span class="delivery-point__label"> · <c:out value="${point.label}"/></span></c:if>
                                        </h3>
                                        <span class="inv-pill delivery-pill delivery-pill--${point.tone}">${point.statusLabel}</span>
                                    </div>
                                    <p class="delivery-point__address">
                                        <span class="inv-icon inv-icon--map-pin" aria-hidden="true"></span><c:out value="${point.address}"/>
                                    </p>
                                    <p class="delivery-point__schedule">
                                        Lịch giao: ${point.scheduledDateText}
                                        <c:if test="${point.pendingCount > 0}"> • còn ${point.pendingCount} đơn chờ giao</c:if>
                                    </p>
                                    <ul class="delivery-point__orders" aria-label="Đơn tại điểm giao">
                                        <c:forEach var="stop" items="${point.stops}">
                                            <li class="delivery-order-chip delivery-order-chip--${stop.tone}" title="${stop.statusLabel}"><c:out value="${stop.orderNo}"/></li>
                                        </c:forEach>
                                    </ul>
                                    <div class="delivery-point__foot">
                                        <span class="delivery-point__count">${point.orderCount} đơn</span>
                                        <a class="delivery-point__link" href="${fn:escapeXml(pointUrl)}">
                                            Xem chi tiết<span class="inv-icon delivery-icon--chevron-right" aria-hidden="true"></span>
                                        </a>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/deliveries.js'/>"></script>
</body>
</html>
