<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="stock"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Sổ tồn kho"/>
<c:url var="listUrl" value="/stock">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="category" value="${filter.categoryId}"/>
    <c:param name="level" value="${filter.level.code}"/>
</c:url>
<c:url var="exportUrl" value="/stock/export">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="category" value="${filter.categoryId}"/>
    <c:param name="level" value="${filter.level.code}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sổ tồn kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/stock.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Sổ tồn kho</h1>
                    <p class="inv-header__subtitle">Theo dõi tồn thực tế, hàng giữ chỗ và tồn khả dụng theo từng SKU tại mỗi kho.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="stock-export" href="${fn:escapeXml(exportUrl)}">
                        <span class="inv-icon inv-icon--download" aria-hidden="true"></span>Xuất danh sách
                    </a>
                </div>
            </header>

            <section class="inv-stats" aria-label="Tổng quan tồn kho">
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Tổng SKU đang quản lý</p>
                    <span class="inv-stat__icon tone-blue"><span class="inv-icon inv-icon--sheet" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-total"><fmt:formatNumber value="${summary.total}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">${filter.filtered ? 'Trong phạm vi bộ lọc đang chọn' : 'Theo từng kho đang lưu trữ'}</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">SKU hết hàng</p>
                    <span class="inv-stat__icon tone-red"><span class="inv-icon inv-icon--circle-x" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-out"><fmt:formatNumber value="${summary.out}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">Tồn khả dụng bằng 0</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">SKU sắp hết</p>
                    <span class="inv-stat__icon tone-amber"><span class="inv-icon inv-icon--triangle-alert" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-low"><fmt:formatNumber value="${summary.low}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">Cần theo dõi mức tồn khả dụng</p>
                </article>
            </section>

            <section class="inv-card" aria-labelledby="stock-filter-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="stock-filter-title">Tìm kiếm và lọc tồn kho</h2>
                        <p class="inv-card__subtitle">Lọc dữ liệu theo kho, nhóm hàng hoặc mã SKU và tên sản phẩm.</p>
                    </div>
                </div>
                <form class="stock-filters" id="stock-filter-form" action="<c:url value='/stock'/>" method="get" role="search">
                    <input type="hidden" name="level" value="${filter.level.code}">
                    <div class="inv-field stock-filters__keyword">
                        <label class="stock-filters__label" for="stock-keyword">SKU hoặc tên sản phẩm</label>
                        <span class="inv-search">
                            <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                            <input class="inv-control" type="search" id="stock-keyword" name="keyword" maxlength="100"
                                   value="<c:out value='${filter.keyword}'/>" placeholder="Nhập SKU hoặc tên sản phẩm" autocomplete="off">
                        </span>
                    </div>
                    <div class="inv-field stock-filters__warehouse">
                        <label class="stock-filters__label" for="stock-warehouse">Kho hàng</label>
                        <select class="inv-control" id="stock-warehouse" name="warehouse">
                            <option value="">Tất cả kho</option>
                            <c:forEach var="warehouse" items="${warehouses}">
                                <option value="${warehouse.id}"${warehouse.id == filter.warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field stock-filters__category">
                        <label class="stock-filters__label" for="stock-category">Nhóm hàng</label>
                        <select class="inv-control" id="stock-category" name="category">
                            <option value="">Tất cả nhóm hàng</option>
                            <c:forEach var="category" items="${categories}">
                                <option value="${category.id}"${category.id == filter.categoryId ? ' selected' : ''}><c:out value="${category.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <button class="inv-button inv-button--primary stock-filters__submit" type="submit" id="stock-filter-submit">
                        <span class="inv-icon inv-icon--funnel" aria-hidden="true"></span>Lọc
                    </button>
                    <%-- Chú giải trạng thái, bấm để lọc theo trạng thái tồn; bấm lại trạng thái đang chọn để bỏ lọc --%>
                    <div class="stock-legend" aria-label="Lọc theo trạng thái tồn">
                        <c:forEach var="level" items="${levels}">
                            <c:url var="levelUrl" value="/stock">
                                <c:param name="keyword" value="${filter.keyword}"/>
                                <c:param name="warehouse" value="${filter.warehouseId}"/>
                                <c:param name="category" value="${filter.categoryId}"/>
                                <c:param name="level" value="${level == filter.level ? '' : level.code}"/>
                            </c:url>
                            <a class="stock-legend__item stock-legend__item--${level.tone}${level == filter.level ? ' stock-legend__item--active' : ''}"
                               href="${fn:escapeXml(levelUrl)}"${level == filter.level ? ' aria-current="true"' : ''}>${level.label}</a>
                        </c:forEach>
                        <span class="stock-legend__note">Đơn vị tồn: đơn vị cơ sở</span>
                        <c:if test="${filter.filtered}">
                            <a class="stock-legend__clear" id="stock-filter-clear" href="<c:url value='/stock'/>">Xoá bộ lọc</a>
                        </c:if>
                    </div>
                </form>
            </section>

            <section class="inv-card" aria-labelledby="stock-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="stock-table-title">Chi tiết sổ tồn kho</h2>
                        <p class="inv-card__subtitle">Số liệu tồn kho theo từng sản phẩm và kho lưu trữ.</p>
                    </div>
                    <p class="inv-card__meta" id="stock-count">Tổng cộng <strong>${stockPage.totalItems} SKU</strong></p>
                </div>
                <div class="inv-table-scroll stock-table-wrap">
                    <table class="inv-table stock-table" id="stock-table">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm / SKU</th>
                                <th scope="col">Kho</th>
                                <th scope="col" class="is-number">Tồn thực tế</th>
                                <th scope="col" class="is-number">Đang giữ chỗ</th>
                                <th scope="col" class="is-number">Tồn khả dụng</th>
                                <th scope="col">Đơn vị cơ sở</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Nhóm hàng</th>
                                <th scope="col">Xem</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="stock" items="${stockPage.items}">
                                <c:url var="reservationsUrl" value="/stock/reservations">
                                    <c:param name="product" value="${stock.productId}"/>
                                    <c:param name="warehouse" value="${stock.warehouseId}"/>
                                    <c:param name="status" value="ALL"/>
                                </c:url>
                                <tr class="${stock.level == 'OUT' ? 'stock-table__row--out' : ''}">
                                    <td>
                                        <span class="inv-table__main"><c:out value="${stock.productName}"/></span>
                                        <span class="inv-table__sub">SKU: <c:out value="${stock.sku}"/></span>
                                    </td>
                                    <td>
                                        <span class="stock-table__warehouse"><c:out value="${stock.warehouseName}"/></span>
                                        <span class="stock-table__code"><c:out value="${stock.warehouseCode}"/></span>
                                    </td>
                                    <td class="is-number stock-table__qty"><fmt:formatNumber value="${stock.onHand}" maxFractionDigits="3"/></td>
                                    <td class="is-number stock-table__qty"><fmt:formatNumber value="${stock.reserved}" maxFractionDigits="3"/></td>
                                    <td class="is-number stock-table__qty stock-table__available--${stock.level.tone}"><fmt:formatNumber value="${stock.available}" maxFractionDigits="3"/></td>
                                    <td><c:out value="${stock.baseUnitName}"/></td>
                                    <td><span class="inv-pill inv-pill--${stock.level.tone}">${stock.level.label}</span></td>
                                    <td><c:out value="${stock.categoryName}"/></td>
                                    <td>
                                        <a class="inv-icon-button" href="${fn:escapeXml(reservationsUrl)}" title="Xem đơn đang giữ chỗ"
                                           aria-label="Xem đơn đang giữ chỗ <c:out value='${stock.sku}'/> tại <c:out value='${stock.warehouseCode}'/>">
                                            <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty stockPage.items}">
                                <tr>
                                    <td class="inv-table__empty" colspan="9">
                                        <c:choose>
                                            <c:when test="${filter.filtered}">Không có SKU nào khớp bộ lọc. Thử bỏ bớt điều kiện lọc.</c:when>
                                            <c:otherwise>Chưa có tồn kho. Tồn được ghi khi ghi sổ phiếu nhập kho.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="inv-footer">
                    <p class="inv-footer__info" id="stock-page-info">
                        <c:choose>
                            <c:when test="${stockPage.totalItems == 0}">Không có SKU nào</c:when>
                            <c:otherwise>Hiển thị <strong>${stockPage.firstRowNumber}–${stockPage.firstRowNumber + fn:length(stockPage.items) - 1}</strong>
                                trong tổng số <strong>${stockPage.totalItems} SKU</strong></c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="inv-pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${stockPage.page > 1}">
                                        <a class="inv-pagination__item" aria-label="Trang trước" href="${pageUrl}${stockPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${stockPage.startPage}" end="${stockPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == stockPage.page}">
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
                                    <c:when test="${stockPage.page < stockPage.totalPages}">
                                        <a class="inv-pagination__item" aria-label="Trang sau" href="${pageUrl}${stockPage.page + 1}">›</a>
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
</body>
</html>
