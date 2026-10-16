<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="overdue"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Báo cáo công nợ quá hạn"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Báo cáo công nợ quá hạn | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Báo cáo công nợ quá hạn</h1>
                    <p class="inv-header__subtitle">Tuổi nợ tính từ hạn thanh toán của hoá đơn còn nợ. Đại lý xếp theo mức rủi ro: cao khi quá hạn trên 90 ngày, trung bình 31–90 ngày.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--primary" id="overdue-export" href="<c:url value='/debts/overdue/export'/>">Xuất Excel</a>
                </div>
            </header>

            <c:if test="${not empty report.highestRisk}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="overdue-top" role="status">
                    Rủi ro cao nhất: <strong><c:out value="${report.highestRisk.name}"/></strong> (<c:out value="${report.highestRisk.code}"/>) · quá hạn <fmt:formatNumber value="${report.highestRisk.overdue}" maxFractionDigits="0"/> ₫, lâu nhất ${report.highestRisk.oldestDays} ngày.
                </div>
            </c:if>

            <section class="inv-card" aria-labelledby="overdue-bucket-title">
                <h2 class="inv-card__title" id="overdue-bucket-title">Tổng hợp theo nhóm tuổi nợ</h2>
                <dl class="invoice-totals">
                    <div><dt>1–30 ngày quá hạn</dt><dd><fmt:formatNumber value="${report.bucket1To30}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>31–60 ngày</dt><dd><fmt:formatNumber value="${report.bucket31To60}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>61–90 ngày</dt><dd><fmt:formatNumber value="${report.bucket61To90}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Trên 90 ngày</dt><dd><fmt:formatNumber value="${report.bucketOver90}" maxFractionDigits="0"/> ₫</dd></div>
                    <div class="invoice-totals__grand"><dt>Tổng quá hạn</dt><dd id="overdue-total"><fmt:formatNumber value="${report.total}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
            </section>

            <section class="inv-card" aria-labelledby="overdue-rep-title">
                <h2 class="inv-card__title" id="overdue-rep-title">Theo nhân viên phụ trách</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="overdue-reps">
                        <thead>
                            <tr><th scope="col">Nhân viên</th><th scope="col" class="is-number">Công nợ quá hạn</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="rep" items="${report.repTotals}">
                                <tr>
                                    <td><c:out value="${rep.name}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${rep.amount}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty report.repTotals}">
                                <tr><td class="inv-table__empty" colspan="2">Không có công nợ quá hạn.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>

            <section class="inv-card" aria-labelledby="overdue-table-title">
                <h2 class="inv-card__title" id="overdue-table-title">Đại lý quá hạn, xếp theo mức rủi ro</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="overdue-table">
                        <thead>
                            <tr>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Nhân viên</th>
                                <th scope="col">Rủi ro</th>
                                <th scope="col" class="is-number">Quá hạn</th>
                                <th scope="col" class="is-number">1–30</th>
                                <th scope="col" class="is-number">31–60</th>
                                <th scope="col" class="is-number">61–90</th>
                                <th scope="col" class="is-number">&gt;90</th>
                                <th scope="col" class="is-number">Lâu nhất (ngày)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${report.rows}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/debts/detail'><c:param name='customer' value='${row.customerId}'/></c:url>"><c:out value="${row.name}"/></a><span class="inv-table__sub"><c:out value="${row.code}"/></span></td>
                                    <td><c:out value="${row.repName}"/></td>
                                    <td><span class="invoice-pill invoice-pill--${row.riskTone}"><c:out value="${row.riskLabel}"/></span></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${row.overdue}" maxFractionDigits="0"/></strong></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket1To30}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket31To60}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket61To90}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucketOver90}" maxFractionDigits="0"/></td>
                                    <td class="is-number">${row.oldestDays}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty report.rows}">
                                <tr><td class="inv-table__empty" colspan="9">Không có đại lý nào quá hạn thanh toán.</td></tr>
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
