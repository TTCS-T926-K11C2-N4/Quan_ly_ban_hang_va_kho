<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="trips"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Theo dõi chuyến đang giao"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Theo dõi chuyến đang giao | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/trips.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Theo dõi chuyến đang giao</h1>
                    <p class="inv-header__subtitle">Hàng đã xuất kho nhưng chưa giao xong: số điểm đã giao và còn lại, tiền hàng đang trên đường, chuyến quá giờ dự kiến.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/trips'/>">← Các chuyến giao</a>
                </div>
            </header>

            <section class="inv-stats" aria-label="Tổng quan hàng đang trên đường">
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Chuyến đang giao</p>
                    <p class="inv-stat__value" id="monitor-trips">${trips.size()}</p>
                    <p class="inv-stat__note">${remainingStops} điểm còn phải giao</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Tiền hàng đang trên đường</p>
                    <p class="inv-stat__value" id="monitor-value"><fmt:formatNumber value="${onRoadValue}" maxFractionDigits="0"/> ₫</p>
                    <p class="inv-stat__note">Các điểm chưa có kết quả giao</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Chuyến quá giờ dự kiến</p>
                    <p class="inv-stat__value${overdueCount > 0 ? ' trip-overdue' : ''}" id="monitor-overdue">${overdueCount}</p>
                    <p class="inv-stat__note">Hàng chưa giao được: <fmt:formatNumber value="${failedValue}" maxFractionDigits="0"/> ₫</p>
                </article>
            </section>

            <c:if test="${empty trips}">
                <p class="inv-card trip-monitor-empty" id="monitor-empty">Hiện không có chuyến nào đang giao.</p>
            </c:if>

            <c:forEach var="trip" items="${trips}">
                <section class="inv-card trip-monitor${trip.overdue ? ' trip-monitor--overdue' : ''}" aria-label="Chuyến ${trip.tripNo}">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title">
                                <a href="<c:url value='/trips/view'><c:param name='id' value='${trip.id}'/></c:url>"><c:out value="${trip.tripNo}"/></a>
                                <c:if test="${trip.overdue}"><span class="trip-pill trip-pill--danger">Quá giờ dự kiến</span></c:if>
                            </h2>
                            <p class="inv-card__subtitle"><c:out value="${trip.warehouseName}"/> · Tài xế <c:out value="${trip.driverName}"/> · Xe <c:out value="${trip.vehiclePlate}"/>
                                · Xuất phát <c:out value="${trip.departedAtText}"/> · Dự kiến về <c:out value="${empty trip.expectedReturnAtText ? '—' : trip.expectedReturnAtText}"/></p>
                        </div>
                        <p class="inv-card__meta">Đã giao <strong>${trip.doneCount} / ${trip.stopCount}</strong> · còn <strong>${trip.remainingCount}</strong>
                            · trên đường <strong><fmt:formatNumber value="${trip.onRoadValue}" maxFractionDigits="0"/> ₫</strong></p>
                    </div>
                    <progress class="trip-progress" max="${trip.stopCount}" value="${trip.doneCount}" aria-label="Tiến độ chuyến ${trip.tripNo}"></progress>
                    <c:if test="${not empty trip.failedStops}">
                        <p class="trip-failed">
                            <strong>Chưa giao được:</strong>
                            <c:forEach var="stop" items="${trip.failedStops}" varStatus="loop"><c:out value="${stop.customerName}"/> (<c:out value="${stop.deliveryNo}"/>)<c:if test="${not loop.last}">, </c:if></c:forEach>
                            · hàng vẫn tính vào chuyến cho tới khi chuyến hoàn thành.
                        </p>
                    </c:if>
                </section>
            </c:forEach>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
