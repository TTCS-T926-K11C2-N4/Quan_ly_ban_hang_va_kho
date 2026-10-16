<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="debts"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Sổ công nợ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sổ công nợ | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Sổ công nợ theo đại lý</h1>
                    <p class="inv-header__subtitle">Phải thu, đã thu, còn nợ và phân tích tuổi nợ tính từ ngày lập hoá đơn. Cột "Quá hạn" là phần còn nợ của hoá đơn đã quá hạn thanh toán.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--primary" id="debt-export" href="<c:url value='/debts/export'><c:if test="${overdue}"><c:param name='overdue' value='1'/></c:if></c:url>">Xuất Excel</a>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="debt-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="debt-table-title">Công nợ phải thu</h2>
                    <p class="inv-card__meta"><strong>${rows.size()}</strong> đại lý</p>
                </div>
                <form class="invoice-filters" action="<c:url value='/debts'/>" method="get">
                    <label class="invoice-filters__check"><input type="checkbox" name="overdue" value="1"${overdue ? ' checked' : ''}> Chỉ đại lý có công nợ quá hạn</label>
                    <button class="inv-button" type="submit" id="debt-filter">Lọc</button>
                </form>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="debt-table">
                        <thead>
                            <tr>
                                <th scope="col">Đại lý</th>
                                <th scope="col" class="is-number">Phải thu</th>
                                <th scope="col" class="is-number">Đã thu</th>
                                <th scope="col" class="is-number">Còn nợ</th>
                                <th scope="col" class="is-number">0–30 ngày</th>
                                <th scope="col" class="is-number">31–60 ngày</th>
                                <th scope="col" class="is-number">61–90 ngày</th>
                                <th scope="col" class="is-number">Trên 90 ngày</th>
                                <th scope="col" class="is-number">Quá hạn</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${rows}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/debts/detail'><c:param name='customer' value='${row.customerId}'/></c:url>"><c:out value="${row.name}"/></a><span class="inv-table__sub"><c:out value="${row.code}"/> · Sổ chi tiết</span></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.receivable}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.paid}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${row.outstanding}" maxFractionDigits="0"/></strong></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket0To30}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket31To60}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucket61To90}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${row.bucketOver90}" maxFractionDigits="0"/></td>
                                    <td class="is-number"><c:choose><c:when test="${row.overdue.signum() > 0}"><span class="invoice-pill invoice-pill--danger"><fmt:formatNumber value="${row.overdue}" maxFractionDigits="0"/></span></c:when><c:otherwise>0</c:otherwise></c:choose></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rows}">
                                <tr><td class="inv-table__empty" colspan="9">Không có công nợ để hiển thị.</td></tr>
                            </c:if>
                        </tbody>
                        <tfoot>
                            <tr id="debt-total">
                                <th scope="row">Tổng cộng</th>
                                <td class="is-number"><fmt:formatNumber value="${total.receivable}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.paid}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.outstanding}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.bucket0To30}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.bucket31To60}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.bucket61To90}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.bucketOver90}" maxFractionDigits="0"/></td>
                                <td class="is-number"><fmt:formatNumber value="${total.overdue}" maxFractionDigits="0"/></td>
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
