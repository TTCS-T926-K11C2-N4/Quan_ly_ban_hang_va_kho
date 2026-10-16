<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="trips"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Chuyến giao hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuyến <c:out value="${trip.tripNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chuyến <c:out value="${trip.tripNo}"/>
                        <span class="trip-pill trip-pill--${trip.statusTone}" id="trip-status"><c:out value="${trip.statusLabel}"/></span></h1>
                    <p class="inv-header__subtitle"><c:out value="${trip.warehouseName}"/> · <c:out value="${empty trip.regionName ? 'Mọi khu vực' : trip.regionName}"/>
                        · Tài xế <strong><c:out value="${trip.driverName}"/></strong><c:if test="${not empty trip.driverPhone}"> (<c:out value="${trip.driverPhone}"/>)</c:if>
                        · Xe <strong><c:out value="${trip.vehiclePlate}"/></strong></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/trips'/>">← Danh sách chuyến</a>
                    <a class="inv-button" id="trip-print" target="_blank" rel="noopener" href="<c:url value='/trips/print'><c:param name='id' value='${trip.id}'/></c:url>">In danh sách điểm giao</a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="trip-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="trip-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="trip-info-title">
                <h2 class="inv-card__title" id="trip-info-title">Thời gian</h2>
                <dl class="trip-info">
                    <div><dt>Xuất phát dự kiến</dt><dd><c:out value="${empty trip.plannedDepartureText ? '—' : trip.plannedDepartureText}"/></dd></div>
                    <div><dt>Dự kiến về</dt><dd><c:out value="${empty trip.expectedReturnAtText ? '—' : trip.expectedReturnAtText}"/></dd></div>
                    <div><dt>Đã xuất phát</dt><dd><c:out value="${empty trip.departedAtText ? '—' : trip.departedAtText}"/></dd></div>
                    <div><dt>Hoàn thành</dt><dd><c:out value="${empty trip.completedAtText ? '—' : trip.completedAtText}"/></dd></div>
                    <c:if test="${not empty trip.note}"><div class="trip-info__wide"><dt>Ghi chú</dt><dd><c:out value="${trip.note}"/></dd></div></c:if>
                </dl>
            </section>

            <section class="inv-card" aria-labelledby="trip-stops-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="trip-stops-title">Điểm giao</h2>
                    <p class="inv-card__meta" id="trip-progress">Đã có kết quả <strong>${trip.doneCount} / ${trip.stopCount}</strong>, còn <strong>${trip.remainingCount}</strong></p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="trip-stops">
                        <thead>
                            <tr>
                                <th scope="col">Phiếu xuất</th>
                                <th scope="col">Đại lý / điểm giao</th>
                                <th scope="col">Người nhận</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Giá trị</th>
                                <c:if test="${canManage and trip.planned}"><th scope="col"><span class="visually-hidden">Bỏ</span></th></c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="stop" items="${trip.stops}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/delivery-notes/view'><c:param name='id' value='${stop.noteId}'/></c:url>"><c:out value="${stop.deliveryNo}"/></a><span class="inv-table__sub">Đơn <c:out value="${stop.orderNo}"/></span></td>
                                    <td><span class="inv-table__main"><c:out value="${stop.customerName}"/></span><span class="inv-table__sub"><c:out value="${stop.address}"/></span></td>
                                    <td><c:out value="${stop.receiverName}"/><span class="inv-table__sub"><c:out value="${stop.receiverPhone}"/></span></td>
                                    <td><c:out value="${stop.noteStatusLabel}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${stop.value}" maxFractionDigits="0"/> ₫</td>
                                    <c:if test="${canManage and trip.planned}">
                                        <td>
                                            <form action="<c:url value='/trips/remove'/>" method="post">
                                                <input type="hidden" name="id" value="${trip.id}">
                                                <input type="hidden" name="note" value="${stop.noteId}">
                                                <button class="inv-button inv-button--small" type="submit">Bỏ</button>
                                            </form>
                                        </td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty trip.stops}">
                                <tr><td class="inv-table__empty" colspan="6">Chuyến chưa có điểm giao nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <c:if test="${canManage and trip.planned and not empty candidates}">
                <section class="inv-card" aria-labelledby="trip-add-title">
                    <h2 class="inv-card__title" id="trip-add-title">Thêm điểm giao</h2>
                    <form action="<c:url value='/trips/add'/>" method="post" id="trip-add-form">
                        <input type="hidden" name="id" value="${trip.id}">
                        <ul class="trip-candidates">
                            <c:forEach var="stop" items="${candidates}">
                                <li><label><input type="checkbox" name="note_id" value="${stop.noteId}"> <strong><c:out value="${stop.deliveryNo}"/></strong> · <c:out value="${stop.customerName}"/> · <c:out value="${stop.address}"/></label></li>
                            </c:forEach>
                        </ul>
                        <button class="inv-button" type="submit">Thêm vào chuyến</button>
                    </form>
                </section>
            </c:if>

            <c:if test="${canManage}">
                <div class="trip-actions">
                    <c:if test="${trip.planned}">
                        <form action="<c:url value='/trips/cancel'/>" method="post">
                            <input type="hidden" name="id" value="${trip.id}">
                            <button class="inv-button" type="submit" id="trip-cancel">Huỷ chuyến</button>
                        </form>
                        <form action="<c:url value='/trips/depart'/>" method="post">
                            <input type="hidden" name="id" value="${trip.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="trip-depart">Xuất phát</button>
                        </form>
                    </c:if>
                    <c:if test="${trip.running}">
                        <form action="<c:url value='/trips/complete'/>" method="post">
                            <input type="hidden" name="id" value="${trip.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="trip-complete">Hoàn thành chuyến</button>
                        </form>
                    </c:if>
                </div>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
