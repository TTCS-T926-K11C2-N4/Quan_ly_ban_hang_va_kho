<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="product-list"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Danh sách sản phẩm"/>
<%-- Người chỉ có quyền xem không thấy nút thêm, sửa, xoá, đổi trạng thái; cột giá vốn chỉ hiện khi có COST_PRICE_VIEW --%>
<c:set var="canManage" value="${currentUser.can('PRODUCT_MANAGE')}"/>
<c:set var="columnCount" value="${canViewCost ? 10 : 9}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách sản phẩm | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/products.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header product-header">
                <div>
                    <h1 class="page-header__title product-header__title">Danh sách sản phẩm</h1>
                    <p class="page-header__subtitle">Quản lý thông tin sản phẩm, theo dõi số lượng, giá và trạng thái hàng hóa.</p>
                </div>
                <c:if test="${canManage}">
                    <div class="product-header__actions">
                        <a class="account-filters__create" id="create-product-link" href="<c:url value='/products/new'/>">
                            <span aria-hidden="true">+</span> Thêm sản phẩm
                        </a>
                    </div>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="product-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="product-flash-error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <form class="account-filters product-filters" id="product-filter-form" action="<c:url value='/products'/>" method="get" role="search">
                <input type="hidden" name="sort" value="${sort}">
                <div class="account-filters__field account-filters__field--keyword">
                    <label class="account-filters__label" for="product-keyword">Tìm kiếm</label>
                    <input class="account-filters__control product-filters__search" type="search" id="product-keyword" name="keyword"
                           value="<c:out value='${keyword}'/>" placeholder="Tìm kiếm tên sản phẩm, mã SKU..." maxlength="100">
                </div>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="product-category-filter">Danh mục</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="product-category-filter" name="categoryId">
                            <option value="">Tất cả</option>
                            <c:forEach var="category" items="${categories}">
                                <option value="${category.id}"${category.id == categoryFilter ? ' selected' : ''}><c:forEach begin="2" end="${category.level}">&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${category.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="product-status-filter">Trạng thái</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="product-status-filter" name="status">
                            <option value="">Tất cả</option>
                            <c:forEach var="status" items="${statuses}">
                                <option value="${status.code}"${status.code == statusFilter ? ' selected' : ''}>${status.label}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="product-warehouse-filter">Kho</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="product-warehouse-filter" name="warehouseId">
                            <option value="">Tất cả</option>
                            <c:forEach var="warehouse" items="${warehouses}">
                                <option value="${warehouse.id}"${warehouse.id == warehouseFilter ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <a class="product-filters__reset" id="reset-product-filters" href="<c:url value='/products'/>">
                    <span aria-hidden="true">⟳</span> Làm mới
                </a>
            </form>

            <section class="account-table-card" aria-label="Danh sách sản phẩm">
                <div class="account-table-scroll">
                    <table class="account-table product-list-table${canViewCost ? ' product-list-table--cost' : ''}" id="product-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Ảnh</th>
                                <th scope="col">Tên sản phẩm</th>
                                <th scope="col">Danh mục</th>
                                <th scope="col">Mã SKU</th>
                                <th scope="col">Quy đổi</th>
                                <c:if test="${canViewCost}">
                                    <th scope="col" aria-sort="${sort == 'cost_asc' ? 'ascending' : sort == 'cost_desc' ? 'descending' : 'none'}">
                                        <a class="sort-link" href="<c:url value='/products'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='categoryId' value='${categoryFilter}'/>
                                            <c:param name='status' value='${statusFilter}'/><c:param name='warehouseId' value='${warehouseFilter}'/>
                                            <c:param name='sort' value="${sort == 'cost_asc' ? 'cost_desc' : 'cost_asc'}"/>
                                        </c:url>">Giá vốn (VNĐ) <span class="sort-link__icon" aria-hidden="true">${sort == 'cost_asc' ? '▲' : sort == 'cost_desc' ? '▼' : '⇅'}</span></a>
                                    </th>
                                </c:if>
                                <th scope="col" aria-sort="${sort == 'stock_asc' ? 'ascending' : sort == 'stock_desc' ? 'descending' : 'none'}">
                                    <a class="sort-link" href="<c:url value='/products'>
                                        <c:param name='keyword' value='${keyword}'/><c:param name='categoryId' value='${categoryFilter}'/>
                                        <c:param name='status' value='${statusFilter}'/><c:param name='warehouseId' value='${warehouseFilter}'/>
                                        <c:param name='sort' value="${sort == 'stock_desc' ? 'stock_asc' : 'stock_desc'}"/>
                                    </c:url>">Tồn kho <span class="sort-link__icon" aria-hidden="true">${sort == 'stock_asc' ? '▲' : sort == 'stock_desc' ? '▼' : '⇅'}</span></a>
                                </th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="product" items="${productPage.items}" varStatus="loop">
                                <tr>
                                    <td class="account-table__index">${productPage.firstRowNumber + loop.index}</td>
                                    <td>
                                        <img class="product-thumb" alt="" width="40" height="40" loading="lazy"
                                             src="<c:url value='/products/image'><c:param name='id' value='${product.id}'/><c:param name='v' value='${product.imageFileId}'/></c:url>">
                                    </td>
                                    <td class="account-table__name"><c:out value="${product.name}"/></td>
                                    <td><c:out value="${product.categoryName}"/></td>
                                    <td class="product-sku"><c:out value="${product.sku}"/></td>
                                    <td class="product-conversions">
                                        <c:choose>
                                            <c:when test="${empty product.conversions}">—</c:when>
                                            <c:otherwise>
                                                <c:forEach var="conversion" items="${product.conversions}"><span class="product-conversion"><c:out value="${conversion}"/></span></c:forEach>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <c:if test="${canViewCost}">
                                        <td class="product-number"><fmt:formatNumber value="${product.costPrice}" maxFractionDigits="0"/></td>
                                    </c:if>
                                    <td class="product-number">
                                        <fmt:formatNumber value="${product.stock}" maxFractionDigits="3"/>
                                        <span class="product-unit"><c:out value="${product.baseUnitName}"/></span>
                                    </td>
                                    <td>
                                        <span class="status-badge stock-badge--${fn:toLowerCase(product.stockStatus.code)}">${product.stockStatus.label}</span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${canManage}">
                                                <div class="row-actions">
                                                    <a class="row-actions__icon" href="<c:url value='/products/edit'><c:param name='id' value='${product.id}'/></c:url>"
                                                       aria-label="Sửa sản phẩm ${fn:escapeXml(product.name)}" title="Sửa">
                                                        <img src="<c:url value='/assets/img/icons/edit.svg'/>" alt="" width="20" height="20">
                                                    </a>
                                                    <details class="row-menu">
                                                        <summary class="row-menu__toggle product-more" aria-label="Thao tác khác cho sản phẩm ${fn:escapeXml(product.name)}">•••</summary>
                                                        <div class="row-menu__list">
                                                            <form action="<c:url value='/products/status'/>" method="post"<c:if test="${not product.discontinued}">
                                                                  data-confirm="Ngừng kinh doanh sản phẩm &quot;${fn:escapeXml(product.name)}&quot;?"</c:if>>
                                                                <input type="hidden" name="id" value="${product.id}">
                                                                <input type="hidden" name="status" value="${product.discontinued ? 'ACTIVE' : 'DISCONTINUED'}">
                                                                <button class="row-menu__item row-menu__item--button" type="submit">
                                                                    ${product.discontinued ? 'Kinh doanh lại' : 'Ngừng kinh doanh'}
                                                                </button>
                                                            </form>
                                                            <form action="<c:url value='/products/delete'/>" method="post"
                                                                  data-confirm="Xóa sản phẩm &quot;${fn:escapeXml(product.name)}&quot;? Sản phẩm đã phát sinh giao dịch sẽ không xóa được.">
                                                                <input type="hidden" name="id" value="${product.id}">
                                                                <button class="row-menu__item row-menu__item--button row-menu__item--danger" type="submit">Xóa</button>
                                                            </form>
                                                        </div>
                                                    </details>
                                                </div>
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty productPage.items}">
                                <tr>
                                    <td class="category-table__empty" colspan="${columnCount}">
                                        ${empty keyword and empty categoryFilter and empty statusFilter and empty warehouseFilter
                                            ? 'Chưa có sản phẩm nào.' : 'Không tìm thấy sản phẩm phù hợp.'}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">
                        <c:choose>
                            <c:when test="${productPage.totalItems == 0}">Hiển thị 0 kết quả</c:when>
                            <c:otherwise>
                                Hiển thị ${productPage.firstRowNumber}-${productPage.firstRowNumber + fn:length(productPage.items) - 1}
                                trong ${productPage.totalItems} kết quả
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${productPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/products'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='categoryId' value='${categoryFilter}'/>
                                            <c:param name='status' value='${statusFilter}'/><c:param name='warehouseId' value='${warehouseFilter}'/>
                                            <c:param name='sort' value='${sort}'/><c:param name='page' value='${productPage.page - 1}'/>
                                        </c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${productPage.startPage}" end="${productPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == productPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/products'>
                                                <c:param name='keyword' value='${keyword}'/><c:param name='categoryId' value='${categoryFilter}'/>
                                                <c:param name='status' value='${statusFilter}'/><c:param name='warehouseId' value='${warehouseFilter}'/>
                                                <c:param name='sort' value='${sort}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${productPage.page < productPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/products'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='categoryId' value='${categoryFilter}'/>
                                            <c:param name='status' value='${statusFilter}'/><c:param name='warehouseId' value='${warehouseFilter}'/>
                                            <c:param name='sort' value='${sort}'/><c:param name='page' value='${productPage.page + 1}'/>
                                        </c:url>">›</a>
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
    <script src="<c:url value='/assets/js/categories.js'/>"></script>
    <script src="<c:url value='/assets/js/products.js'/>"></script>
</body>
</html>
