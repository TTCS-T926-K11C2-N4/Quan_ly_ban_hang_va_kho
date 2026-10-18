<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="sales"/>
<c:set var="activeSubmenu" value="order-list"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Danh sách đơn hàng"/>
<%-- Link giữ nguyên điều kiện lọc hiện tại, dùng cho phân trang --%>
<c:url var="listUrl" value="/orders">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="status" value="${filter.status}"/>
    <c:param name="customer" value="${filter.customerId}"/>
    <c:param name="salesRep" value="${filter.salesRepId}"/>
    <c:param name="region" value="${filter.regionId}"/>
    <c:param name="from" value="${filter.dateFrom}"/>
    <c:param name="to" value="${filter.dateTo}"/>
    <c:param name="sort" value="${filter.sort}"/>
    <c:param name="size" value="${filter.pageSize}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách đơn hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page order-list">
            <header class="page-header customer-list-header">
                <div>
                    <h1 class="page-header__title">Danh sách đơn hàng</h1>
                    <p class="page-header__subtitle">Tìm đơn theo mã đơn, đại lý và lọc theo trạng thái, nhân viên, khu vực, khoảng ngày tạo.</p>
                </div>
                <c:if test="${canCreate}">
                    <a class="account-filters__create" id="create-order-link" href="<c:url value='/orders/new'/>">
                        <span aria-hidden="true">+</span> Tạo đơn hàng
                    </a>
                </c:if>
            </header>

            <form class="customer-filters${filter.activeFilterCount > 0 ? ' customer-filters--open' : ''}" id="order-filter-form"
                  action="<c:url value='/orders'/>" method="get" role="search">
                <div class="customer-filters__search">
                    <div class="customer-filters__field customer-filters__field--keyword">
                        <label class="customer-filters__label" for="order-keyword">Tìm kiếm</label>
                        <div class="customer-filters__search-box">
                            <img class="customer-filters__search-icon" src="<c:url value='/assets/img/icons/search.svg'/>" alt="" width="18" height="18">
                            <input class="account-filters__control customer-filters__keyword" type="search" id="order-keyword"
                                   name="keyword" value="<c:out value='${filter.keyword}'/>" maxlength="100"
                                   placeholder="Mã đơn, tên đại lý, mã đại lý..." autocomplete="off">
                        </div>
                    </div>
                    <div class="customer-filters__actions">
                        <button class="customer-filters__toggle" type="button" id="order-filter-toggle"
                                aria-controls="order-filter-fields" aria-expanded="${filter.activeFilterCount > 0}">
                            <span class="customer-filters__icon" aria-hidden="true"></span>Bộ lọc
                            <c:if test="${filter.activeFilterCount > 0}"><span class="customer-filters__count">${filter.activeFilterCount}</span></c:if>
                        </button>
                        <button class="customer-filters__submit" type="submit" id="order-filter-submit">Tìm kiếm</button>
                    </div>
                </div>

                <div class="customer-filters__fields order-filters__fields${canViewAll ? ' order-filters__fields--rep' : ''}" id="order-filter-fields">
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="order-status">Trạng thái</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="order-status" name="status">
                                <option value="">Tất cả (trừ nháp)</option>
                                <c:forEach var="code" items="${statusCodes}">
                                    <option value="${code}"${code == filter.status ? ' selected' : ''}>${fn:escapeXml(statusLabels[code])}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="order-customer">Đại lý</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="order-customer" name="customer">
                                <option value="">Tất cả</option>
                                <c:forEach var="customer" items="${customers}">
                                    <option value="${customer.id}"${customer.id == filter.customerId ? ' selected' : ''}><c:out value="${customer.name}"/> (<c:out value="${customer.code}"/>)</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <c:if test="${canViewAll}">
                        <div class="customer-filters__field">
                            <label class="customer-filters__label" for="order-sales-rep">Nhân viên kinh doanh</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="order-sales-rep" name="salesRep">
                                    <option value="">Tất cả</option>
                                    <c:forEach var="rep" items="${salesReps}">
                                        <option value="${rep.id}"${rep.id == filter.salesRepId ? ' selected' : ''}><c:out value="${rep.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </c:if>
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="order-region">Khu vực</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="order-region" name="region">
                                <option value="">Tất cả</option>
                                <c:forEach var="region" items="${regions}">
                                    <option value="${region.id}"${region.id == filter.regionId ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="customer-filters__field order-filters__dates">
                        <span class="customer-filters__label" id="order-date-label">Khoảng ngày tạo</span>
                        <div class="order-filters__date-row" role="group" aria-labelledby="order-date-label">
                            <input class="account-filters__control" type="date" id="order-from" name="from" value="${filter.dateFrom}" aria-label="Từ ngày">
                            <span aria-hidden="true">–</span>
                            <input class="account-filters__control" type="date" id="order-to" name="to" value="${filter.dateTo}" aria-label="Đến ngày">
                        </div>
                    </div>
                    <c:if test="${filter.filtered}">
                        <a class="customer-filters__clear" id="order-filter-clear" href="<c:url value='/orders'/>">Xoá bộ lọc</a>
                    </c:if>
                </div>
            </form>

            <section class="account-table-card" aria-label="Danh sách đơn hàng">
                <div class="order-list__bar">
                    <p class="order-list__summary" id="order-summary" aria-live="polite">
                        <strong>${rows.totalItems}</strong> đơn
                        <span class="order-list__sep" aria-hidden="true">·</span>
                        Tổng tiền <strong class="order-list__total"><fmt:formatNumber value="${totals.amount}" maxFractionDigits="0"/> ₫</strong>
                        <c:if test="${totals.excludedOrders > 0}">
                            <span class="order-list__note">(không tính ${totals.excludedOrders} đơn đã huỷ / bị từ chối)</span>
                        </c:if>
                    </p>
                    <div class="order-list__controls">
                        <label class="order-list__control">
                            <span>Sắp xếp</span>
                            <span class="select"><select class="account-filters__control select__control" name="sort" form="order-filter-form" data-autosubmit>
                                <c:forEach var="sort" items="${sorts}">
                                    <option value="${sort.key}"${sort.key == filter.sort ? ' selected' : ''}>${sort.value}</option>
                                </c:forEach>
                            </select></span>
                        </label>
                        <label class="order-list__control">
                            <span>Hiển thị</span>
                            <span class="select"><select class="account-filters__control select__control" name="size" form="order-filter-form" data-autosubmit>
                                <c:forEach var="size" items="${pageSizes}">
                                    <option value="${size}"${size == filter.pageSize ? ' selected' : ''}>${size} / trang</option>
                                </c:forEach>
                            </select></span>
                        </label>
                    </div>
                </div>

                <div class="account-table-scroll">
                    <table class="account-table customer-table order-list-table" id="order-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã đơn</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Ngày giao</th>
                                <th scope="col" class="order-list-table__money">Tổng tiền</th>
                                <th scope="col">Nhân viên phụ trách</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col"><span class="visually-hidden">Thao tác</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="order" items="${rows.items}">
                                <c:url var="detailUrl" value="/orders/view"><c:param name="id" value="${order.id}"/></c:url>
                                <tr>
                                    <td data-label="Mã đơn">
                                        <a class="customer-table__link" href="${fn:escapeXml(detailUrl)}"><c:out value="${order.orderNo}"/></a>
                                        <span class="order-list-table__sub"><c:out value="${order.createdAtText}"/></span>
                                    </td>
                                    <td data-label="Đại lý">
                                        <span class="customer-table__name"><c:out value="${order.customerName}"/></span>
                                        <span class="order-list-table__sub"><c:out value="${order.customerCode}"/> · <c:out value="${order.regionName}"/></span>
                                    </td>
                                    <td data-label="Ngày giao"><c:out value="${empty order.requestedDateText ? '—' : order.requestedDateText}"/></td>
                                    <td data-label="Tổng tiền" class="order-list-table__money"><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</td>
                                    <td data-label="Nhân viên phụ trách">
                                        <c:choose>
                                            <c:when test="${empty order.salesRepName}"><span class="customer-table__none">Chưa phân công</span></c:when>
                                            <c:otherwise><c:out value="${order.salesRepName}"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td data-label="Trạng thái">
                                        <span class="order-status order-status--${order.statusTone}">${order.statusLabel}</span>
                                        <c:if test="${order.needsApproval}"><span class="order-list-table__sub order-list-table__sub--warn">Cần duyệt</span></c:if>
                                    </td>
                                    <td class="order-list-table__action">
                                        <a class="order-list-table__open" href="${fn:escapeXml(detailUrl)}">Xem chi tiết</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rows.items}">
                                <tr class="customer-table__empty-row">
                                    <td class="customer-table__empty" colspan="7">
                                        <c:choose>
                                            <c:when test="${not filter.filtered}">Chưa có đơn hàng nào trong phạm vi bạn được xem.</c:when>
                                            <c:otherwise>Không tìm thấy đơn hàng phù hợp. Thử bỏ bớt điều kiện lọc hoặc đổi từ khoá tìm kiếm.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info" id="order-count">
                        <c:choose>
                            <c:when test="${rows.totalItems == 0}">Không có đơn nào</c:when>
                            <c:otherwise>
                                Hiển thị ${rows.firstRowNumber} - ${rows.firstRowNumber + fn:length(rows.items) - 1}
                                trong tổng ${rows.totalItems} đơn
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${rows.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="${pageUrl}${rows.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${rows.startPage}" end="${rows.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == rows.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="${pageUrl}${pageNumber}">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${rows.page < rows.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="${pageUrl}${rows.page + 1}">›</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/order-list.js'/>"></script>
</body>
</html>
