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
    <link rel="stylesheet" href="<c:url value='/assets/css/reports.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Báo cáo</h1>
                    <p class="inv-header__subtitle">Chọn báo cáo theo việc bạn cần theo dõi. Các báo cáo lọc được theo thời gian và có nút Xuất Excel để tải số liệu.</p>
                </div>
            </header>

            <div class="report-groups" id="report-hub">
                <c:if test="${canViewCompany}">
                    <section class="report-group" aria-labelledby="report-group-overview">
                        <h2 class="report-group__title" id="report-group-overview">Tổng quan toàn công ty</h2>
                        <div class="report-tiles">
                            <a class="report-tile" id="report-executive" href="<c:url value='/executive'/>">
                                <span class="report-tile__icon tone-navy"><span class="inv-icon inv-icon--layers" aria-hidden="true"></span></span>
                                <span class="report-tile__body">
                                    <span class="report-tile__title">Bảng điều hành</span>
                                    <span class="report-tile__desc">Xem nhanh trong một màn: doanh số hôm nay và tháng này, đơn chờ duyệt, đơn chờ xuất kho, công nợ quá hạn, hàng dưới tồn tối thiểu.</span>
                                </span>
                            </a>
                        </div>
                    </section>
                </c:if>

                <section class="report-group" aria-labelledby="report-group-sales">
                    <h2 class="report-group__title" id="report-group-sales">Bán hàng</h2>
                    <div class="report-tiles">
                        <a class="report-tile" id="report-sales" href="<c:url value='/reports/sales'/>">
                            <span class="report-tile__icon tone-blue"><span class="inv-icon inv-icon--sheet" aria-hidden="true"></span></span>
                            <span class="report-tile__body">
                                <span class="report-tile__title">Doanh số bán hàng</span>
                                <span class="report-tile__desc">Doanh số và sản lượng theo nhân viên, khu vực, đại lý, nhóm hàng hoặc từng mặt hàng; so sánh với kỳ trước.</span>
                            </span>
                        </a>
                        <c:if test="${currentUser.can('SALES_TARGET_MANAGE')}">
                            <a class="report-tile" id="report-targets" href="<c:url value='/targets'/>">
                                <span class="report-tile__icon tone-blue"><span class="inv-icon inv-icon--pencil" aria-hidden="true"></span></span>
                                <span class="report-tile__body">
                                    <span class="report-tile__title">Giao chỉ tiêu doanh số</span>
                                    <span class="report-tile__desc">Đặt chỉ tiêu tháng cho từng nhân viên và khu vực, theo dõi đã đạt bao nhiêu phần trăm.</span>
                                </span>
                            </a>
                        </c:if>
                        <a class="report-tile" id="report-my-target" href="<c:url value='/targets/my'/>">
                            <span class="report-tile__icon tone-blue"><span class="inv-icon inv-icon--user" aria-hidden="true"></span></span>
                            <span class="report-tile__body">
                                <span class="report-tile__title">Tiến độ chỉ tiêu của tôi</span>
                                <span class="report-tile__desc">Chỉ tiêu tháng này của bạn, doanh số đã đạt và phần còn thiếu.</span>
                            </span>
                        </a>
                    </div>
                </section>

                <c:if test="${currentUser.can('INVENTORY_VIEW')}">
                    <section class="report-group" aria-labelledby="report-group-stock">
                        <h2 class="report-group__title" id="report-group-stock">Kho hàng</h2>
                        <div class="report-tiles">
                            <a class="report-tile" id="report-stock" href="<c:url value='/reports/stock'/>">
                                <span class="report-tile__icon tone-green"><span class="inv-icon inv-icon--warehouse" aria-hidden="true"></span></span>
                                <span class="report-tile__body">
                                    <span class="report-tile__title">Giá trị tồn kho và hàng bán chậm</span>
                                    <span class="report-tile__desc">Giá trị hàng đang tồn theo kho và nhóm hàng; số ngày tồn bình quân, mặt hàng lâu bán.</span>
                                </span>
                            </a>
                        </div>
                    </section>
                </c:if>

                <c:if test="${currentUser.can('INVOICE_VIEW') and not currentUser.customer}">
                    <section class="report-group" aria-labelledby="report-group-debt">
                        <h2 class="report-group__title" id="report-group-debt">Công nợ</h2>
                        <div class="report-tiles">
                            <a class="report-tile" id="report-overdue" href="<c:url value='/debts/overdue'/>">
                                <span class="report-tile__icon tone-red"><span class="inv-icon inv-icon--triangle-alert" aria-hidden="true"></span></span>
                                <span class="report-tile__body">
                                    <span class="report-tile__title">Công nợ quá hạn</span>
                                    <span class="report-tile__desc">Khoản nợ đã quá hạn thanh toán, chia theo số ngày quá hạn và nhân viên phụ trách; đại lý có rủi ro cao nhất.</span>
                                </span>
                            </a>
                            <a class="report-tile" id="report-debts" href="<c:url value='/debts'/>">
                                <span class="report-tile__icon tone-amber"><span class="inv-icon inv-icon--rows" aria-hidden="true"></span></span>
                                <span class="report-tile__body">
                                    <span class="report-tile__title">Sổ công nợ đại lý</span>
                                    <span class="report-tile__desc">Từng đại lý phải thu, đã thu, còn nợ bao nhiêu và nợ đã bao lâu.</span>
                                </span>
                            </a>
                        </div>
                    </section>
                </c:if>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
