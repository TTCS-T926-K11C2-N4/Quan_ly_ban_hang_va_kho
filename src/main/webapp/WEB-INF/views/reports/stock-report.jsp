<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Tồn kho và vòng quay"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Báo cáo tồn kho | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Báo cáo tồn kho và vòng quay hàng</h1>
                    <p class="inv-header__subtitle">Số ngày tồn = tồn hiện tại / bán bình quân mỗi ngày trong ${report.windowDays} ngày gần nhất. Chậm luân chuyển: không bán ra hoặc tồn trên ${report.slowDays} ngày.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--primary" id="stock-report-export" href="<c:url value='/reports/stock/export'><c:param name='window' value='${report.windowDays}'/><c:param name='slow' value='${report.slowDays}'/></c:url>">Xuất Excel</a>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="stock-filter-title">
                <h2 class="visually-hidden" id="stock-filter-title">Tham số</h2>
                <form class="invoice-filters" action="<c:url value='/reports/stock'/>" method="get">
                    <div class="inv-field">
                        <label class="stock-filters__label" for="stock-window">Tính doanh số bán trong (ngày)</label>
                        <input class="inv-control" type="number" min="1" max="365" id="stock-window" name="window" value="${report.windowDays}">
                    </div>
                    <div class="inv-field">
                        <label class="stock-filters__label" for="stock-slow">Ngưỡng chậm luân chuyển (ngày tồn)</label>
                        <input class="inv-control" type="number" min="1" max="3650" id="stock-slow" name="slow" value="${report.slowDays}">
                    </div>
                    <button class="inv-button" type="submit" id="stock-report-filter">Cập nhật</button>
                </form>
            </section>

            <c:if test="${report.showValue}">
                <section class="inv-card" aria-labelledby="stock-value-title">
                    <h2 class="inv-card__title" id="stock-value-title">Giá trị tồn: tổng <span id="stock-value-total"><fmt:formatNumber value="${report.totalValue}" maxFractionDigits="0"/></span> ₫</h2>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="stock-by-warehouse">
                            <thead><tr><th scope="col">Theo kho</th><th scope="col" class="is-number">Giá trị tồn</th></tr></thead>
                            <tbody>
                                <c:forEach var="group" items="${report.byWarehouse}">
                                    <tr><td><c:out value="${group.name}"/></td><td class="is-number"><fmt:formatNumber value="${group.value}" maxFractionDigits="0"/> ₫</td></tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <div class="inv-table-scroll">
                        <table class="inv-table" id="stock-by-category">
                            <thead><tr><th scope="col">Theo nhóm hàng</th><th scope="col" class="is-number">Giá trị tồn</th></tr></thead>
                            <tbody>
                                <c:forEach var="group" items="${report.byCategory}">
                                    <tr><td><c:out value="${group.name}"/></td><td class="is-number"><fmt:formatNumber value="${group.value}" maxFractionDigits="0"/> ₫</td></tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="stock-sku-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="stock-sku-title">Số ngày tồn theo SKU</h2>
                    <p class="inv-card__meta"><strong id="stock-slow-count">${report.slowCount}</strong> SKU chậm luân chuyển</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="stock-sku-table">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col">Nhóm hàng</th>
                                <th scope="col" class="is-number">Tồn</th>
                                <th scope="col" class="is-number">Bán ra trong kỳ</th>
                                <th scope="col" class="is-number">Số ngày tồn</th>
                                <th scope="col">Đánh giá</th>
                                <c:if test="${report.showValue}"><th scope="col" class="is-number">Giá trị tồn</th></c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="sku" items="${report.skus}">
                                <tr class="${sku.slow ? 'invoice-row--overdue' : ''}">
                                    <td><span class="inv-table__main"><c:out value="${sku.name}"/></span><span class="inv-table__sub"><c:out value="${sku.sku}"/></span></td>
                                    <td><c:out value="${sku.category}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${sku.onHand}" maxFractionDigits="3"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${sku.sold}" maxFractionDigits="3"/></td>
                                    <td class="is-number"><c:choose><c:when test="${empty sku.daysOfStock}">—</c:when><c:otherwise><fmt:formatNumber value="${sku.daysOfStock}" maxFractionDigits="1"/></c:otherwise></c:choose></td>
                                    <td><c:if test="${sku.slow}"><span class="invoice-pill invoice-pill--danger">Chậm luân chuyển</span></c:if></td>
                                    <c:if test="${report.showValue}"><td class="is-number"><fmt:formatNumber value="${sku.value}" maxFractionDigits="0"/> ₫</td></c:if>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty report.skus}">
                                <tr><td class="inv-table__empty" colspan="7">Không có hàng tồn.</td></tr>
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
