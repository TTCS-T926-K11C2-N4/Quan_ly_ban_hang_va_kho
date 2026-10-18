<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Doanh số"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Báo cáo doanh số | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Báo cáo doanh số</h1>
                    <p class="inv-header__subtitle">Theo hoá đơn đã phát hành (chưa trừ chứng từ giảm trừ do trả hàng). So sánh với kỳ liền trước cùng độ dài: <c:out value="${report.previousFrom}"/> – <c:out value="${report.previousTo}"/>.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--primary" id="sales-export" href="<c:url value='/reports/sales/export'><c:param name='dim' value='${report.dimension}'/><c:param name='from' value='${from}'/><c:param name='to' value='${to}'/></c:url>">Xuất Excel</a>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="sales-filter-title">
                <h2 class="visually-hidden" id="sales-filter-title">Bộ lọc</h2>
                <form class="invoice-filters" action="<c:url value='/reports/sales'/>" method="get">
                    <div class="inv-field">
                        <label class="inv-field__label" for="sales-dim">Nhóm theo</label>
                        <select class="inv-control" id="sales-dim" name="dim">
                            <c:forEach var="entry" items="${dimensions}">
                                <option value="${entry.key}"${report.dimension == entry.key ? ' selected' : ''}><c:out value="${entry.value}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="sales-from">Từ ngày</label>
                        <input class="inv-control" type="date" id="sales-from" name="from" value="${from}">
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="sales-to">Đến ngày</label>
                        <input class="inv-control" type="date" id="sales-to" name="to" value="${to}">
                    </div>
                    <button class="inv-button" type="submit" id="sales-filter">Xem báo cáo</button>
                </form>
            </section>

            <section class="inv-card" aria-labelledby="sales-table-title">
                <h2 class="inv-card__title" id="sales-table-title">Doanh số theo <c:out value="${report.dimensionLabel}"/></h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="sales-table">
                        <thead>
                            <tr>
                                <th scope="col"><c:out value="${report.dimensionLabel}"/></th>
                                <th scope="col" class="is-number">Doanh số</th>
                                <th scope="col" class="is-number">Sản lượng</th>
                                <c:if test="${report.showMargin}">
                                    <th scope="col" class="is-number">Lợi nhuận gộp</th>
                                    <th scope="col" class="is-number">Biên (%)</th>
                                </c:if>
                                <th scope="col" class="is-number">Kỳ trước</th>
                                <th scope="col" class="is-number">Thay đổi</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${report.rows}">
                                <tr>
                                    <td><c:out value="${row.name}"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${row.revenue}" maxFractionDigits="0"/></strong></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.qty}" maxFractionDigits="3"/></td>
                                    <c:if test="${report.showMargin}">
                                        <td class="is-number"><fmt:formatNumber value="${row.profit}" maxFractionDigits="0"/></td>
                                        <td class="is-number"><c:choose><c:when test="${empty row.marginPercent}">—</c:when><c:otherwise><fmt:formatNumber value="${row.marginPercent}" maxFractionDigits="1"/></c:otherwise></c:choose></td>
                                    </c:if>
                                    <td class="is-number"><fmt:formatNumber value="${row.previousRevenue}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><c:choose><c:when test="${empty row.changePercent}">—</c:when><c:otherwise><fmt:formatNumber value="${row.changePercent}" maxFractionDigits="1"/>%</c:otherwise></c:choose></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty report.rows}">
                                <tr><td class="inv-table__empty" colspan="7">Không có doanh số trong kỳ.</td></tr>
                            </c:if>
                        </tbody>
                        <tfoot>
                            <tr id="sales-total">
                                <th scope="row">Tổng cộng</th>
                                <td class="is-number"><fmt:formatNumber value="${report.total.revenue}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${report.total.qty}" maxFractionDigits="3"/></td>
                                <c:if test="${report.showMargin}">
                                    <td class="is-number"><fmt:formatNumber value="${report.total.profit}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><c:choose><c:when test="${empty report.total.marginPercent}">—</c:when><c:otherwise><fmt:formatNumber value="${report.total.marginPercent}" maxFractionDigits="1"/></c:otherwise></c:choose></td>
                                </c:if>
                                <td class="is-number"><fmt:formatNumber value="${report.total.previousRevenue}" maxFractionDigits="0"/></td>
                                <td class="is-number"><c:choose><c:when test="${empty report.total.changePercent}">—</c:when><c:otherwise><fmt:formatNumber value="${report.total.changePercent}" maxFractionDigits="1"/>%</c:otherwise></c:choose></td>
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
