<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="pick-lists"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Phiếu soạn hàng"/>
<c:url var="listUrl" value="/pick-lists">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="date" value="${filter.deliveryDate}"/>
    <c:param name="status" value="${filter.status}"/>
    <c:param name="due" value="${filter.due}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<c:set var="pickPage" value="${overview.pickLists}"/>
<c:set var="selectable" value="${canManage and (not empty overview.pending or not empty overview.backorders)}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu soạn hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/pick-lists.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Danh sách phiếu soạn hàng</h1>
                    <p class="inv-header__subtitle">Quản lý phiếu soạn, theo dõi đơn đã duyệt chờ soạn và trạng thái soạn hàng theo kho, ngày giao.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="pick-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="pick-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <%-- Ô đánh dấu đơn ở hai bảng bên dưới gắn vào form này (thuộc tính form) --%>
            <c:if test="${canManage}">
                <form id="pick-create-form" action="<c:url value='/pick-lists/new'/>" method="post"></form>
            </c:if>

            <form class="inv-card pick-filters" id="pick-filter-form" action="<c:url value='/pick-lists'/>" method="get" role="search">
                <div class="pick-filters__top">
                    <div class="inv-field pick-filters__search">
                        <label class="pick-filters__label" for="pick-keyword">Tìm kiếm</label>
                        <span class="inv-search">
                            <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                            <input class="inv-control" type="search" id="pick-keyword" name="keyword" maxlength="100"
                                   value="<c:out value='${filter.keyword}'/>" placeholder="Mã phiếu, mã đơn hàng, kho..." autocomplete="off">
                        </span>
                    </div>
                    <button class="inv-button pick-filters__apply" type="submit" id="pick-filter-apply">
                        <span class="inv-icon inv-icon--funnel" aria-hidden="true"></span>Bộ lọc
                    </button>
                    <c:if test="${canManage}">
                        <button class="inv-button inv-button--primary pick-filters__create" type="submit" form="pick-create-form"
                                id="pick-create" disabled data-selection-required>
                            <span class="inv-icon inv-icon--plus" aria-hidden="true"></span>Tạo phiếu soạn
                        </button>
                    </c:if>
                </div>
                <div class="pick-filters__grid">
                    <div class="inv-field">
                        <label class="pick-filters__label" for="pick-warehouse">Kho</label>
                        <select class="inv-control" id="pick-warehouse" name="warehouse" data-auto-submit>
                            <option value="">Tất cả</option>
                            <c:forEach var="warehouse" items="${warehouses}">
                                <option value="${warehouse.id}"${warehouse.id == filter.warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="pick-filters__label" for="pick-date">Ngày giao</label>
                        <input class="inv-control" type="date" id="pick-date" name="date" value="${filter.deliveryDate}" data-auto-submit>
                    </div>
                    <div class="inv-field">
                        <label class="pick-filters__label" for="pick-status">Trạng thái</label>
                        <select class="inv-control" id="pick-status" name="status" data-auto-submit>
                            <option value="">Tất cả</option>
                            <c:forEach var="status" items="${statusLabels}">
                                <option value="${status.key}"${status.key == filter.status ? ' selected' : ''}>${status.value}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="pick-filters__label" for="pick-due">Đơn chờ soạn</label>
                        <select class="inv-control" id="pick-due" name="due" data-auto-submit>
                            <option value="">Tất cả</option>
                            <c:forEach var="due" items="${dueLabels}">
                                <option value="${due.key}"${due.key == filter.due ? ' selected' : ''}>${due.value}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <c:if test="${filter.filtered}">
                    <a class="pick-filters__clear" id="pick-filter-clear" href="<c:url value='/pick-lists'/>">Xoá bộ lọc</a>
                </c:if>
            </form>

            <section class="inv-card pick-lists" aria-label="Đơn chờ soạn và phiếu soạn">
                <div class="pick-section" id="pending-section">
                    <div class="pick-section__head">
                        <div>
                            <h2 class="pick-section__title">Danh sách đơn đã duyệt chờ soạn</h2>
                            <p class="pick-section__count" id="pending-count">${fn:length(overview.pending)} đơn chờ soạn</p>
                        </div>
                        <c:if test="${selectable}">
                            <p class="pick-section__hint" id="pick-selection" aria-live="polite">Đánh dấu các đơn cùng kho rồi bấm "Tạo phiếu soạn" để gộp một lượt soạn.</p>
                        </c:if>
                    </div>
                    <div class="inv-table-scroll">
                        <table class="inv-table pick-table" id="pending-table">
                            <thead>
                                <tr>
                                    <c:if test="${canManage}">
                                        <th scope="col" class="pick-table__check">
                                            <c:if test="${not empty overview.pending}">
                                                <input class="inv-checkbox" type="checkbox" id="pending-all" aria-label="Chọn tất cả đơn chờ soạn" data-check-all="pending">
                                            </c:if>
                                        </th>
                                    </c:if>
                                    <th scope="col">Mã đơn</th>
                                    <th scope="col">Khách hàng</th>
                                    <th scope="col">Kho</th>
                                    <th scope="col">Ngày giao</th>
                                    <th scope="col">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="order" items="${overview.pending}">
                                    <c:url var="orderUrl" value="/orders/view"><c:param name="id" value="${order.orderId}"/></c:url>
                                    <tr>
                                        <c:if test="${canManage}">
                                            <td class="pick-table__check">
                                                <input class="inv-checkbox" type="checkbox" name="orderId" value="${order.orderId}" form="pick-create-form"
                                                       data-group="pending" data-warehouse="${order.warehouseId}" data-warehouse-name="<c:out value='${order.warehouseName}'/>"
                                                       aria-label="Chọn đơn <c:out value='${order.orderNo}'/>">
                                            </td>
                                        </c:if>
                                        <td><a class="inv-table__code" href="${fn:escapeXml(orderUrl)}"><c:out value="${order.orderNo}"/></a></td>
                                        <td><span class="inv-table__main"><c:out value="${order.customerName}"/></span></td>
                                        <td><c:out value="${order.warehouseName}"/></td>
                                        <td>
                                            <span class="${order.overdue ? 'pick-table__late' : ''}">${order.deliveryDateText}</span>
                                            <c:if test="${order.overdue}"><span class="inv-table__sub pick-table__late">Quá ngày giao</span></c:if>
                                        </td>
                                        <td><span class="inv-pill pick-pill pick-pill--open">Chờ soạn</span></td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty overview.pending}">
                                    <tr>
                                        <td class="inv-table__empty" colspan="${canManage ? 6 : 5}">
                                            <c:choose>
                                                <c:when test="${filter.filtered}">Không có đơn chờ soạn phù hợp bộ lọc.</c:when>
                                                <c:otherwise>Không có đơn đã duyệt nào đang chờ soạn.</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="pick-section" id="picklist-section">
                    <div class="pick-section__head">
                        <div>
                            <h2 class="pick-section__title">Danh sách phiếu soạn</h2>
                            <p class="pick-section__count" id="picklist-count">${pickPage.totalItems} phiếu soạn</p>
                        </div>
                    </div>
                    <div class="inv-table-scroll">
                        <table class="inv-table pick-table" id="picklist-table">
                            <thead>
                                <tr>
                                    <th scope="col">Mã phiếu</th>
                                    <th scope="col">Đơn hàng</th>
                                    <th scope="col">Kho</th>
                                    <th scope="col">Ngày giao</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col" class="pick-table__action"><span class="visually-hidden">Thao tác</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="pick" items="${pickPage.items}">
                                    <c:url var="viewUrl" value="/pick-lists/view"><c:param name="id" value="${pick.id}"/></c:url>
                                    <tr>
                                        <td>
                                            <a class="inv-table__code pick-table__code" href="${fn:escapeXml(viewUrl)}"><c:out value="${pick.pickNo}"/></a>
                                            <span class="inv-table__sub">Lập ${pick.createdAtText}</span>
                                        </td>
                                        <td><span class="pick-table__orders"><c:out value="${pick.orderNos}"/></span></td>
                                        <td><c:out value="${pick.warehouseName}"/></td>
                                        <td>${pick.deliveryDateText}</td>
                                        <td>
                                            <span class="inv-pill pick-pill pick-pill--${pick.tone}">${pick.statusLabel}</span>
                                            <c:if test="${pick.awaitingIssue}"><span class="inv-table__sub">Chờ xác nhận xuất kho</span></c:if>
                                        </td>
                                        <td class="pick-table__action">
                                            <a class="inv-icon-button" href="${fn:escapeXml(viewUrl)}" title="Mở phiếu soạn"
                                               aria-label="Mở phiếu soạn <c:out value='${pick.pickNo}'/>">
                                                <span class="inv-icon inv-icon--eye" aria-hidden="true"></span>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty pickPage.items}">
                                    <tr>
                                        <td class="inv-table__empty" colspan="6">
                                            <c:choose>
                                                <c:when test="${filter.filtered}">Không tìm thấy phiếu soạn phù hợp. Thử bỏ bớt điều kiện lọc.</c:when>
                                                <c:otherwise>Chưa có phiếu soạn nào.</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="inv-footer">
                    <p class="inv-footer__info" id="picklist-page-info">
                        <c:choose>
                            <c:when test="${pickPage.totalItems == 0}">Không có phiếu soạn nào</c:when>
                            <c:otherwise>Hiển thị ${pickPage.firstRowNumber}–${pickPage.firstRowNumber + fn:length(pickPage.items) - 1}
                                trong tổng số ${pickPage.totalItems} phiếu</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="inv-pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${pickPage.page > 1}">
                                        <a class="inv-pagination__item" aria-label="Trang trước" href="${pageUrl}${pickPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${pickPage.startPage}" end="${pickPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == pickPage.page}">
                                            <span class="inv-pagination__item inv-pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="inv-pagination__item" aria-label="Trang ${pageNumber}" href="${pageUrl}${pageNumber}">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${pickPage.page < pickPage.totalPages}">
                                        <a class="inv-pagination__item" aria-label="Trang sau" href="${pageUrl}${pickPage.page + 1}">›</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>

            <%-- Đơn còn nợ hàng (xuất thiếu, chọn giao bổ sung sau - S6-05) hiện riêng, chờ soạn bổ sung --%>
            <section class="inv-card pick-lists" aria-labelledby="backorder-title">
                <div class="pick-section" id="backorder-section">
                    <div class="pick-section__head">
                        <div>
                            <h2 class="pick-section__title" id="backorder-title">Đơn còn nợ hàng</h2>
                            <p class="pick-section__count" id="backorder-count">${fn:length(overview.backorders)} đơn chờ giao bổ sung</p>
                        </div>
                    </div>
                    <div class="inv-table-scroll">
                        <table class="inv-table pick-table" id="backorder-table">
                            <thead>
                                <tr>
                                    <c:if test="${canManage}">
                                        <th scope="col" class="pick-table__check"><span class="visually-hidden">Chọn</span></th>
                                    </c:if>
                                    <th scope="col">Mã đơn</th>
                                    <th scope="col">Khách hàng</th>
                                    <th scope="col">Kho</th>
                                    <th scope="col" class="is-number">Còn nợ</th>
                                    <th scope="col">Lịch giao bổ sung</th>
                                    <th scope="col">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="order" items="${overview.backorders}">
                                    <c:url var="orderUrl" value="/orders/view"><c:param name="id" value="${order.orderId}"/></c:url>
                                    <tr>
                                        <c:if test="${canManage}">
                                            <td class="pick-table__check">
                                                <c:if test="${order.selectable}">
                                                    <input class="inv-checkbox" type="checkbox" name="orderId" value="${order.orderId}" form="pick-create-form"
                                                           data-group="backorder" data-warehouse="${order.warehouseId}" data-warehouse-name="<c:out value='${order.warehouseName}'/>"
                                                           aria-label="Chọn đơn nợ hàng <c:out value='${order.orderNo}'/>">
                                                </c:if>
                                            </td>
                                        </c:if>
                                        <td>
                                            <a class="inv-table__code" href="${fn:escapeXml(orderUrl)}"><c:out value="${order.orderNo}"/></a>
                                            <c:if test="${not empty order.deliveryNo}"><span class="inv-table__sub">Xuất thiếu ở <c:out value="${order.deliveryNo}"/></span></c:if>
                                        </td>
                                        <td><span class="inv-table__main"><c:out value="${order.customerName}"/></span></td>
                                        <td><c:out value="${order.warehouseName}"/></td>
                                        <td class="is-number pick-table__qty"><fmt:formatNumber value="${order.remainingQty}" maxFractionDigits="3"/></td>
                                        <td>${order.backorderDueText}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${order.selectable}"><span class="inv-pill pick-pill pick-pill--backorder">Chờ soạn bổ sung</span></c:when>
                                                <c:otherwise>
                                                    <span class="inv-pill pick-pill pick-pill--picking">Đang soạn bổ sung</span>
                                                    <span class="inv-table__sub"><c:out value="${order.openPickNo}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty overview.backorders}">
                                    <tr>
                                        <td class="inv-table__empty" colspan="${canManage ? 7 : 6}">Không có đơn nào còn nợ hàng.</td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/pick-lists.js'/>"></script>
</body>
</html>
