<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuyến giao <c:out value="${trip.tripNo}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <a class="print-toolbar__button" href="<c:url value='/trips/view'><c:param name='id' value='${trip.id}'/></c:url>">← Quay lại chuyến</a>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Danh sách điểm giao</h1>
                <p class="print-head__meta">Chuyến <strong><c:out value="${trip.tripNo}"/></strong> · <c:out value="${trip.warehouseName}"/></p>
            </div>
            <div class="print-code">
                <p class="print-head__meta">Tài xế: <strong><c:out value="${trip.driverName}"/></strong><c:if test="${not empty trip.driverPhone}"> · <c:out value="${trip.driverPhone}"/></c:if></p>
                <p class="print-head__meta">Xe: <strong><c:out value="${trip.vehiclePlate}"/></strong></p>
                <p class="print-head__meta">Xuất phát: <c:out value="${empty trip.plannedDepartureText ? '—' : trip.plannedDepartureText}"/></p>
            </div>
        </header>

        <table class="print-items" style="margin-top: 14px">
            <thead>
                <tr>
                    <th scope="col" class="print-items__index">STT</th>
                    <th scope="col">Đại lý</th>
                    <th scope="col">Địa chỉ giao</th>
                    <th scope="col">Người nhận</th>
                    <th scope="col">Điện thoại</th>
                    <th scope="col">Phiếu xuất</th>
                    <th scope="col" class="print-items__num">Giá trị</th>
                    <th scope="col">Ký nhận</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="stop" items="${trip.stops}" varStatus="loop">
                    <tr>
                        <td class="print-items__index">${loop.index + 1}</td>
                        <td><c:out value="${stop.customerName}"/></td>
                        <td><c:out value="${stop.address}"/><c:if test="${not empty stop.routeNote}"><br><em><c:out value="${stop.routeNote}"/></em></c:if></td>
                        <td><c:out value="${stop.receiverName}"/></td>
                        <td><c:out value="${stop.receiverPhone}"/></td>
                        <td><c:out value="${stop.deliveryNo}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${stop.value}" maxFractionDigits="0"/></td>
                        <td></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <c:if test="${not empty trip.note}"><p class="print-note"><strong>Ghi chú:</strong> <c:out value="${trip.note}"/></p></c:if>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
