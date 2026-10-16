<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="portal-statements"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Đối chiếu công nợ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đối chiếu công nợ | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/portal.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content portal-orders">
            <header class="page-header">
                <h1 class="page-header__title">Đối chiếu công nợ</h1>
                <p class="page-header__subtitle">Biên bản công ty gửi theo kỳ. Kiểm tra số liệu rồi xác nhận hoặc báo lệch.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="portal-statement-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="portal-statement-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:choose>
                <c:when test="${empty statements}">
                    <p class="sales-empty" id="portal-statements-empty">Chưa có biên bản đối chiếu nào được gửi cho bạn.</p>
                </c:when>
                <c:otherwise>
                    <c:forEach var="item" items="${statements}">
                        <section class="sales-card portal-statement" aria-label="Biên bản ${item.statementNo}">
                            <h2 class="portal-statement__title"><c:out value="${item.statementNo}"/> · <c:out value="${item.periodText}"/>
                                <span class="portal-statement__status"><c:out value="${item.statusLabel}"/></span></h2>
                            <dl class="portal-debt-summary__list">
                                <div><dt>Số dư đầu kỳ</dt><dd><fmt:formatNumber value="${item.openingBalance}" maxFractionDigits="0"/> ₫</dd></div>
                                <div><dt>Phát sinh tăng</dt><dd><fmt:formatNumber value="${item.totalDebit}" maxFractionDigits="0"/> ₫</dd></div>
                                <div><dt>Phát sinh giảm</dt><dd><fmt:formatNumber value="${item.totalCredit}" maxFractionDigits="0"/> ₫</dd></div>
                                <div><dt>Số dư cuối kỳ</dt><dd><fmt:formatNumber value="${item.closingBalance}" maxFractionDigits="0"/> ₫</dd></div>
                            </dl>
                            <p><a class="portal-orders__link" target="_blank" rel="noopener" href="<c:url value='/portal/statements/print'><c:param name='id' value='${item.id}'/></c:url>">Xem / tải PDF biên bản</a></p>
                            <c:choose>
                                <c:when test="${item.awaitingResponse}">
                                    <form class="portal-statement__form" action="<c:url value='/portal/statements/respond'/>" method="post">
                                        <input type="hidden" name="id" value="${item.id}">
                                        <label class="portal-statement__label" for="note-${item.id}">Ghi chú (bắt buộc khi báo lệch)</label>
                                        <input class="portal-statement__input" type="text" id="note-${item.id}" name="note" maxlength="1000">
                                        <button class="sales-button sales-button--primary" type="submit" name="decision" value="confirm">Xác nhận số liệu</button>
                                        <button class="sales-button" type="submit" name="decision" value="dispute">Báo lệch</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <c:if test="${not empty item.responseNote}"><p><strong>Phản hồi của bạn:</strong> <c:out value="${item.responseNote}"/></p></c:if>
                                </c:otherwise>
                            </c:choose>
                        </section>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
