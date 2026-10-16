<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="statements"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
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
                    <h1 class="inv-header__title">Đối chiếu công nợ</h1>
                    <p class="inv-header__subtitle">Lập biên bản theo kỳ gồm số dư đầu kỳ, phát sinh và số dư cuối kỳ, gửi đại lý xác nhận hoặc báo lệch.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="statement-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="statement-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${currentUser.can('INVOICE_MANAGE')}">
                <section class="inv-card" aria-labelledby="statement-new-title">
                    <h2 class="inv-card__title" id="statement-new-title">Lập biên bản mới</h2>
                    <form class="invoice-filters" id="statement-form" action="<c:url value='/statements'/>" method="post">
                        <div class="inv-field">
                            <label class="stock-filters__label" for="statement-customer">Đại lý</label>
                            <select class="inv-control" id="statement-customer" name="customer" required>
                                <c:forEach var="customer" items="${customers}">
                                    <option value="${customer.id}"><c:out value="${customer.name}"/> (<c:out value="${customer.code}"/>)</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="stock-filters__label" for="statement-from">Từ ngày</label>
                            <input class="inv-control" type="date" id="statement-from" name="from" value="${defaultFrom}" required>
                        </div>
                        <div class="inv-field">
                            <label class="stock-filters__label" for="statement-to">Đến ngày</label>
                            <input class="inv-control" type="date" id="statement-to" name="to" value="${defaultTo}" required>
                        </div>
                        <button class="inv-button inv-button--primary" type="submit" id="statement-create">Lập biên bản</button>
                    </form>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="statement-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="statement-table-title">Biên bản đã lập</h2>
                    <p class="inv-card__meta"><strong>${statements.size()}</strong> biên bản</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="statement-table">
                        <thead>
                            <tr>
                                <th scope="col">Số biên bản</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Kỳ đối chiếu</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Số dư đầu kỳ</th>
                                <th scope="col" class="is-number">Số dư cuối kỳ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${statements}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/statements/view'><c:param name='id' value='${item.id}'/></c:url>"><c:out value="${item.statementNo}"/></a></td>
                                    <td><c:out value="${item.customerName}"/></td>
                                    <td><c:out value="${item.periodText}"/></td>
                                    <td><span class="invoice-pill invoice-pill--${item.statusTone}"><c:out value="${item.statusLabel}"/></span></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.openingBalance}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${item.closingBalance}" maxFractionDigits="0"/></strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty statements}">
                                <tr><td class="inv-table__empty" colspan="6">Chưa có biên bản đối chiếu nào.</td></tr>
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
