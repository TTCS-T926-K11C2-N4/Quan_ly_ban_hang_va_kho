<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="collections"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Thu tiền tại cửa hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thu tiền tại cửa hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Thu tiền tại cửa hàng</h1>
                    <p class="inv-header__subtitle">Ghi nhận khoản thu kèm ảnh biên lai. Khoản thu ở trạng thái Chờ nộp cho tới khi kế toán xác nhận đã nhận tiền.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="collection-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty formError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="collection-error" role="alert"><c:out value="${formError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="collection-form-title">
                <h2 class="inv-card__title" id="collection-form-title">Ghi nhận khoản thu</h2>
                <c:choose>
                    <c:when test="${empty debtors}">
                        <p class="inv-table__empty" id="collection-empty">Không có đại lý nào bạn phụ trách đang còn nợ hoá đơn.</p>
                    </c:when>
                    <c:otherwise>
                        <form class="collection-form" id="collection-form" action="<c:url value='/collections/new'/>" method="post" enctype="multipart/form-data">
                            <div class="inv-field collection-form__wide">
                                <label class="inv-field__label" for="collection-customer">Đại lý</label>
                                <select class="inv-control" id="collection-customer" name="customer" required>
                                    <c:forEach var="debtor" items="${debtors}">
                                        <option value="${debtor.customerId}"${customer == debtor.customerId.toString() ? ' selected' : ''}><c:out value="${debtor.name}"/> (<c:out value="${debtor.code}"/>) · còn nợ <fmt:formatNumber value="${debtor.outstanding}" maxFractionDigits="0"/> ₫</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="inv-field">
                                <label class="inv-field__label" for="collection-amount">Số tiền thu (₫)</label>
                                <input class="inv-control" type="text" inputmode="numeric" id="collection-amount" name="amount" required value="<c:out value='${amount}'/>">
                            </div>
                            <div class="inv-field">
                                <label class="inv-field__label" for="collection-method">Hình thức thu</label>
                                <select class="inv-control" id="collection-method" name="method">
                                    <option value="CASH"${method != 'BANK_TRANSFER' ? ' selected' : ''}>Tiền mặt</option>
                                    <option value="BANK_TRANSFER"${method == 'BANK_TRANSFER' ? ' selected' : ''}>Chuyển khoản</option>
                                </select>
                            </div>
                            <div class="inv-field">
                                <label class="inv-field__label" for="collection-date">Ngày thu</label>
                                <input class="inv-control" type="date" id="collection-date" name="date" value="<c:out value='${date}'/>">
                            </div>
                            <div class="inv-field">
                                <label class="inv-field__label" for="collection-receipt">Ảnh biên lai (JPG/PNG, tối đa ${maxReceiptMb}MB)</label>
                                <input class="inv-control" type="file" id="collection-receipt" name="receipt" accept="image/jpeg,image/png" capture="environment" required>
                            </div>
                            <div class="inv-field collection-form__wide">
                                <label class="inv-field__label" for="collection-note">Ghi chú</label>
                                <input class="inv-control" type="text" id="collection-note" name="note" maxlength="1000" value="<c:out value='${note}'/>">
                            </div>
                            <div class="collection-form__wide">
                                <button class="inv-button inv-button--primary" type="submit" id="collection-submit">Ghi nhận khoản thu</button>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="inv-card" aria-labelledby="collection-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="collection-table-title">Khoản thu của tôi</h2>
                    <p class="inv-card__meta"><strong>${collections.size()}</strong> khoản</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="collection-table">
                        <thead>
                            <tr>
                                <th scope="col">Số phiếu</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Ngày thu</th>
                                <th scope="col">Hình thức</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Số tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${collections}">
                                <tr>
                                    <td><c:out value="${item.receiptNo}"/></td>
                                    <td><c:out value="${item.customerName}"/></td>
                                    <td><c:out value="${item.paymentDateText}"/></td>
                                    <td><c:out value="${item.methodLabel}"/></td>
                                    <td><span class="invoice-pill ${item.pending ? 'invoice-pill--warning' : 'invoice-pill--success'}"><c:out value="${item.statusLabel}"/></span></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${item.amount}" maxFractionDigits="0"/> ₫</strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty collections}">
                                <tr><td class="inv-table__empty" colspan="6">Bạn chưa ghi nhận khoản thu nào.</td></tr>
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
