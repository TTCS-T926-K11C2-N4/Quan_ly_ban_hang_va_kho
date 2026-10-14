<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="approvals"/>
<c:set var="breadcrumbSection" value="Duyệt đơn"/>
<c:set var="breadcrumbPage" value="Đơn chờ duyệt"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Duyệt đơn hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/approvals.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page approvals">
            <header class="page-header">
                <h1 class="page-header__title">Duyệt đơn hàng</h1>
                <p class="page-header__subtitle">Đơn vượt hạn mức công nợ hoặc bán dưới giá sàn chờ Quản lý kinh doanh duyệt. Đơn không vi phạm được hệ thống duyệt ngay khi gửi.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="approval-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="sales-flash sales-flash--error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="sales-card approval-queue" aria-labelledby="approval-queue-title">
                <div class="approval-queue__head">
                    <div>
                        <h2 class="order-card__title" id="approval-queue-title">Đơn chờ duyệt</h2>
                        <p class="approval-queue__count"><strong>${rows.size()}</strong> đơn<c:if test="${not empty keyword}"> khớp "<c:out value="${keyword}"/>"</c:if> · cũ nhất trước</p>
                    </div>
                    <form class="approval-search" action="<c:url value='/approvals'/>" method="get" role="search">
                        <label class="visually-hidden" for="approval-keyword">Tìm đơn chờ duyệt</label>
                        <img class="approval-search__icon" src="<c:url value='/assets/img/icons/search.svg'/>" alt="" width="16" height="16">
                        <input class="sales-field__control approval-search__input" type="search" id="approval-keyword" name="keyword"
                               value="<c:out value='${keyword}'/>" maxlength="100" placeholder="Mã đơn, tên hoặc mã đại lý" autocomplete="off">
                        <button class="sales-button sales-button--primary sales-button--small" type="submit">Tìm</button>
                    </form>
                </div>

                <c:choose>
                    <c:when test="${empty rows}">
                        <div class="approval-empty">
                            <img src="<c:url value='/assets/img/icons/check-box.svg'/>" alt="" width="40" height="40">
                            <p class="approval-empty__title">${empty keyword ? 'Không có đơn nào chờ duyệt' : 'Không có đơn chờ duyệt khớp tìm kiếm'}</p>
                            <p class="approval-empty__text">${empty keyword ? 'Đơn vượt hạn mức hoặc dưới giá sàn sẽ hiện ở đây ngay khi nhân viên gửi.' : 'Thử tìm theo mã đơn hoặc tên đại lý khác.'}</p>
                            <c:if test="${not empty keyword}">
                                <a class="sales-button" href="<c:url value='/approvals'/>">Xem tất cả</a>
                            </c:if>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="order-items-scroll">
                            <table class="order-items approval-table">
                                <thead>
                                    <tr>
                                        <th scope="col">Mã đơn</th>
                                        <th scope="col">Đại lý</th>
                                        <th scope="col">Lý do cần duyệt</th>
                                        <th scope="col" class="order-items__money">Tổng tiền</th>
                                        <th scope="col">Gửi lúc</th>
                                        <th scope="col"><span class="visually-hidden">Thao tác</span></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="row" items="${rows}">
                                        <tr>
                                            <td data-label="Mã đơn">
                                                <span class="order-items__name"><c:out value="${row.orderNo}"/></span>
                                                <span class="order-items__sku"><c:out value="${empty row.salesRepName ? 'Chưa phân công' : row.salesRepName}"/></span>
                                            </td>
                                            <td data-label="Đại lý">
                                                <span class="order-items__name"><c:out value="${row.customerName}"/></span>
                                                <span class="order-items__sku"><c:out value="${row.customerCode}"/></span>
                                            </td>
                                            <td data-label="Lý do cần duyệt">
                                                <span class="approval-reasons">
                                                    <c:if test="${row.overCreditLimit}">
                                                        <span class="approval-reason approval-reason--credit">Vượt hạn mức <fmt:formatNumber value="${row.exceededBy}" maxFractionDigits="0"/> ₫</span>
                                                    </c:if>
                                                    <c:if test="${row.belowFloorLines > 0}">
                                                        <span class="approval-reason approval-reason--floor">${row.belowFloorLines} dòng dưới giá sàn</span>
                                                    </c:if>
                                                </span>
                                            </td>
                                            <td data-label="Tổng tiền" class="order-items__money order-items__total"><fmt:formatNumber value="${row.total}" maxFractionDigits="0"/> ₫</td>
                                            <td data-label="Gửi lúc"><c:out value="${row.requestedAtText}"/></td>
                                            <td class="approval-table__action">
                                                <a class="sales-button sales-button--primary sales-button--small"
                                                   href="<c:url value='/approvals/view'><c:param name='id' value='${row.orderId}'/></c:url>">${currentUser.can('ORDER_APPROVE') ? 'Xem &amp; duyệt' : 'Xem'}</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
