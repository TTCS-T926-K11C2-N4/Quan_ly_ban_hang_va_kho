<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sổ chi tiết công nợ <c:out value="${detail.customerCode}"/></title>
    <link rel="stylesheet" href="<c:url value='/assets/css/order-print.css'/>">
</head>
<body class="print-page">
    <div class="print-toolbar">
        <a class="print-toolbar__button" href="<c:url value='/debts/detail'><c:param name='customer' value='${detail.customerId}'/><c:param name='from' value='${from}'/><c:param name='to' value='${to}'/></c:url>">← Quay lại sổ</a>
        <button class="print-toolbar__button print-toolbar__button--primary" type="button" id="print-order">In / Lưu PDF</button>
        <p class="print-toolbar__hint">Trong hộp in chọn "Lưu dạng PDF" để xuất file.</p>
    </div>

    <main class="print-sheet">
        <header class="print-head">
            <div>
                <p class="print-head__org">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="print-head__title">Sổ chi tiết công nợ</h1>
                <p class="print-head__meta">Kỳ: <strong><c:out value="${detail.periodText}"/></strong></p>
            </div>
        </header>

        <section class="print-grid" aria-label="Thông tin đại lý">
            <div class="print-box">
                <h2 class="print-box__title">Đại lý</h2>
                <p class="print-box__name"><c:out value="${detail.customerName}"/></p>
                <dl class="print-info">
                    <div><dt>Mã đại lý</dt><dd><c:out value="${detail.customerCode}"/></dd></div>
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
                    <th scope="col" class="print-items__num">Số dư luỹ kế</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td colspan="5"><strong>Số dư đầu kỳ</strong></td>
                    <td class="print-items__num"><strong><fmt:formatNumber value="${detail.opening}" maxFractionDigits="0"/></strong></td>
                </tr>
                <c:forEach var="row" items="${detail.rows}">
                    <tr>
                        <td><c:out value="${row.dateText}"/></td>
                        <td><c:out value="${row.typeLabel}"/></td>
                        <td><c:out value="${row.note}"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${row.debit}" maxFractionDigits="0"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${row.credit}" maxFractionDigits="0"/></td>
                        <td class="print-items__num"><fmt:formatNumber value="${row.balance}" maxFractionDigits="0"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <dl class="print-totals">
            <div><dt>Tổng phát sinh tăng</dt><dd><fmt:formatNumber value="${detail.totalDebit}" maxFractionDigits="0"/> ₫</dd></div>
            <div><dt>Tổng phát sinh giảm</dt><dd><fmt:formatNumber value="${detail.totalCredit}" maxFractionDigits="0"/> ₫</dd></div>
            <div class="print-totals__grand"><dt>Số dư cuối kỳ</dt><dd><fmt:formatNumber value="${detail.closing}" maxFractionDigits="0"/> ₫</dd></div>
        </dl>
    </main>

    <script src="<c:url value='/assets/js/order-print.js'/>"></script>
</body>
</html>
