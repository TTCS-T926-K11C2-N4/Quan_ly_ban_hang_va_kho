<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="reports"/>
<c:set var="breadcrumbSection" value="Báo cáo"/>
<c:set var="breadcrumbPage" value="Tổng hợp báo cáo"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Báo cáo | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Báo cáo</h1>
                    <p class="inv-header__subtitle">Chọn báo cáo cần xem. Mỗi báo cáo xuất được ra Excel.</p>
                </div>
            </header>
            <section class="inv-card" aria-label="Danh sách báo cáo">
                <ul class="report-hub" id="report-hub">
                    <li><a class="inv-table__main" id="report-sales" href="<c:url value='/reports/sales'/>">Doanh số nhiều chiều</a>
                        <span class="inv-table__sub">Theo nhân viên, khu vực, đại lý, nhóm hàng, SKU; so với kỳ trước; biên lợi nhuận cho người xem được giá vốn.</span></li>
                    <c:if test="${currentUser.can('INVENTORY_VIEW')}">
                        <li><a class="inv-table__main" id="report-stock" href="<c:url value='/reports/stock'/>">Tồn kho và vòng quay hàng</a>
                            <span class="inv-table__sub">Giá trị tồn theo kho, nhóm hàng; số ngày tồn và hàng chậm luân chuyển.</span></li>
                    </c:if>
                    <c:if test="${currentUser.can('INVOICE_VIEW') and not currentUser.customer}">
                        <li><a class="inv-table__main" id="report-overdue" href="<c:url value='/debts/overdue'/>">Công nợ quá hạn theo tuổi nợ</a>
                            <span class="inv-table__sub">Tổng hợp theo nhóm tuổi nợ, nhân viên phụ trách và mức rủi ro.</span></li>
                        <li><a class="inv-table__main" id="report-debts" href="<c:url value='/debts'/>">Sổ công nợ theo đại lý</a>
                            <span class="inv-table__sub">Phải thu, đã thu, còn nợ và phân tích tuổi nợ.</span></li>
                    </c:if>
                </ul>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
