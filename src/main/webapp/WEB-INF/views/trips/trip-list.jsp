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
    <title>Chuyến giao hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chuyến giao hàng</h1>
                    <p class="inv-header__subtitle">Gom nhiều phiếu xuất vào một chuyến theo khu vực, gán tài xế và phương tiện để xe chạy một vòng giao nhiều điểm.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="trip-monitor-link" href="<c:url value='/trips/monitor'/>">Theo dõi chuyến đang giao</a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="trip-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="trip-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${canManage}">
                <section class="inv-card" aria-labelledby="trip-new-title">
                    <h2 class="inv-card__title" id="trip-new-title">Lập chuyến giao</h2>
                    <form class="trip-pick" id="trip-pick-form" action="<c:url value='/trips'/>" method="get">
                        <div class="inv-field">
                            <label class="stock-filters__label" for="trip-warehouse">Kho xuất hàng</label>
                            <select class="inv-control" id="trip-warehouse" name="warehouse" required>
                                <option value="">Chọn kho</option>
                                <c:forEach var="warehouse" items="${warehouses}">
                                    <option value="${warehouse.id}"${warehouse.id == warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="stock-filters__label" for="trip-region">Khu vực</label>
                            <select class="inv-control" id="trip-region" name="region">
                                <option value="">Tất cả khu vực</option>
                                <c:forEach var="region" items="${regions}">
                                    <option value="${region.id}"${region.id == regionId ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <button class="inv-button" type="submit" id="trip-load">Xem phiếu chờ giao</button>
                    </form>

                    <c:if test="${not empty warehouseId}">
                        <form id="trip-form" action="<c:url value='/trips/new'/>" method="post">
                            <input type="hidden" name="warehouse" value="${warehouseId}">
                            <input type="hidden" name="region" value="${regionId}">
                            <div class="inv-table-scroll">
                                <table class="inv-table" id="trip-candidates">
                                    <thead>
                                        <tr>
                                            <th scope="col"><span class="visually-hidden">Chọn</span></th>
                                            <th scope="col">Phiếu xuất</th>
                                            <th scope="col">Đại lý / điểm giao</th>
                                            <th scope="col">Người nhận</th>
                                            <th scope="col" class="is-number">Giá trị</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="stop" items="${candidates}">
                                            <tr>
                                                <td><input type="checkbox" name="note_id" value="${stop.noteId}" aria-label="Chọn phiếu ${stop.deliveryNo}"></td>
                                                <td><span class="inv-table__main"><c:out value="${stop.deliveryNo}"/></span><span class="inv-table__sub">Đơn <c:out value="${stop.orderNo}"/></span></td>
                                                <td><span class="inv-table__main"><c:out value="${stop.customerName}"/></span><span class="inv-table__sub"><c:out value="${stop.address}"/></span></td>
                                                <td><c:out value="${stop.receiverName}"/> · <c:out value="${stop.receiverPhone}"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${stop.value}" maxFractionDigits="0"/> ₫</td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty candidates}">
                                            <tr><td class="inv-table__empty" colspan="5">Không có phiếu xuất nào đang chờ giao ở kho và khu vực này.</td></tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                            <div class="trip-fields">
                                <div class="inv-field">
                                    <label class="stock-filters__label" for="trip-driver">Tài xế<span class="trip-required">*</span></label>
                                    <input class="inv-control" type="text" id="trip-driver" name="driverName" maxlength="150" required>
                                </div>
                                <div class="inv-field">
                                    <label class="stock-filters__label" for="trip-phone">Điện thoại tài xế</label>
                                    <input class="inv-control" type="text" id="trip-phone" name="driverPhone" maxlength="20" inputmode="tel">
                                </div>
                                <div class="inv-field">
                                    <label class="stock-filters__label" for="trip-plate">Biển số xe<span class="trip-required">*</span></label>
                                    <input class="inv-control" type="text" id="trip-plate" name="vehiclePlate" maxlength="20" required>
                                </div>
                                <div class="inv-field">
                                    <label class="stock-filters__label" for="trip-departure">Giờ xuất phát dự kiến</label>
                                    <input class="inv-control" type="datetime-local" id="trip-departure" name="plannedDeparture">
                                </div>
                                <div class="inv-field">
                                    <label class="stock-filters__label" for="trip-return">Giờ dự kiến về</label>
                                    <input class="inv-control" type="datetime-local" id="trip-return" name="expectedReturn">
                                </div>
                                <div class="inv-field trip-fields__note">
                                    <label class="stock-filters__label" for="trip-note">Ghi chú</label>
                                    <input class="inv-control" type="text" id="trip-note" name="note" maxlength="1000">
                                </div>
                            </div>
                            <div class="trip-actions">
                                <button class="inv-button inv-button--primary" type="submit" id="trip-create">Lập chuyến giao</button>
                            </div>
                        </form>
                    </c:if>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="trip-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="trip-table-title">Các chuyến giao</h2>
                    <p class="inv-card__meta"><strong>${trips.size()}</strong> chuyến</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="trip-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã chuyến</th>
                                <th scope="col">Kho / khu vực</th>
                                <th scope="col">Tài xế / xe</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Điểm giao</th>
                                <th scope="col">Xuất phát</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="trip" items="${trips}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/trips/view'><c:param name='id' value='${trip.id}'/></c:url>"><c:out value="${trip.tripNo}"/></a></td>
                                    <td><c:out value="${trip.warehouseName}"/><span class="inv-table__sub"><c:out value="${empty trip.regionName ? 'Mọi khu vực' : trip.regionName}"/></span></td>
                                    <td><c:out value="${trip.driverName}"/><span class="inv-table__sub"><c:out value="${trip.vehiclePlate}"/></span></td>
                                    <td><span class="trip-pill trip-pill--${trip.statusTone}"><c:out value="${trip.statusLabel}"/></span></td>
                                    <td class="is-number">${trip.doneCount} / ${trip.stopCount}</td>
                                    <td><c:out value="${empty trip.departedAtText ? (empty trip.plannedDepartureText ? '—' : trip.plannedDepartureText) : trip.departedAtText}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty trips}">
                                <tr><td class="inv-table__empty" colspan="6">Chưa có chuyến giao nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
