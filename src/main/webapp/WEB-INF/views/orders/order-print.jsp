<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu đơn hàng <c:out value="${order.orderNo}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <a class="print-toolbar__button" href="<c:url value='/orders/view'><c:param name='id' value='${order.id}'/></c:url>">← Quay lại đơn</a>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
        <p class="print-toolbar__hint">Trong hộp in chọn "Lưu dạng PDF" để xuất file.</p>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Phiếu đơn hàng</h1>
                <p class="print-head__meta">Trạng thái: <strong><c:out value="${order.statusLabel}"/></strong></p>
            </div>
            <div class="print-code">
                <div class="print-code__bars">${barcode}</div>
                <p class="print-code__no"><c:out value="${order.orderNo}"/></p>
            </div>
        </header>

        <section class="print-grid" aria-label="Thông tin đơn hàng">
            <div class="print-box">
                <h2 class="print-box__title">Đại lý</h2>
                <p class="print-box__name"><c:out value="${order.customerName}"/></p>
                <dl class="print-info">
                    <div><dt>Mã đại lý</dt><dd><c:out value="${order.customerCode}"/></dd></div>
                    <div><dt>Mã số thuế</dt><dd><c:out value="${empty order.customerTaxCode ? '—' : order.customerTaxCode}"/></dd></div>
                    <div><dt>Điện thoại</dt><dd><c:out value="${empty order.customerPhone ? '—' : order.customerPhone}"/></dd></div>
                    <div><dt>Địa chỉ</dt><dd><c:out value="${empty order.customerAddress ? '—' : order.customerAddress}"/></dd></div>
                </dl>
            </div>
            <div class="print-box">
                <h2 class="print-box__title">Điểm giao hàng</h2>
                <p class="print-box__name"><c:out value="${empty order.addressLabel ? 'Điểm giao' : order.addressLabel}"/></p>
                <dl class="print-info">
                    <div><dt>Địa chỉ giao</dt><dd><c:out value="${order.deliveryAddress}"/></dd></div>
                    <div><dt>Người nhận</dt><dd><c:out value="${order.receiverName}"/></dd></div>
                    <div><dt>Điện thoại</dt><dd><c:out value="${order.receiverPhone}"/></dd></div>
                    <c:if test="${not empty order.routeNote}">
                        <div><dt>Ghi chú đường đi</dt><dd><c:out value="${order.routeNote}"/></dd></div>
                    </c:if>
                </dl>
            </div>
            <div class="print-box">
                <h2 class="print-box__title">Đơn hàng</h2>
                <dl class="print-info">
                    <div><dt>Ngày tạo</dt><dd><c:out value="${order.createdAtText}"/></dd></div>
                    <div><dt>Ngày giao mong muốn</dt><dd><c:out value="${empty order.requestedDateText ? '—' : order.requestedDateText}"/></dd></div>
                    <div><dt>Nhân viên phụ trách</dt><dd><c:out value="${empty order.salesRepName ? 'Chưa phân công' : order.salesRepName}"/></dd></div>
                    <div><dt>Kho phục vụ</dt><dd><c:out value="${order.warehouseName}"/></dd></div>
                </dl>
            </div>
        </section>

        <table class="print-items">
            <thead>
                <tr>
                    <th scope="col" class="print-items__index">STT</th>
                    <th scope="col">Mã hàng</th>
                    <th scope="col">Tên hàng</th>
                    <th scope="col">ĐVT</th>
                    <th scope="col" class="print-items__num">Số lượng</th>
                    <th scope="col" class="print-items__num">Đơn giá</th>
                    <th scope="col" class="print-items__num">Chiết khấu</th>
                    <th scope="col" class="print-items__num">Thành tiền</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${order.items}">
                    <tr>
                        <td class="print-items__index">${item.lineNo}</td>
                        <td><c:out value="${item.sku}"/></td>
                        <td><c:out value="${item.productName}"/></td>
                        <td><c:out value="${item.unitName}"/></td>
                        <td class="print-items__num"><c:out value="${item.qtyText}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/></td>
                        <td class="print-items__num"><c:choose><c:when test="${item.discount.signum() == 0}">—</c:when><c:otherwise><fmt:formatNumber value="${item.discount}" maxFractionDigits="0"/></c:otherwise></c:choose></td>
                        <td class="print-items__num"><fmt:formatNumber value="${item.lineTotal}" maxFractionDigits="0"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <dl class="print-totals">
            <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${order.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
            <div><dt>Chiết khấu</dt><dd>${order.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${order.discount}" maxFractionDigits="0"/> ₫</dd></div>
            <div class="print-totals__grand"><dt>Tổng phải thu</dt><dd><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</dd></div>
        </dl>

        <c:if test="${not empty order.note}">
            <p class="print-note"><strong>Ghi chú:</strong> <c:out value="${order.note}"/></p>
        </c:if>

        <footer class="print-sign">
            <div><p>Đại lý xác nhận</p><span>(Ký, ghi rõ họ tên)</span></div>
            <div><p>Nhân viên kinh doanh</p><span>(Ký, ghi rõ họ tên)</span></div>
        </footer>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
