<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Quản lý đại lý"/>
<%-- Link giữ nguyên điều kiện lọc hiện tại, dùng cho phân trang --%>
<c:url var="listUrl" value="/customers">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="region" value="${filter.regionId}"/>
    <c:param name="group" value="${filter.customerGroupId}"/>
    <c:param name="salesRep" value="${filter.salesRepId}"/>
    <c:param name="status" value="${filter.status.code}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<%-- Số ô lọc đang chọn (không tính ô tìm kiếm), hiện cạnh nút Bộ lọc trên điện thoại --%>
<c:set var="activeFilterCount" value="${(empty filter.regionId ? 0 : 1) + (empty filter.customerGroupId ? 0 : 1)
        + (empty filter.salesRepId ? 0 : 1) + (empty filter.status ? 0 : 1)}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý đại lý | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Quản lý đại lý</h1>
                <p class="page-header__subtitle">Tìm đại lý theo mã, tên, số điện thoại và lọc theo khu vực, nhóm khách hàng,
                    người phụ trách, trạng thái.</p>
            </header>

            <form class="customer-filters${activeFilterCount > 0 ? ' customer-filters--open' : ''}" id="customer-filter-form"
                  action="<c:url value='/customers'/>" method="get" role="search">
                <div class="customer-filters__search">
                    <div class="customer-filters__field customer-filters__field--keyword">
                        <label class="customer-filters__label" for="customer-keyword">Tìm kiếm</label>
                        <div class="customer-filters__search-box">
                            <img class="customer-filters__search-icon" src="<c:url value='/assets/img/icons/search.svg'/>" alt="" width="18" height="18">
                            <input class="account-filters__control customer-filters__keyword" type="search" id="customer-keyword"
                                   name="keyword" value="<c:out value='${filter.keyword}'/>" maxlength="100"
                                   placeholder="Mã đại lý, tên đại lý, số điện thoại..." autocomplete="off">
                        </div>
                    </div>
                    <div class="customer-filters__actions">
                        <button class="customer-filters__toggle" type="button" id="customer-filter-toggle"
                                aria-controls="customer-filter-fields" aria-expanded="${activeFilterCount > 0}">
                            <span class="customer-filters__icon" aria-hidden="true"></span>Bộ lọc
                            <c:if test="${activeFilterCount > 0}"><span class="customer-filters__count">${activeFilterCount}</span></c:if>
                        </button>
                        <button class="customer-filters__submit" type="submit" id="customer-filter-submit">Tìm kiếm</button>
                    </div>
                </div>

                <div class="customer-filters__fields" id="customer-filter-fields">
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="customer-region">Khu vực</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="customer-region" name="region">
                                <option value="">Tất cả</option>
                                <c:forEach var="region" items="${regions}">
                                    <option value="${region.id}"${region.id == filter.regionId ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="customer-group">Nhóm khách hàng</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="customer-group" name="group">
                                <option value="">Tất cả</option>
                                <c:forEach var="group" items="${groups}">
                                    <option value="${group.id}"${group.id == filter.customerGroupId ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <c:if test="${canViewAll}">
                        <div class="customer-filters__field">
                            <label class="customer-filters__label" for="customer-sales-rep">Người phụ trách</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="customer-sales-rep" name="salesRep">
                                    <option value="">Tất cả</option>
                                    <c:forEach var="rep" items="${salesReps}">
                                        <option value="${rep.id}"${rep.id == filter.salesRepId ? ' selected' : ''}><c:out value="${rep.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </c:if>
                    <div class="customer-filters__field">
                        <label class="customer-filters__label" for="customer-status">Trạng thái</label>
                        <div class="select">
                            <select class="account-filters__control select__control" id="customer-status" name="status">
                                <option value="">Tất cả</option>
                                <c:forEach var="status" items="${statuses}">
                                    <option value="${status.code}"${status == filter.status ? ' selected' : ''}>${status.label}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <c:if test="${filter.filtered}">
                        <a class="customer-filters__clear" id="customer-filter-clear" href="<c:url value='/customers'/>">Xoá bộ lọc</a>
                    </c:if>
                </div>
            </form>

            <section class="account-table-card" aria-label="Danh sách đại lý">
                <div class="account-table-scroll">
                    <table class="account-table customer-table" id="customer-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã đại lý</th>
                                <th scope="col">Tên đại lý</th>
                                <th scope="col">Khu vực</th>
                                <th scope="col">Nhóm khách hàng</th>
                                <th scope="col">Người phụ trách</th>
                                <th scope="col">Số điện thoại</th>
                                <th scope="col">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="customer" items="${customerPage.items}">
                                <c:url var="detailUrl" value="/customers/credit-limit"><c:param name="id" value="${customer.id}"/></c:url>
                                <tr>
                                    <td class="customer-table__code" data-label="Mã đại lý">
                                        <a class="customer-table__link" href="${fn:escapeXml(detailUrl)}"><c:out value="${customer.code}"/></a>
                                    </td>
                                    <td class="customer-table__name" data-label="Tên đại lý">
                                        <a class="customer-table__link" href="${fn:escapeXml(detailUrl)}"><c:out value="${customer.name}"/></a>
                                    </td>
                                    <td data-label="Khu vực"><c:out value="${customer.regionName}"/></td>
                                    <td data-label="Nhóm khách hàng"><c:out value="${customer.customerGroupName}"/></td>
                                    <td data-label="Người phụ trách">
                                        <c:choose>
                                            <c:when test="${empty customer.salesRepName}"><span class="customer-table__none">Chưa phân công</span></c:when>
                                            <c:otherwise><c:out value="${customer.salesRepName}"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td data-label="Số điện thoại">
                                        <c:choose>
                                            <c:when test="${empty customer.phone}"><span class="customer-table__none">—</span></c:when>
                                            <c:otherwise>
                                                <a class="customer-table__phone" href="tel:${fn:escapeXml(fn:replace(customer.phone, ' ', ''))}"><c:out value="${customer.phone}"/></a>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td data-label="Trạng thái">
                                        <span class="customer-status customer-status--${fn:toLowerCase(customer.status.code)}">${customer.status.label}</span>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty customerPage.items}">
                                <tr class="customer-table__empty-row">
                                    <td class="customer-table__empty" colspan="7">
                                        <c:choose>
                                            <c:when test="${not filter.filtered}">Chưa có đại lý nào trong phạm vi bạn được xem.</c:when>
                                            <c:otherwise>Không tìm thấy đại lý phù hợp. Thử bỏ bớt điều kiện lọc hoặc đổi từ khoá tìm kiếm.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info" id="customer-count">
                        <c:choose>
                            <c:when test="${customerPage.totalItems == 0}">Không có đại lý nào</c:when>
                            <c:otherwise>
                                Hiển thị ${customerPage.firstRowNumber} - ${customerPage.firstRowNumber + fn:length(customerPage.items) - 1}
                                trong tổng ${customerPage.totalItems} đại lý
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${customerPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="${pageUrl}${customerPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${customerPage.startPage}" end="${customerPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == customerPage.page}">
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
                                    <c:when test="${customerPage.page < customerPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="${pageUrl}${customerPage.page + 1}">›</a>
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
    <script src="<c:url value='/assets/js/customers.js'/>"></script>
</body>
</html>
