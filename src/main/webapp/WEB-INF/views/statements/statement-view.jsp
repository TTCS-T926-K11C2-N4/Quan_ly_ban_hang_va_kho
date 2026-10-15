<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="statements"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Biên bản đối chiếu"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Biên bản <c:out value="${statement.statementNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/invoices.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Biên bản <c:out value="${statement.statementNo}"/>
                        <span class="invoice-pill invoice-pill--${statement.statusTone}" id="statement-status"><c:out value="${statement.statusLabel}"/></span></h1>
                    <p class="inv-header__subtitle"><c:out value="${statement.customerName}"/> (<c:out value="${statement.customerCode}"/>) · Kỳ <c:out value="${statement.periodText}"/> · Lập lúc <c:out value="${statement.createdAtText}"/></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/statements'/>">← Danh sách biên bản</a>
                    <a class="inv-button" id="statement-print" target="_blank" rel="noopener" href="<c:url value='/statements/print'><c:param name='id' value='${statement.id}'/></c:url>">In / Xuất PDF</a>
                    <c:if test="${statement.draft and canManage}">
                        <form action="<c:url value='/statements/view'/>" method="post">
                            <input type="hidden" name="id" value="${statement.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="statement-send">Gửi cho đại lý</button>
                        </form>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="statement-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="statement-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="statement-sum-title">
                <h2 class="inv-card__title" id="statement-sum-title">Tổng hợp công nợ trong kỳ</h2>
                <dl class="invoice-totals">
                    <div><dt>Số dư đầu kỳ</dt><dd><fmt:formatNumber value="${statement.openingBalance}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Phát sinh tăng (hoá đơn)</dt><dd><fmt:formatNumber value="${statement.totalDebit}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Phát sinh giảm (thanh toán)</dt><dd><fmt:formatNumber value="${statement.totalCredit}" maxFractionDigits="0"/> ₫</dd></div>
                    <div class="invoice-totals__grand"><dt>Số dư cuối kỳ</dt><dd id="statement-closing"><fmt:formatNumber value="${statement.closingBalance}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
                <c:if test="${not empty statement.respondedAtText}">
                    <p class="invoice-note"><strong>Phản hồi của đại lý:</strong> <c:out value="${statement.statusLabel}"/> bởi <c:out value="${statement.respondedByName}"/> lúc <c:out value="${statement.respondedAtText}"/><c:if test="${not empty statement.responseNote}"> — <c:out value="${statement.responseNote}"/></c:if></p>
                </c:if>
            </section>

            <section class="inv-card" aria-labelledby="statement-entries-title">
                <h2 class="inv-card__title" id="statement-entries-title">Phát sinh trong kỳ</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="statement-entries">
                        <thead>
                            <tr>
                                <th scope="col">Ngày</th>
                                <th scope="col">Loại</th>
                                <th scope="col">Nội dung</th>
                                <th scope="col" class="is-number">Tăng nợ</th>
                                <th scope="col" class="is-number">Giảm nợ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="entry" items="${statement.entries}">
                                <tr>
                                    <td><c:out value="${entry.dateText}"/></td>
                                    <td><c:out value="${entry.typeLabel}"/></td>
                                    <td><c:out value="${entry.note}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${entry.debit}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${entry.credit}" maxFractionDigits="0"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty statement.entries}">
                                <tr><td class="inv-table__empty" colspan="5">Không có phát sinh trong kỳ.</td></tr>
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
