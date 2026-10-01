<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="products"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Nhóm hàng"/>
<%-- Người chỉ có quyền xem không thấy nút thêm, sửa, xoá, đổi trạng thái --%>
<c:set var="canManage" value="${currentUser.can('PRODUCT_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý nhóm hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Quản lý nhóm hàng</h1>
                <p class="page-header__subtitle">Quản lý các nhóm hàng hóa, sản phẩm trong hệ thống. Bạn có thể thêm, sửa, xóa và sắp xếp nhóm hàng theo cấu trúc cây danh mục.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="category-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="category-flash-error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <form class="account-filters" id="category-filter-form" action="<c:url value='/categories'/>" method="get" role="search">
                <div class="account-filters__field account-filters__field--keyword">
                    <label class="account-filters__label" for="category-keyword">Tìm kiếm</label>
                    <input class="account-filters__control" type="search" id="category-keyword" name="keyword"
                           value="<c:out value='${keyword}'/>" placeholder="Nhập tên hoặc mã nhóm hàng..." maxlength="100">
                </div>

                <div class="account-filters__field account-filters__field--status">
                    <label class="account-filters__label" for="category-status-filter">Trạng thái</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="category-status-filter" name="status">
                            <option value="all">Tất cả</option>
                            <option value="active"${statusFilter == 'active' ? ' selected' : ''}>Hoạt động</option>
                            <option value="inactive"${statusFilter == 'inactive' ? ' selected' : ''}>Ngừng hoạt động</option>
                        </select>
                    </div>
                </div>

                <c:if test="${canManage}">
                    <a class="account-filters__create" id="create-category-link" href="<c:url value='/categories/new'/>">
                        <span aria-hidden="true">+</span> Tạo nhóm hàng
                    </a>
                </c:if>
            </form>

            <section class="account-table-card" aria-label="Cây nhóm hàng">
                <div class="account-table-scroll">
                    <table class="account-table category-table" id="category-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Tên nhóm hàng</th>
                                <th scope="col">Mô tả</th>
                                <th scope="col">Số sản phẩm</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${categoryPage.items}">
                                <c:set var="category" value="${row.category}"/>
                                <tr class="${row.matched ? '' : 'category-row--context'}" data-path="${category.path}">
                                    <td class="account-table__index">${row.number}</td>
                                    <td>
                                        <div class="category-name category-name--level-${category.level}">
                                            <c:choose>
                                                <c:when test="${category.childCount > 0}">
                                                    <button class="category-toggle" type="button" aria-expanded="true"
                                                            aria-label="Thu gọn nhóm ${fn:escapeXml(category.name)}">
                                                        <img src="<c:url value='/assets/img/icons/chevron-down.svg'/>" alt="" width="16" height="16">
                                                    </button>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="category-toggle-spacer" aria-hidden="true"></span>
                                                </c:otherwise>
                                            </c:choose>
                                            <img src="<c:url value='/assets/img/icons/folder.svg'/>" alt="" width="16" height="16">
                                            <span><c:out value="${category.name}"/></span>
                                        </div>
                                    </td>
                                    <td class="category-description"><c:out value="${empty category.description ? '—' : category.description}"/></td>
                                    <td>${category.branchProductCount}</td>
                                    <td>
                                        <span class="status-badge status-badge--${category.active ? 'active' : 'inactive'}">${category.active ? 'Hoạt động' : 'Ngừng hoạt động'}</span>
                                    </td>
                                    <td>
                                        <div class="row-actions">
                                            <c:choose>
                                                <c:when test="${canManage}">
                                                    <a class="row-actions__link" href="<c:url value='/categories/edit'><c:param name='id' value='${category.id}'/></c:url>">Sửa</a>
                                                    <form action="<c:url value='/categories/delete'/>" method="post"
                                                          data-confirm="Xóa nhóm hàng &quot;${fn:escapeXml(category.name)}&quot;?">
                                                        <input type="hidden" name="id" value="${category.id}">
                                                        <button class="row-actions__link row-actions__button" type="submit">Xóa</button>
                                                    </form>
                                                    <details class="row-menu">
                                                        <summary class="row-menu__toggle" aria-label="Thao tác khác cho nhóm ${fn:escapeXml(category.name)}">⋮</summary>
                                                        <div class="row-menu__list">
                                                            <c:if test="${category.active and category.level < 5}">
                                                                <a class="row-menu__item" href="<c:url value='/categories/new'><c:param name='parentId' value='${category.id}'/></c:url>">Thêm nhóm con</a>
                                                            </c:if>
                                                            <a class="row-menu__item" href="<c:url value='/categories/products'><c:param name='id' value='${category.id}'/></c:url>">Xem / chuyển sản phẩm</a>
                                                            <form action="<c:url value='/categories/status'/>" method="post"<c:if test="${category.active}">
                                                                  data-confirm="Ngừng hoạt động nhóm &quot;${fn:escapeXml(category.name)}&quot;${category.childCount > 0 ? ' và toàn bộ nhóm con' : ''}?"</c:if>>
                                                                <input type="hidden" name="id" value="${category.id}">
                                                                <input type="hidden" name="active" value="${not category.active}">
                                                                <button class="row-menu__item row-menu__item--button${category.active ? ' row-menu__item--danger' : ''}" type="submit">
                                                                    ${category.active ? 'Ngừng hoạt động' : 'Hoạt động lại'}
                                                                </button>
                                                            </form>
                                                        </div>
                                                    </details>
                                                </c:when>
                                                <c:otherwise>
                                                    <a class="row-actions__link" href="<c:url value='/categories/products'><c:param name='id' value='${category.id}'/></c:url>">Xem sản phẩm</a>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty categoryPage.items}">
                                <tr>
                                    <td class="category-table__empty" colspan="6">
                                        ${empty keyword and statusFilter == 'all' ? 'Chưa có nhóm hàng nào.' : 'Không tìm thấy nhóm hàng phù hợp.'}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">
                        <c:choose>
                            <c:when test="${categoryPage.totalItems == 0}">Hiển thị 0 nhóm hàng</c:when>
                            <c:otherwise>
                                Hiển thị ${categoryPage.firstRowNumber} - ${categoryPage.firstRowNumber + categoryPage.pageSize - 1 > categoryPage.totalItems ? categoryPage.totalItems : categoryPage.firstRowNumber + categoryPage.pageSize - 1}
                                trong tổng ${categoryPage.totalItems} nhóm hàng
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${categoryPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/categories'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='status' value='${statusFilter}'/><c:param name='page' value='${categoryPage.page - 1}'/>
                                        </c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${categoryPage.startPage}" end="${categoryPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == categoryPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/categories'>
                                                <c:param name='keyword' value='${keyword}'/><c:param name='status' value='${statusFilter}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${categoryPage.page < categoryPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/categories'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='status' value='${statusFilter}'/><c:param name='page' value='${categoryPage.page + 1}'/>
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
</body>
</html>
