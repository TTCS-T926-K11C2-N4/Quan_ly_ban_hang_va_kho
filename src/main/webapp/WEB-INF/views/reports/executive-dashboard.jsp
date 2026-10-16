<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Dashboard điều hành"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard điều hành | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Dashboard điều hành</h1>
                    <p class="inv-header__subtitle">Tình hình bán hàng và kho toàn công ty, cập nhật mỗi lần mở trang.</p>
                </div>
            </header>

            <section class="exec-cards" id="exec-cards" aria-label="Chỉ số chính">
                <article class="exec-card"><p class="exec-card__label">Doanh số hôm nay</p><p class="exec-card__value" id="exec-today"><fmt:formatNumber value="${board.todayRevenue}" maxFractionDigits="0"/> ₫</p></article>
                <article class="exec-card"><p class="exec-card__label">Doanh số tháng này</p><p class="exec-card__value" id="exec-month"><fmt:formatNumber value="${board.monthRevenue}" maxFractionDigits="0"/> ₫</p></article>
                <article class="exec-card"><p class="exec-card__label">Đơn chờ duyệt</p><p class="exec-card__value" id="exec-pending"><a href="<c:url value='/approvals'/>">${board.pendingApproval}</a></p></article>
                <article class="exec-card"><p class="exec-card__label">Đơn chờ xuất kho</p><p class="exec-card__value" id="exec-waiting">${board.waitingShipment}</p></article>
                <article class="exec-card exec-card--alert"><p class="exec-card__label">Công nợ quá hạn</p><p class="exec-card__value" id="exec-overdue"><a href="<c:url value='/debts/overdue'/>"><fmt:formatNumber value="${board.overdueDebt}" maxFractionDigits="0"/> ₫</a></p></article>
                <article class="exec-card"><p class="exec-card__label">SKU dưới tồn tối thiểu</p><p class="exec-card__value" id="exec-min-stock">${board.belowMinStock}</p></article>
            </section>

            <section class="inv-card" aria-labelledby="exec-daily-title">
                <h2 class="inv-card__title" id="exec-daily-title">Doanh số theo ngày trong tháng</h2>
                <ul class="exec-bars" id="exec-daily">
                    <c:forEach var="bar" items="${board.daily}">
                        <li class="exec-bars__row"><span class="exec-bars__label"><c:out value="${bar.label}"/></span>
                            <span class="exec-bars__track"><span class="exec-bars__fill" style="width: ${bar.percent}%"></span></span>
                            <span class="exec-bars__value"><fmt:formatNumber value="${bar.value}" maxFractionDigits="0"/></span></li>
                    </c:forEach>
                </ul>
            </section>

            <section class="inv-card" aria-labelledby="exec-region-title">
                <h2 class="inv-card__title" id="exec-region-title">Doanh số tháng này theo khu vực</h2>
                <ul class="exec-bars" id="exec-regions">
                    <c:forEach var="bar" items="${board.byRegion}">
                        <li class="exec-bars__row"><span class="exec-bars__label"><c:out value="${bar.label}"/></span>
                            <span class="exec-bars__track"><span class="exec-bars__fill" style="width: ${bar.percent}%"></span></span>
                            <span class="exec-bars__value"><fmt:formatNumber value="${bar.value}" maxFractionDigits="0"/></span></li>
                    </c:forEach>
                    <c:if test="${empty board.byRegion}"><li class="inv-table__empty">Chưa có doanh số trong tháng.</li></c:if>
                </ul>
            </section>

            <section class="inv-card" aria-labelledby="exec-overdue-title">
                <h2 class="inv-card__title" id="exec-overdue-title">Đại lý quá hạn công nợ (theo số tiền)</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="exec-overdue-table">
                        <thead><tr><th scope="col">Đại lý</th><th scope="col">Nhân viên</th><th scope="col" class="is-number">Quá hạn</th><th scope="col" class="is-number">Lâu nhất (ngày)</th></tr></thead>
                        <tbody>
                            <c:forEach var="row" items="${board.overdueCustomers}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/debts/detail'><c:param name='customer' value='${row.customerId}'/></c:url>"><c:out value="${row.name}"/></a></td>
                                    <td><c:out value="${row.repName}"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${row.overdue}" maxFractionDigits="0"/> ₫</strong></td>
                                    <td class="is-number">${row.oldestDays}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty board.overdueCustomers}"><tr><td class="inv-table__empty" colspan="4">Không có đại lý nào quá hạn.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
