<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Chỉ tiêu doanh số"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chỉ tiêu doanh số | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chỉ tiêu doanh số tháng <c:out value="${month}"/></h1>
                    <p class="inv-header__subtitle">Doanh số thực tế tính từ hoá đơn đã phát hành trong tháng. Chỉ tiêu của tháng đã qua bị khoá: sửa phải có lý do và được ghi nhật ký.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="target-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="target-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="target-month-title">
                <h2 class="visually-hidden" id="target-month-title">Chọn tháng</h2>
                <form class="invoice-filters" action="<c:url value='/targets'/>" method="get">
                    <div class="inv-field">
                        <label class="stock-filters__label" for="target-month">Tháng</label>
                        <input class="inv-control" type="month" id="target-month" name="month" value="${month}">
                    </div>
                    <button class="inv-button" type="submit" id="target-view">Xem</button>
                </form>
            </section>

            <form action="<c:url value='/targets'/>" method="post" id="target-form">
                <input type="hidden" name="month" value="${month}">
                <section class="inv-card" aria-labelledby="target-rep-title">
                    <h2 class="inv-card__title" id="target-rep-title">Theo nhân viên</h2>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="target-reps">
                            <thead>
                                <tr>
                                    <th scope="col">Nhân viên</th>
                                    <th scope="col">Chỉ tiêu (₫)</th>
                                    <th scope="col" class="is-number">Thực tế</th>
                                    <th scope="col">Tiến độ</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="row" items="${reps}">
                                    <tr>
                                        <td><c:out value="${row.name}"/></td>
                                        <td><input class="inv-control" type="text" inputmode="numeric" name="${row.key}" value="${row.target.signum() == 0 ? '' : row.target.toBigInteger()}" aria-label="Chỉ tiêu ${row.name}"></td>
                                        <td class="is-number"><fmt:formatNumber value="${row.actual}" maxFractionDigits="0"/></td>
                                        <td><c:choose><c:when test="${empty row.percent}">Chưa đặt chỉ tiêu</c:when><c:otherwise><span class="invoice-pill invoice-pill--${row.behind ? 'danger' : 'success'}">${row.percent}%</span> (kỳ đã qua ${row.expectedPercent}%)</c:otherwise></c:choose></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty reps}">
                                    <tr><td class="inv-table__empty" colspan="4">Chưa có nhân viên kinh doanh.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="inv-card" aria-labelledby="target-region-title">
                    <h2 class="inv-card__title" id="target-region-title">Theo khu vực</h2>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="target-regions">
                            <thead>
                                <tr>
                                    <th scope="col">Khu vực</th>
                                    <th scope="col">Chỉ tiêu (₫)</th>
                                    <th scope="col" class="is-number">Thực tế</th>
                                    <th scope="col">Tiến độ</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="row" items="${regions}">
                                    <tr>
                                        <td><c:out value="${row.name}"/></td>
                                        <td><input class="inv-control" type="text" inputmode="numeric" name="${row.key}" value="${row.target.signum() == 0 ? '' : row.target.toBigInteger()}" aria-label="Chỉ tiêu ${row.name}"></td>
                                        <td class="is-number"><fmt:formatNumber value="${row.actual}" maxFractionDigits="0"/></td>
                                        <td><c:choose><c:when test="${empty row.percent}">Chưa đặt chỉ tiêu</c:when><c:otherwise><span class="invoice-pill invoice-pill--${row.behind ? 'danger' : 'success'}">${row.percent}%</span> (kỳ đã qua ${row.expectedPercent}%)</c:otherwise></c:choose></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="inv-card" aria-labelledby="target-save-title">
                    <h2 class="visually-hidden" id="target-save-title">Lưu</h2>
                    <div class="invoice-issue">
                        <c:if test="${locked}">
                            <input class="inv-control" type="text" name="reason" maxlength="500" placeholder="Lý do sửa chỉ tiêu tháng đã qua (bắt buộc)" aria-label="Lý do sửa">
                        </c:if>
                        <button class="inv-button inv-button--primary" type="submit" id="target-save">Lưu chỉ tiêu</button>
                    </div>
                </section>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
