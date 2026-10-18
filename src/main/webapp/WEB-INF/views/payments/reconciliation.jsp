<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="reconcile"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Đối chiếu tiền thu"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đối chiếu tiền thu | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Đối chiếu tiền thu</h1>
                    <p class="inv-header__subtitle">Tiền thu theo từng nhân viên và từng ngày: đã nộp (kế toán xác nhận), còn chờ nộp và đã huỷ.</p>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="reconcile-title">
                <form class="invoice-filters" action="<c:url value='/collections/reconcile'/>" method="get">
                    <div class="inv-field">
                        <label class="inv-field__label" for="reconcile-from">Từ ngày</label>
                        <input class="inv-control" type="date" id="reconcile-from" name="from" value="${from}">
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="reconcile-to">Đến ngày</label>
                        <input class="inv-control" type="date" id="reconcile-to" name="to" value="${to}">
                    </div>
                    <button class="inv-button" type="submit" id="reconcile-filter">Xem</button>
                </form>
                <h2 class="visually-hidden" id="reconcile-title">Bảng đối chiếu</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="reconcile-table">
                        <thead>
                            <tr>
                                <th scope="col">Ngày thu</th>
                                <th scope="col">Nhân viên</th>
                                <th scope="col" class="is-number">Số khoản</th>
                                <th scope="col" class="is-number">Tổng đã thu</th>
                                <th scope="col" class="is-number">Đã nộp / xác nhận</th>
                                <th scope="col" class="is-number">Chờ nộp</th>
                                <th scope="col" class="is-number">Đã huỷ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${rows}">
                                <tr>
                                    <td><c:out value="${row.dateText}"/></td>
                                    <td><c:out value="${row.userName}"/></td>
                                    <td class="is-number">${row.count}</td>
                                    <td class="is-number"><fmt:formatNumber value="${row.total}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.confirmed}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><c:choose><c:when test="${row.pending.signum() > 0}"><span class="invoice-pill invoice-pill--warning"><fmt:formatNumber value="${row.pending}" maxFractionDigits="0"/></span></c:when><c:otherwise>0</c:otherwise></c:choose></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.voided}" maxFractionDigits="0"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rows}">
                                <tr><td class="inv-table__empty" colspan="7">Không có khoản thu nào trong khoảng này.</td></tr>
                            </c:if>
                        </tbody>
                        <tfoot>
                            <tr id="reconcile-total">
                                <th scope="row" colspan="3">Tổng cộng</th>
                                <td class="is-number"><fmt:formatNumber value="${sumTotal}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${sumConfirmed}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${sumPending}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${sumVoided}" maxFractionDigits="0"/></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
