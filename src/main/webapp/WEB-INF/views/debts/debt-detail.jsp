<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="debts"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Sổ chi tiết công nợ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sổ chi tiết công nợ | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Sổ chi tiết công nợ</h1>
                    <p class="inv-header__subtitle">Toàn bộ phát sinh tăng giảm của một đại lý theo thời gian, kèm số dư luỹ kế.</p>
                </div>
                <c:if test="${not empty detail}">
                    <div class="inv-header__actions">
                        <a class="inv-button" id="detail-print" target="_blank" rel="noopener" href="<c:url value='/debts/detail/print'><c:param name='customer' value='${customerId}'/><c:param name='from' value='${from}'/><c:param name='to' value='${to}'/></c:url>">In / Xuất PDF</a>
                        <a class="inv-button inv-button--primary" id="detail-export" href="<c:url value='/debts/detail/export'><c:param name='customer' value='${customerId}'/><c:param name='from' value='${from}'/><c:param name='to' value='${to}'/></c:url>">Xuất Excel</a>
                    </div>
                </c:if>
            </header>

            <section class="inv-card" aria-labelledby="detail-filter-title">
                <h2 class="visually-hidden" id="detail-filter-title">Bộ lọc</h2>
                <form class="invoice-filters" action="<c:url value='/debts/detail'/>" method="get">
                    <div class="inv-field">
                        <label class="inv-field__label" for="detail-customer">Đại lý</label>
                        <select class="inv-control" id="detail-customer" name="customer" required>
                            <c:forEach var="customer" items="${customers}">
                                <option value="${customer.id}"${customerId == customer.id ? ' selected' : ''}><c:out value="${customer.name}"/> (<c:out value="${customer.code}"/>)</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="detail-from">Từ ngày</label>
                        <input class="inv-control" type="date" id="detail-from" name="from" value="${from}">
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="detail-to">Đến ngày</label>
                        <input class="inv-control" type="date" id="detail-to" name="to" value="${to}">
                    </div>
                    <button class="inv-button" type="submit" id="detail-filter">Xem sổ</button>
                </form>
            </section>

            <c:if test="${not empty detail}">
                <section class="inv-card" aria-labelledby="detail-table-title">
                    <h2 class="inv-card__title" id="detail-table-title"><c:out value="${detail.customerName}"/> (<c:out value="${detail.customerCode}"/>) · <c:out value="${detail.periodText}"/></h2>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="detail-table">
                            <thead>
                                <tr>
                                    <th scope="col">Ngày</th>
                                    <th scope="col">Loại</th>
                                    <th scope="col">Nội dung</th>
                                    <th scope="col" class="is-number">Tăng nợ</th>
                                    <th scope="col" class="is-number">Giảm nợ</th>
                                    <th scope="col" class="is-number">Số dư luỹ kế</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr id="detail-opening">
                                    <td colspan="5"><strong>Số dư đầu kỳ</strong></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${detail.opening}" maxFractionDigits="0"/></strong></td>
                                </tr>
                                <c:forEach var="row" items="${detail.rows}">
                                    <tr>
                                        <td><c:out value="${row.dateText}"/></td>
                                        <td><c:out value="${row.typeLabel}"/></td>
                                        <td><c:out value="${row.note}"/></td>
                                        <td class="is-number"><fmt:formatNumber value="${row.debit}" maxFractionDigits="0"/></td>
                                        <td class="is-number"><fmt:formatNumber value="${row.credit}" maxFractionDigits="0"/></td>
                                        <td class="is-number"><fmt:formatNumber value="${row.balance}" maxFractionDigits="0"/></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty detail.rows}">
                                    <tr><td class="inv-table__empty" colspan="6">Không có phát sinh trong kỳ.</td></tr>
                                </c:if>
                            </tbody>
                            <tfoot>
                                <tr id="detail-total">
                                    <th scope="row" colspan="3">Tổng phát sinh · số dư cuối kỳ</th>
                                    <td class="is-number"><fmt:formatNumber value="${detail.totalDebit}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${detail.totalCredit}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${detail.closing}" maxFractionDigits="0"/></td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </section>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
