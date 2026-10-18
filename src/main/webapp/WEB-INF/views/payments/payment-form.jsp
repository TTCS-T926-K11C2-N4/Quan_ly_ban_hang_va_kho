<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="payments"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Ghi nhận thu tiền"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ghi nhận thu tiền | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Ghi nhận thu tiền</h1>
                    <p class="inv-header__subtitle"><c:out value="${customerName}"/> · còn nợ <strong><fmt:formatNumber value="${totalOutstanding}" maxFractionDigits="0"/> ₫</strong> trên ${openInvoices.size()} hoá đơn</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/payments'/>">← Danh sách thu tiền</a>
                </div>
            </header>

            <c:if test="${not empty formError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="payment-error" role="alert"><c:out value="${formError}"/></div>
            </c:if>

            <form action="<c:url value='/payments/new'/>" method="post" id="payment-form">
                <input type="hidden" name="customer" value="${customerId}">
                <section class="inv-card" aria-labelledby="payment-open-title">
                    <h2 class="inv-card__title" id="payment-open-title">Hoá đơn còn nợ</h2>
                    <p class="inv-card__subtitle">Tick hoá đơn muốn đối trừ; để trống thì hệ thống tự đối trừ theo nguyên tắc hoá đơn cũ nhất trước.</p>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="payment-open">
                            <thead>
                                <tr>
                                    <th scope="col"><span class="visually-hidden">Chọn</span></th>
                                    <th scope="col">Số hoá đơn</th>
                                    <th scope="col">Ngày lập</th>
                                    <th scope="col">Hạn thanh toán</th>
                                    <th scope="col" class="is-number">Tổng tiền</th>
                                    <th scope="col" class="is-number">Còn nợ</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="invoice" items="${openInvoices}">
                                    <tr class="${invoice.overdue ? 'invoice-row--overdue' : ''}">
                                        <td><input type="checkbox" name="invoice_id" value="${invoice.id}"${selected.contains(invoice.id) ? ' checked' : ''} aria-label="Chọn hoá đơn ${invoice.invoiceNo}"></td>
                                        <td><a class="inv-table__main" href="<c:url value='/invoices/view'><c:param name='id' value='${invoice.id}'/></c:url>"><c:out value="${invoice.invoiceNo}"/></a></td>
                                        <td><c:out value="${invoice.issueDateText}"/></td>
                                        <td><c:out value="${invoice.dueDateText}"/><c:if test="${invoice.overdue}"> <span class="invoice-pill invoice-pill--danger">Quá hạn</span></c:if></td>
                                        <td class="is-number"><fmt:formatNumber value="${invoice.total}" maxFractionDigits="0"/> ₫</td>
                                        <td class="is-number"><strong><fmt:formatNumber value="${invoice.outstanding}" maxFractionDigits="0"/> ₫</strong></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="inv-card" aria-labelledby="payment-detail-title">
                    <h2 class="inv-card__title" id="payment-detail-title">Thông tin thu tiền</h2>
                    <div class="invoice-filters">
                        <div class="inv-field">
                            <label class="inv-field__label" for="payment-amount">Số tiền thu (₫)</label>
                            <input class="inv-control" type="text" inputmode="numeric" id="payment-amount" name="amount" required value="<c:out value='${amount}'/>">
                        </div>
                        <div class="inv-field">
                            <label class="inv-field__label" for="payment-method">Hình thức thu</label>
                            <select class="inv-control" id="payment-method" name="method">
                                <option value="CASH"${method != 'BANK_TRANSFER' ? ' selected' : ''}>Tiền mặt</option>
                                <option value="BANK_TRANSFER"${method == 'BANK_TRANSFER' ? ' selected' : ''}>Chuyển khoản</option>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="inv-field__label" for="payment-date">Ngày thu</label>
                            <input class="inv-control" type="date" id="payment-date" name="date" value="<c:out value='${date}'/>">
                        </div>
                    </div>
                    <div class="invoice-issue">
                        <input class="inv-control" type="text" name="note" maxlength="1000" placeholder="Ghi chú (tuỳ chọn)" aria-label="Ghi chú" value="<c:out value='${note}'/>">
                        <button class="inv-button inv-button--primary" type="submit" id="payment-submit">Ghi nhận thu tiền</button>
                    </div>
                </section>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
