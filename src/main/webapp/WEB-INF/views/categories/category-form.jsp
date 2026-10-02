<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="categories"/>
<c:set var="breadcrumbSection" value="Nhóm hàng"/>
<%-- Dùng chung cho Thêm và Sửa nhóm hàng; Servlet sửa gán editing = true và category --%>
<c:set var="pageTitle" value="${editing ? 'Sửa nhóm hàng' : 'Thêm nhóm hàng'}"/>
<c:set var="breadcrumbPage" value="${pageTitle}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">
                    ${editing ? 'Cập nhật thông tin hoặc chuyển nhóm hàng sang nhóm cha khác.' : 'Thêm nhóm hàng mới vào cây danh mục.'}
                </p>
            </header>

            <form class="account-form" id="category-form" method="post"
                  action="<c:url value='${editing ? "/categories/edit" : "/categories/new"}'/>">
                <c:if test="${editing}">
                    <input type="hidden" name="id" value="${category.id}">
                </c:if>
                <section class="account-form__section" aria-labelledby="category-info-title">
                    <h2 class="account-form__section-title" id="category-info-title">Thông tin nhóm hàng</h2>
                    <p class="account-form__section-desc">Cây nhóm hàng tối đa ${maxLevel} cấp. Mã nhóm dùng để đối chiếu khi nhập sản phẩm từ Excel.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.code ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="category-code">Mã nhóm hàng *</label>
                            <input class="form-group__control" type="text" id="category-code" name="code"
                                   value="<c:out value='${form.code}'/>" placeholder="Ví dụ: BIA-LON" maxlength="30"
                                   autocomplete="off" required
                                   aria-describedby="category-code-hint category-code-error" aria-invalid="${not empty errors.code}">
                            <p class="form-group__hint" id="category-code-hint">2–30 ký tự: chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang.</p>
                            <p class="form-group__error" id="category-code-error"${empty errors.code ? ' hidden' : ''}><c:out value="${errors.code}"/></p>
                        </div>

                        <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="category-name">Tên nhóm hàng *</label>
                            <input class="form-group__control" type="text" id="category-name" name="name"
                                   value="<c:out value='${form.name}'/>" placeholder="Nhập tên nhóm hàng" maxlength="150"
                                   autocomplete="off" required
                                   aria-describedby="category-name-error" aria-invalid="${not empty errors.name}">
                            <p class="form-group__error" id="category-name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                        </div>

                        <div class="form-group${not empty errors.parentId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="category-parent">Nhóm cha</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="category-parent" name="parentId"
                                        aria-describedby="category-parent-hint category-parent-error" aria-invalid="${not empty errors.parentId}">
                                    <option value="">— Không có (nhóm cấp 1) —</option>
                                    <c:forEach var="option" items="${parentOptions}">
                                        <option value="${option.id}"${form.parentId == option.id ? ' selected' : ''}><c:forEach begin="2" end="${option.level}">&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${option.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__hint" id="category-parent-hint">
                                <c:choose>
                                    <c:when test="${editing and category.childCount > 0}">Đổi nhóm cha sẽ chuyển cả ${category.childCount} nhóm con đi theo.</c:when>
                                    <c:otherwise>Chỉ hiện các nhóm đang hoạt động và còn chỗ trong giới hạn ${maxLevel} cấp.</c:otherwise>
                                </c:choose>
                            </p>
                            <p class="form-group__error" id="category-parent-error"${empty errors.parentId ? ' hidden' : ''}><c:out value="${errors.parentId}"/></p>
                        </div>

                        <div class="form-group${not empty errors.sortOrder ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="category-sort">Thứ tự hiển thị</label>
                            <input class="form-group__control" type="number" id="category-sort" name="sortOrder"
                                   value="<c:out value='${form.sortOrder}'/>" placeholder="0" min="0" max="9999"
                                   aria-describedby="category-sort-hint category-sort-error" aria-invalid="${not empty errors.sortOrder}">
                            <p class="form-group__hint" id="category-sort-hint">Số nhỏ đứng trước trong cùng nhóm cha.</p>
                            <p class="form-group__error" id="category-sort-error"${empty errors.sortOrder ? ' hidden' : ''}><c:out value="${errors.sortOrder}"/></p>
                        </div>

                        <div class="form-group form-group--full${not empty errors.description ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="category-description">Mô tả</label>
                            <textarea class="form-group__control form-group__control--textarea" id="category-description"
                                      name="description" maxlength="500" placeholder="Ví dụ: Bia đóng lon 330ml, 500ml"
                                      aria-describedby="category-description-error" aria-invalid="${not empty errors.description}"><c:out value="${form.description}"/></textarea>
                            <p class="form-group__error" id="category-description-error"${empty errors.description ? ' hidden' : ''}><c:out value="${errors.description}"/></p>
                        </div>
                    </div>
                </section>

                <footer class="account-form__footer">
                    <a class="button button--secondary" id="cancel-category-form" href="<c:url value='/categories'/>">Hủy</a>
                    <button class="button button--primary" type="submit" id="save-category-submit">${editing ? 'Lưu thay đổi' : 'Tạo nhóm hàng'}</button>
                </footer>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
