<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="payments"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Thu tiền"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thu tiền | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Thu tiền</h1>
                    <p class="inv-header__subtitle">Ghi nhận thanh toán của đại lý và đối trừ vào hoá đơn. Phiếu thu đã ghi nhận không xoá, không sửa.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="payment-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="payment-debtors-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="payment-debtors-title">Đại lý còn nợ hoá đơn</h2>
                    <p class="inv-card__meta"><strong>${debtors.size()}</strong> đại lý</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="payment-debtors">
                        <thead>
                            <tr>
                                <th scope="col">Đại lý</th>
                                <th scope="col" class="is-number">Số hoá đơn còn nợ</th>
                                <th scope="col" class="is-number">Còn nợ</th>
                                <th scope="col"><span class="visually-hidden">Thao tác</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="debtor" items="${debtors}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${debtor.name}"/></span><span class="inv-table__sub"><c:out value="${debtor.code}"/></span></td>
                                    <td class="is-number">${debtor.openInvoices}</td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${debtor.outstanding}" maxFractionDigits="0"/> ₫</strong></td>
                                    <td>
                                        <c:if test="${canManage}">
                                            <a class="inv-button inv-button--primary" href="<c:url value='/payments/new'><c:param name='customer' value='${debtor.customerId}'/></c:url>">Ghi nhận thu tiền</a>
                                        </c:if>
                                        <a class="inv-button" href="<c:url value='/payments'><c:param name='customer' value='${debtor.customerId}'/></c:url>">Lịch sử thu</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty debtors}">
                                <tr><td class="inv-table__empty" colspan="4">Không có đại lý nào còn nợ hoá đơn.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="inv-card" aria-labelledby="payment-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="payment-table-title">Phiếu thu đã ghi nhận</h2>
                    <p class="inv-card__meta"><strong>${payments.size()}</strong> phiếu<c:if test="${not empty customerId}"> · <a href="<c:url value='/payments'/>">Xem tất cả đại lý</a></c:if></p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="payment-table">
                        <thead>
                            <tr>
                                <th scope="col">Số phiếu</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Ngày thu</th>
                                <th scope="col">Hình thức</th>
                                <th scope="col">Đối trừ vào</th>
                                <th scope="col">Người thu</th>
                                <th scope="col" class="is-number">Số tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="payment" items="${payments}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${payment.receiptNo}"/></span><span class="inv-table__sub"><c:out value="${payment.statusLabel}"/></span></td>
                                    <td><c:out value="${payment.customerName}"/></td>
                                    <td><c:out value="${payment.paymentDateText}"/></td>
                                    <td><c:out value="${payment.methodLabel}"/></td>
                                    <td>
                                        <c:forEach var="allocation" items="${payment.allocations}">
                                            <a class="inv-table__sub" href="<c:url value='/invoices/view'><c:param name='id' value='${allocation.invoiceId}'/></c:url>"><c:out value="${allocation.invoiceNo}"/> (<fmt:formatNumber value="${allocation.amount}" maxFractionDigits="0"/> ₫)</a>
                                        </c:forEach>
                                    </td>
                                    <td><c:out value="${payment.collectedByName}"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${payment.amount}" maxFractionDigits="0"/> ₫</strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty payments}">
                                <tr><td class="inv-table__empty" colspan="7">Chưa có phiếu thu nào.</td></tr>
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
