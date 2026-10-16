<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="warehouses"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Danh sách kho & vị trí"/>
<%-- Link giữ nguyên điều kiện lọc hiện tại, dùng cho phân trang và xuất Excel --%>
<c:url var="listUrl" value="/warehouses">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="type" value="${filter.type}"/>
    <c:param name="status" value="${filter.status}"/>
</c:url>
<c:url var="exportUrl" value="/warehouses/export">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="type" value="${filter.type}"/>
    <c:param name="status" value="${filter.status}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách kho &amp; vị trí | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/warehouses.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Danh sách kho &amp; vị trí</h1>
                    <p class="inv-header__subtitle">Quản lý thông tin kho hàng và vị trí lưu trữ trong hệ thống.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="warehouse-export" href="${fn:escapeXml(exportUrl)}">
                        <span class="inv-icon inv-icon--download" aria-hidden="true"></span>Xuất danh sách
                    </a>
                    <c:if test="${canManage}">
                        <a class="inv-button inv-button--primary" id="warehouse-create" href="<c:url value='/warehouses/new'/>">
                            <span class="inv-icon inv-icon--plus" aria-hidden="true"></span>Thêm kho mới
                        </a>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="warehouse-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="warehouse-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-stats" aria-label="Tổng quan kho">
                <article class="inv-stat">
                    <span class="inv-stat__icon tone-blue"><span class="inv-icon inv-icon--warehouse" aria-hidden="true"></span></span>
                    <div>
                        <p class="inv-stat__label">Tổng số kho</p>
                        <p class="inv-stat__value" id="stat-total">${summary.total}</p>
                    </div>
                </article>
                <article class="inv-stat">
                    <span class="inv-stat__icon tone-green"><span class="inv-icon inv-icon--circle-check" aria-hidden="true"></span></span>
                    <div>
                        <p class="inv-stat__label">Kho đang hoạt động</p>
                        <p class="inv-stat__value" id="stat-active">${summary.active}</p>
                    </div>
                </article>
                <article class="inv-stat">
                    <span class="inv-stat__icon tone-amber"><span class="inv-icon inv-icon--triangle-alert" aria-hidden="true"></span></span>
                    <div>
                        <p class="inv-stat__label">Kho hàng lỗi</p>
                        <p class="inv-stat__value" id="stat-defect">${summary.defect}</p>
                    </div>
                </article>
            </section>

            <form class="inv-card warehouse-filters" id="warehouse-filter-form" action="<c:url value='/warehouses'/>" method="get" role="search">
                <label class="inv-search warehouse-filters__keyword">
                    <span class="visually-hidden">Tìm kho</span>
                    <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                    <input class="inv-control" type="search" id="warehouse-keyword" name="keyword" maxlength="100"
                           value="<c:out value='${filter.keyword}'/>" placeholder="Tìm theo mã kho, tên kho..." autocomplete="off">
                </label>
                <label class="warehouse-filters__select">
                    <span class="visually-hidden">Loại kho</span>
                    <select class="inv-control" id="warehouse-type" name="type" data-auto-submit>
                        <option value="">Tất cả loại kho</option>
                        <c:forEach var="type" items="${typeLabels}">
                            <option value="${type.key}"${type.key == filter.type ? ' selected' : ''}>${type.value}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="warehouse-filters__select">
                    <span class="visually-hidden">Trạng thái</span>
                    <select class="inv-control" id="warehouse-status" name="status" data-auto-submit>
                        <option value="">Tất cả trạng thái</option>
                        <c:forEach var="status" items="${statusLabels}">
                            <option value="${status.key}"${status.key == filter.status ? ' selected' : ''}>${status.value}</option>
                        </c:forEach>
                    </select>
                </label>
                <button class="visually-hidden" type="submit">Tìm kiếm</button>
                <c:if test="${filter.filtered}">
                    <a class="warehouse-filters__clear" id="warehouse-filter-clear" href="<c:url value='/warehouses'/>">Xoá bộ lọc</a>
                </c:if>
                <p class="warehouse-filters__count" id="warehouse-count">${warehousePage.totalItems} kho</p>
            </form>

            <section class="inv-card" aria-labelledby="warehouse-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="warehouse-table-title">Danh sách kho</h2>
                        <p class="inv-card__subtitle">Thông tin các kho hiện có trong hệ thống</p>
                    </div>
                    <button class="inv-button inv-button--small" type="button" id="warehouse-filter-toggle"
                            aria-controls="warehouse-filter-form" aria-expanded="true">
                        <span class="inv-icon inv-icon--list-filter" aria-hidden="true"></span>Bộ lọc
                    </button>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table warehouse-table" id="warehouse-table">
                        <thead>
                            <tr>
                                <th scope="col" class="warehouse-table__check">
                                    <input class="inv-checkbox" type="checkbox" id="warehouse-check-all" aria-label="Chọn tất cả kho">
                                </th>
                                <th scope="col">Mã kho</th>
                                <th scope="col">Tên kho</th>
                                <th scope="col">Địa chỉ</th>
                                <th scope="col">Loại kho</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="warehouse" items="${warehousePage.items}">
                                <c:url var="locationsUrl" value="/warehouses/locations"><c:param name="id" value="${warehouse.id}"/></c:url>
                                <c:url var="editUrl" value="/warehouses/edit"><c:param name="id" value="${warehouse.id}"/></c:url>
                                <tr>
                                    <td class="warehouse-table__check">
                                        <input class="inv-checkbox warehouse-table__select" type="checkbox" value="${warehouse.id}"
                                               aria-label="Chọn kho <c:out value='${warehouse.code}'/>">
                                    </td>
                                    <td><a class="inv-table__code" href="${fn:escapeXml(locationsUrl)}"><c:out value="${warehouse.code}"/></a></td>
                                    <td>
                                        <span class="inv-table__main"><c:out value="${warehouse.name}"/></span>
                                        <c:if test="${not empty warehouse.description}">
                                            <span class="inv-table__sub"><c:out value="${warehouse.description}"/></span>
                                        </c:if>
                                    </td>
                                    <td>
                                        <span class="warehouse-table__address"><c:out value="${warehouse.address}"/></span>
                                        <span class="inv-table__sub">Phụ trách: <c:out value="${empty warehouse.managerName ? 'Chưa phân công' : warehouse.managerName}"/></span>
                                    </td>
                                    <td><span class="inv-pill ${warehouse.defect ? 'inv-pill--amber' : 'inv-pill--blue'}">${warehouse.typeLabel}</span></td>
                                    <td><span class="inv-pill ${warehouse.active ? 'inv-pill--green' : 'inv-pill--gray'}">${warehouse.statusLabel}</span></td>
                                    <td>
                                        <div class="inv-actions warehouse-table__actions">
                                            <c:if test="${canManage}">
                                                <a class="inv-icon-button inv-icon-button--plain" href="${fn:escapeXml(editUrl)}"
                                                   title="Sửa kho" aria-label="Sửa kho <c:out value='${warehouse.code}'/>">
                                                    <span class="inv-icon inv-icon--pencil" aria-hidden="true"></span>
                                                </a>
                                            </c:if>
                                            <a class="inv-icon-button inv-icon-button--plain" href="${fn:escapeXml(locationsUrl)}"
                                               title="Xem vị trí" aria-label="Xem vị trí kho <c:out value='${warehouse.code}'/>">
                                                <span class="inv-icon inv-icon--eye" aria-hidden="true"></span>
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty warehousePage.items}">
                                <tr>
                                    <td class="inv-table__empty" colspan="7">
                                        <c:choose>
                                            <c:when test="${filter.filtered}">Không tìm thấy kho phù hợp. Thử bỏ bớt điều kiện lọc hoặc đổi từ khoá.</c:when>
                                            <c:otherwise>Chưa có kho nào trong hệ thống.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="inv-footer">
                    <p class="inv-footer__info" id="warehouse-page-info">
                        <c:choose>
                            <c:when test="${warehousePage.totalItems == 0}">Không có kho nào</c:when>
                            <c:otherwise>Hiển thị ${warehousePage.firstRowNumber}–${warehousePage.firstRowNumber + fn:length(warehousePage.items) - 1}
                                trong tổng số ${warehousePage.totalItems} kho</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="inv-pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${warehousePage.page > 1}">
                                        <a class="inv-pagination__item" aria-label="Trang trước" href="${pageUrl}${warehousePage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${warehousePage.startPage}" end="${warehousePage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == warehousePage.page}">
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
                                    <c:when test="${warehousePage.page < warehousePage.totalPages}">
                                        <a class="inv-pagination__item" aria-label="Trang sau" href="${pageUrl}${warehousePage.page + 1}">›</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/warehouses.js'/>"></script>
</body>
</html>
