<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activeMenu" value="dashboard"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Bảng điều khiển"/>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tổng quan hệ thống | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/dashboard.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content">
            <header class="page-header">
                <h1 class="page-header__title">Tổng quan hệ thống</h1>
                <p class="page-header__subtitle">Theo dõi nhanh hoạt động bán hàng và kho hôm nay.</p>
            </header>

            <section class="summary-grid" aria-label="Chỉ số hôm nay">
                <%-- Chưa lưu số liệu theo ngày nên chưa so được với hôm qua: hiện "—" thay vì số tự đặt --%>
                <article class="summary-card summary-card--blue">
                    <div class="summary-card__head">
                        <span class="summary-card__icon summary-card__icon--orders" aria-hidden="true"></span>
                        <p class="summary-card__label">Đơn hàng</p>
                        <span class="summary-card__tag summary-card__tag--today">Hôm nay</span>
                    </div>
                    <p class="summary-card__value"><fmt:formatNumber value="${summary.todayOrderCount}"/></p>
                    <c:if test="${not summary.hasSalesData}"><p class="summary-card__note">Chưa có đơn hàng</p></c:if>
                    <div class="summary-card__compare">
                        <span class="summary-card__trend" aria-hidden="true"></span>
                        <div>
                            <p class="summary-card__compare-label">So với hôm qua</p>
                            <p class="summary-card__compare-value">
                                <c:choose>
                                    <c:when test="${summary.hasSalesData}"><fmt:formatNumber value="${summary.orderChangePercent}" pattern="+#,##0.0;-#,##0.0"/>%</c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                    </div>
                    <img class="summary-card__art" src="<c:url value='/assets/img/illustrations/dashboard/orders.svg'/>" alt="" width="120" height="100">
                </article>

                <article class="summary-card summary-card--green">
                    <div class="summary-card__head">
                        <span class="summary-card__icon summary-card__icon--revenue" aria-hidden="true"></span>
                        <p class="summary-card__label">Doanh thu hôm nay</p>
                        <span class="summary-card__tag summary-card__tag--today">Hôm nay</span>
                    </div>
                    <p class="summary-card__value"><fmt:formatNumber value="${summary.todayRevenue}" maxFractionDigits="0"/> ₫</p>
                    <c:if test="${not summary.hasSalesData}"><p class="summary-card__note">Chưa có dữ liệu bán hàng</p></c:if>
                    <div class="summary-card__compare">
                        <span class="summary-card__trend" aria-hidden="true"></span>
                        <div>
                            <p class="summary-card__compare-label">So với hôm qua</p>
                            <p class="summary-card__compare-value">
                                <c:choose>
                                    <c:when test="${summary.hasSalesData}"><fmt:formatNumber value="${summary.revenueChangePercent}" pattern="+#,##0.0;-#,##0.0"/>%</c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </p>
                        </div>
                    </div>
                    <img class="summary-card__art" src="<c:url value='/assets/img/illustrations/dashboard/revenue.svg'/>" alt="" width="120" height="100">
                </article>

                <article class="summary-card summary-card--orange">
                    <div class="summary-card__head">
                        <span class="summary-card__icon summary-card__icon--low-stock" aria-hidden="true"></span>
                        <p class="summary-card__label">Sản phẩm sắp hết / hết hàng</p>
                        <c:if test="${summary.lowStockProductCount > 0}">
                            <span class="summary-card__tag summary-card__tag--alert">Cần xử lý</span>
                        </c:if>
                    </div>
                    <p class="summary-card__value"><fmt:formatNumber value="${summary.lowStockProductCount}"/></p>
                    <p class="summary-card__note">${summary.lowStockProductCount > 0 ? 'Sản phẩm cần xử lý' : 'Không có sản phẩm cần xử lý'}</p>
                    <div class="summary-card__compare">
                        <span class="summary-card__trend" aria-hidden="true"></span>
                        <div>
                            <p class="summary-card__compare-label">So với hôm qua</p>
                            <p class="summary-card__compare-value">—</p>
                        </div>
                    </div>
                    <img class="summary-card__art" src="<c:url value='/assets/img/illustrations/dashboard/low-stock.svg'/>" alt="" width="120" height="100">
                </article>
            </section>

            <div class="dashboard-grid">
                <section class="panel panel--orders" aria-labelledby="recent-orders-title">
                    <header class="panel__header">
                        <h2 class="panel__title" id="recent-orders-title">Đơn hàng gần đây</h2>
                        <a class="panel__link" id="view-all-orders-link" href="<c:url value='/orders'/>">Xem tất cả</a>
                    </header>
                    <div class="table-scroll">
                        <table class="data-table data-table--recent-orders">
                            <thead>
                                <tr>
                                    <th scope="col">Mã đơn</th>
                                    <th scope="col">Khách hàng</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col">Tổng tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="order" items="${recentOrders}">
                                    <tr>
                                        <td class="data-table__code"><c:out value="${order.code}"/></td>
                                        <td><c:out value="${order.customerName}"/></td>
                                        <td><c:out value="${order.statusLabel}"/></td>
                                        <td><fmt:formatNumber value="${order.totalAmount}" maxFractionDigits="0"/> ₫</td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty recentOrders}">
                                    <tr>
                                        <td class="data-table__empty" colspan="4">Chưa có đơn hàng nào. Đơn hàng sẽ hiện ở đây khi có chức năng tạo đơn.</td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </section>

                <div class="dashboard-grid__side">
                    <%-- S1-06: chỉ hiện thao tác người dùng có quyền làm --%>
                    <c:set var="canCreateOrder" value="${currentUser.can('ORDER_MANAGE')}"/>
                    <c:set var="canStockIn" value="${currentUser.can('INVENTORY_MANAGE')}"/>
                    <c:set var="canCreateAccount" value="${currentUser.can('USER_MANAGE')}"/>
                    <c:if test="${canCreateOrder or canStockIn or canCreateAccount}">
                    <section class="panel panel--compact" aria-labelledby="quick-actions-title">
                        <h2 class="panel__title" id="quick-actions-title">Thao tác nhanh</h2>
                        <ul class="quick-actions">
                            <c:if test="${canCreateOrder}">
                            <li>
                                <a class="quick-action tone--light-blue" id="quick-create-order" href="<c:url value='/orders/new'/>">
                                    Tạo đơn hàng<span class="quick-action__arrow" aria-hidden="true">→</span>
                                </a>
                            </li>
                            </c:if>
                            <c:if test="${canStockIn}">
                            <li>
                                <a class="quick-action tone--light-green" id="quick-stock-in" href="<c:url value='/warehouse/receipts/new'/>">
                                    Nhập kho<span class="quick-action__arrow" aria-hidden="true">→</span>
                                </a>
                            </li>
                            </c:if>
                            <c:if test="${canCreateAccount}">
                            <li>
                                <a class="quick-action tone--light-violet" id="quick-create-account" href="<c:url value='/accounts/new'/>">
                                    Thêm tài khoản<span class="quick-action__arrow" aria-hidden="true">→</span>
                                </a>
                            </li>
                            </c:if>
                        </ul>
                    </section>
                    </c:if>

                    <section class="panel panel--compact" aria-labelledby="access-scope-title">
                        <h2 class="panel__title" id="access-scope-title">Phạm vi truy cập</h2>
                        <div class="access-scope">
                            <img class="access-scope__icon" src="<c:url value='/assets/img/icons/shield.svg'/>" alt="" width="24" height="24">
                            <div>
                                <p class="access-scope__role">Vai trò: <c:out value="${currentUser.roleName}"/></p>
                                <p class="access-scope__area">Kho / địa bàn: <c:out value="${empty currentUser.scope ? 'Chưa gắn kho hoặc địa bàn' : currentUser.scope}"/></p>
                            </div>
                        </div>
                        <p class="access-scope__note">
                            Menu được hiển thị theo đúng quyền của tài khoản.<br>
                            Các chức năng không có quyền sẽ được ẩn hoàn toàn.
                        </p>
                    </section>
                </div>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
