<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Chỉ tiêu của tôi"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chỉ tiêu của tôi | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chỉ tiêu của tôi · tháng <c:out value="${month}"/></h1>
                    <p class="inv-header__subtitle">Doanh số thực tế cập nhật mỗi lần mở trang, tính từ hoá đơn đã phát hành cho các đại lý bạn phụ trách.</p>
                </div>
            </header>
            <section class="inv-card" aria-labelledby="my-target-title">
                <h2 class="visually-hidden" id="my-target-title">Tiến độ</h2>
                <c:forEach var="row" items="${mine}">
                    <dl class="invoice-totals" id="my-target">
                        <div><dt>Chỉ tiêu</dt><dd id="my-target-amount"><c:choose><c:when test="${row.target.signum() == 0}">Chưa đặt</c:when><c:otherwise><fmt:formatNumber value="${row.target}" maxFractionDigits="0"/> ₫</c:otherwise></c:choose></dd></div>
                        <div><dt>Thực tế</dt><dd id="my-target-actual"><fmt:formatNumber value="${row.actual}" maxFractionDigits="0"/> ₫</dd></div>
                        <div class="invoice-totals__grand"><dt>Tiến độ</dt><dd id="my-target-percent"><c:choose><c:when test="${empty row.percent}">—</c:when><c:otherwise>${row.percent}% (kỳ đã qua ${row.expectedPercent}%)</c:otherwise></c:choose></dd></div>
                    </dl>
                </c:forEach>
                <c:if test="${empty mine}">
                    <p class="inv-table__empty" id="my-target-empty">Bạn không phải nhân viên kinh doanh nên không có chỉ tiêu riêng.</p>
                </c:if>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
