<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="products"/>
<c:set var="breadcrumbSection" value="Nhóm hàng"/>
<c:set var="breadcrumbPage" value="Sản phẩm của nhóm"/>
<c:set var="canManage" value="${currentUser.can('PRODUCT_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sản phẩm nhóm <c:out value="${category.name}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <h1 class="page-header__title">Sản phẩm của nhóm "<c:out value="${category.name}"/>"</h1>
                <p class="page-header__subtitle">
                    Mã nhóm <c:out value="${category.code}"/> · ${products.size()} sản phẩm gắn trực tiếp vào nhóm này
                    ${category.active ? '' : '· Nhóm đang ngừng hoạt động'}
                </p>
                <a class="category-products__back" id="back-to-categories" href="<c:url value='/categories'/>">‹ Quay lại danh sách nhóm hàng</a>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="category-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="category-flash-error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <form id="move-products-form" action="<c:url value='/categories/products/move'/>" method="post">
                <input type="hidden" name="categoryId" value="${category.id}">

                <c:if test="${canManage and not empty products}">
                    <div class="category-move">
                        <div class="form-group">
                            <label class="form-group__label" for="move-target">Chuyển sản phẩm đã chọn sang nhóm</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="move-target" name="targetCategoryId" required>
                                    <option value="">Chọn nhóm hàng</option>
                                    <c:forEach var="target" items="${moveTargets}">
                                        <option value="${target.id}"><c:forEach begin="2" end="${target.level}">&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${target.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <button class="button button--primary" type="submit" id="move-products-submit" disabled>Chuyển nhóm</button>
                        <p class="category-move__hint" id="move-selected-count">Chưa chọn sản phẩm nào.</p>
                    </div>
                </c:if>

                <section class="account-table-card" aria-label="Sản phẩm của nhóm">
                    <div class="account-table-scroll">
                        <table class="account-table product-table">
                            <thead>
                                <tr>
                                    <th scope="col">
                                        <c:if test="${canManage and not empty products}">
                                            <input class="product-table__check" type="checkbox" id="select-all-products" aria-label="Chọn tất cả sản phẩm">
                                        </c:if>
                                    </th>
                                    <th scope="col">Mã SKU</th>
                                    <th scope="col">Tên sản phẩm</th>
                                    <th scope="col">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="product" items="${products}">
                                    <tr>
                                        <td>
                                            <c:if test="${canManage}">
                                                <input class="product-table__check" type="checkbox" name="productIds" value="${product.id}"
                                                       aria-label="Chọn <c:out value='${product.name}'/>">
                                            </c:if>
                                        </td>
                                        <td><c:out value="${product.sku}"/></td>
                                        <td class="account-table__name"><c:out value="${product.name}"/></td>
                                        <td>
                                            <span class="status-badge status-badge--${product.status == 'ACTIVE' ? 'active' : 'discontinued'}">${product.status == 'ACTIVE' ? 'Đang kinh doanh' : 'Ngừng kinh doanh'}</span>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty products}">
                                    <tr>
                                        <td class="category-table__empty" colspan="4">Nhóm này chưa có sản phẩm nào gắn trực tiếp.</td>
                                    </tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </section>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/categories.js'/>"></script>
</body>
</html>
