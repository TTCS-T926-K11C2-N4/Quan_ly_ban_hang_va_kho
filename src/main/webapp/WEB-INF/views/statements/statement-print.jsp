<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Biên bản đối chiếu <c:out value="${statement.statementNo}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <a class="print-toolbar__button" href="<c:url value='${backUrl}'/>">← Quay lại</a>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
        <p class="print-toolbar__hint">Trong hộp in chọn "Lưu dạng PDF" để xuất file.</p>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Biên bản đối chiếu công nợ</h1>
                <p class="print-head__meta">Kỳ đối chiếu: <strong><c:out value="${statement.periodText}"/></strong></p>
            </div>
            <div class="print-code">
                <div class="print-code__bars">${barcode}</div>
                <p class="print-code__no"><c:out value="${statement.statementNo}"/></p>
            </div>
        </header>

        <section class="print-grid" aria-label="Thông tin đại lý">
            <div class="print-box">
                <h2 class="print-box__title">Đại lý</h2>
                <p class="print-box__name"><c:out value="${statement.customerName}"/></p>
                <dl class="print-info">
                    <div><dt>Mã đại lý</dt><dd><c:out value="${statement.customerCode}"/></dd></div>
                    <div><dt>Lập lúc</dt><dd><c:out value="${statement.createdAtText}"/></dd></div>
                </dl>
            </div>
        </section>

        <table class="print-items">
            <thead>
                <tr>
                    <th scope="col">Ngày</th>
                    <th scope="col">Loại</th>
                    <th scope="col">Nội dung</th>
                    <th scope="col" class="print-items__num">Tăng nợ</th>
                    <th scope="col" class="print-items__num">Giảm nợ</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${statement.entries}">
                    <tr>
                        <td><c:out value="${entry.dateText}"/></td>
                        <td><c:out value="${entry.typeLabel}"/></td>
                        <td><c:out value="${entry.note}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${entry.debit}" maxFractionDigits="0"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${entry.credit}" maxFractionDigits="0"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <dl class="print-totals">
            <div><dt>Số dư đầu kỳ</dt><dd><fmt:formatNumber value="${statement.openingBalance}" maxFractionDigits="0"/> ₫</dd></div>
            <div><dt>Phát sinh tăng</dt><dd><fmt:formatNumber value="${statement.totalDebit}" maxFractionDigits="0"/> ₫</dd></div>
            <div><dt>Phát sinh giảm</dt><dd><fmt:formatNumber value="${statement.totalCredit}" maxFractionDigits="0"/> ₫</dd></div>
            <div class="print-totals__grand"><dt>Số dư cuối kỳ</dt><dd><fmt:formatNumber value="${statement.closingBalance}" maxFractionDigits="0"/> ₫</dd></div>
        </dl>

        <footer class="print-sign">
            <div><p>Đại diện công ty</p><span>(Ký, ghi rõ họ tên)</span></div>
            <div><p>Đại diện đại lý xác nhận</p><span>(Ký, ghi rõ họ tên)</span></div>
        </footer>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
