<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu thu <c:out value="${payment.receiptNo}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <a class="print-toolbar__button" href="<c:url value='/payments'/>">← Quay lại thu tiền</a>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
        <p class="print-toolbar__hint">Trong hộp in chọn "Lưu dạng PDF" để xuất file.</p>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Phiếu thu</h1>
                <p class="print-head__meta">Ngày thu: <strong><c:out value="${payment.paymentDateText}"/></strong> · Hình thức: <strong><c:out value="${payment.methodLabel}"/></strong></p>
            </div>
            <div class="print-code">
                <div class="print-code__bars">${barcode}</div>
                <p class="print-code__no"><c:out value="${payment.receiptNo}"/></p>
            </div>
        </header>

        <section class="print-grid" aria-label="Thông tin đại lý">
            <div class="print-box">
                <h2 class="print-box__title">Đại lý nộp tiền</h2>
                <p class="print-box__name"><c:out value="${payment.customerName}"/></p>
                <dl class="print-info">
                    <div><dt>Mã đại lý</dt><dd><c:out value="${payment.customerCode}"/></dd></div>
                    <div><dt>Người thu</dt><dd><c:out value="${payment.collectedByName}"/></dd></div>
                    <div><dt>Ghi nhận lúc</dt><dd><c:out value="${payment.createdAtText}"/></dd></div>
                </dl>
            </div>
        </section>

        <table class="print-items">
            <thead>
                <tr>
                    <th scope="col" class="print-items__index">STT</th>
                    <th scope="col">Đối trừ vào hoá đơn</th>
                    <th scope="col" class="print-items__num">Số tiền</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="allocation" items="${payment.allocations}" varStatus="loop">
                    <tr>
                        <td class="print-items__index">${loop.count}</td>
                        <td><c:out value="${allocation.invoiceNo}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${allocation.amount}" maxFractionDigits="0"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <dl class="print-totals">
            <div class="print-totals__grand"><dt>Số tiền đã thu</dt><dd><fmt:formatNumber value="${payment.amount}" maxFractionDigits="0"/> ₫</dd></div>
        </dl>

        <c:if test="${not empty payment.note}"><p class="print-note"><strong>Ghi chú:</strong> <c:out value="${payment.note}"/></p></c:if>

        <footer class="print-sign">
            <div><p>Người thu tiền</p><span><c:out value="${payment.collectedByName}"/></span></div>
            <div><p>Đại lý nộp tiền</p><span>(Ký, ghi rõ họ tên)</span></div>
        </footer>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
