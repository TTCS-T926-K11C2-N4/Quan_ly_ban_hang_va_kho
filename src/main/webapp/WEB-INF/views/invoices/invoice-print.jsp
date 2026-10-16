<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hoá đơn <c:out value="${invoice.invoiceNo}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <c:choose>
            <c:when test="${not empty backUrl}"><a class="print-toolbar__button" href="<c:url value='${backUrl}'/>">← Quay lại công nợ</a></c:when>
            <c:otherwise><a class="print-toolbar__button" href="<c:url value='/invoices/view'><c:param name='id' value='${invoice.id}'/></c:url>">← Quay lại hoá đơn</a></c:otherwise>
        </c:choose>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
        <p class="print-toolbar__hint">Trong hộp in chọn "Lưu dạng PDF" để xuất file.</p>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Hoá đơn bán hàng</h1>
                <p class="print-head__meta">Ngày lập: <strong><c:out value="${invoice.issueDateText}"/></strong> · Hạn thanh toán: <strong><c:out value="${invoice.dueDateText}"/></strong></p>
            </div>
            <div class="print-code">
                <div class="print-code__bars">${barcode}</div>
                <p class="print-code__no"><c:out value="${invoice.invoiceNo}"/></p>
            </div>
        </header>

        <section class="print-grid" aria-label="Thông tin đại lý">
            <div class="print-box">
                <h2 class="print-box__title">Đại lý</h2>
                <p class="print-box__name"><c:out value="${invoice.customerName}"/></p>
                <dl class="print-info">
                    <div><dt>Mã đại lý</dt><dd><c:out value="${invoice.customerCode}"/></dd></div>
                    <div><dt>Mã số thuế</dt><dd><c:out value="${empty invoice.customerTaxCode ? '—' : invoice.customerTaxCode}"/></dd></div>
                    <div><dt>Địa chỉ</dt><dd><c:out value="${empty invoice.customerAddress ? '—' : invoice.customerAddress}"/></dd></div>
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
                <c:forEach var="item" items="${invoice.items}">
                    <tr>
                        <td class="print-items__index">${item.lineNo}</td>
                        <td><c:out value="${item.sku}"/></td>
                        <td><c:out value="${item.productName}"/></td>
                        <td><c:out value="${item.unitName}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${item.qty}" maxFractionDigits="3"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/></td>
                        <td class="print-items__num"><c:choose><c:when test="${item.discount.signum() == 0}">—</c:when><c:otherwise><fmt:formatNumber value="${item.discount}" maxFractionDigits="0"/></c:otherwise></c:choose></td>
                        <td class="print-items__num"><fmt:formatNumber value="${item.lineTotal}" maxFractionDigits="0"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <dl class="print-totals">
            <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${invoice.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
            <div><dt>Chiết khấu</dt><dd>${invoice.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${invoice.discount}" maxFractionDigits="0"/> ₫</dd></div>
            <div class="print-totals__grand"><dt>Tổng hoá đơn</dt><dd><fmt:formatNumber value="${invoice.total}" maxFractionDigits="0"/> ₫</dd></div>
        </dl>

        <c:if test="${not empty invoice.note}"><p class="print-note"><strong>Ghi chú:</strong> <c:out value="${invoice.note}"/></p></c:if>

        <footer class="print-sign">
            <div><p>Người lập hoá đơn</p><span><c:out value="${invoice.issuedByName}"/></span></div>
            <div><p>Đại lý xác nhận</p><span>(Ký, ghi rõ họ tên)</span></div>
        </footer>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
