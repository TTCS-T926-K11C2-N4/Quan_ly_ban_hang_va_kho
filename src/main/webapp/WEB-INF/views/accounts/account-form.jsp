<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="account-list"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Quản lý tài khoản"/>
<%-- Dùng chung cho S1-09A Tạo và S1-09B Sửa; Servlet sửa gán editing = true và account (EditableAccount) --%>
<c:set var="pageTitle" value="${editing ? 'Chỉnh sửa tài khoản' : 'Tạo tài khoản người dùng'}"/>
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
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">${editing ? 'Cập nhật thông tin và phân quyền của người dùng.' : 'Thêm người dùng mới và gán quyền truy cập phù hợp.'}</p>
            </header>

            <%-- novalidate: kiểm tra bằng account-form.js để hiện lỗi dưới từng ô; server luôn kiểm tra lại --%>
            <%-- data-allow-no-roles: tài khoản Đại lý giữ vai trò Đại lý (không có trong form) nên được bỏ trống vai trò nội bộ --%>
            <form class="account-form" id="account-form" method="post" novalidate
                  action="<c:url value='${editing ? "/accounts/edit" : "/accounts/new"}'/>"${editing && account.hasCustomerRole() ? ' data-allow-no-roles' : ''}>
                <c:if test="${editing}">
                    <input type="hidden" name="id" value="${account.id}">
                </c:if>
                <section class="account-form__section" aria-labelledby="basic-info-title">
                    <h2 class="account-form__section-title" id="basic-info-title">Thông tin cơ bản</h2>
                    <p class="account-form__section-desc">Thông tin định danh và liên hệ của người dùng.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.fullName ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="full-name">Họ và tên *</label>
                            <input class="form-group__control" type="text" id="full-name" name="fullName"
                                   value="<c:out value='${form.fullName}'/>" placeholder="Nhập họ và tên"
                                   maxlength="150" autocomplete="off" required
                                   aria-describedby="full-name-error" aria-invalid="${not empty errors.fullName}">
                            <p class="form-group__error" id="full-name-error"${empty errors.fullName ? ' hidden' : ''}><c:out value="${errors.fullName}"/></p>
                        </div>

                        <div class="form-group${not empty errors.username ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="username">Tên đăng nhập *</label>
                            <%-- Khi sửa: chỉ xem, server không đọc giá trị này --%>
                            <input class="form-group__control" type="text" id="username" name="username"
                                   value="<c:out value='${form.username}'/>" placeholder="Nhập tên đăng nhập"
                                   maxlength="50" autocomplete="off" required${editing ? ' readonly' : ''}
                                   aria-describedby="username-error" aria-invalid="${not empty errors.username}">
                            <p class="form-group__error" id="username-error"${empty errors.username ? ' hidden' : ''}><c:out value="${errors.username}"/></p>
                        </div>

                        <div class="form-group${not empty errors.email ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="email">Email *</label>
                            <input class="form-group__control" type="email" id="email" name="email"
                                   value="<c:out value='${form.email}'/>" placeholder="example@company.vn"
                                   maxlength="150" autocomplete="off" required
                                   aria-describedby="email-error" aria-invalid="${not empty errors.email}">
                            <p class="form-group__error" id="email-error"${empty errors.email ? ' hidden' : ''}><c:out value="${errors.email}"/></p>
                        </div>

                        <div class="form-group${not empty errors.phone ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="phone">Số điện thoại *</label>
                            <input class="form-group__control" type="tel" id="phone" name="phone"
                                   value="<c:out value='${form.phone}'/>" placeholder="Nhập số điện thoại"
                                   maxlength="10" inputmode="numeric" autocomplete="off" required
                                   aria-describedby="phone-hint phone-error" aria-invalid="${not empty errors.phone}">
                            <p class="form-group__hint" id="phone-hint">Định dạng 10 chữ số.</p>
                            <p class="form-group__error" id="phone-error"${empty errors.phone ? ' hidden' : ''}><c:out value="${errors.phone}"/></p>
                        </div>
                    </div>
                </section>

                <section class="account-form__section account-form__section--divided" aria-labelledby="roles-title">
                    <h2 class="account-form__section-title" id="roles-title">Vai trò nghiệp vụ</h2>
                    <p class="account-form__section-desc" id="roles-desc">Có thể chọn một hoặc nhiều vai trò.</p>

                    <div class="role-options${not empty errors.roleCodes ? ' role-options--invalid' : ''}" id="role-options"
                         role="group" aria-labelledby="roles-title" aria-describedby="roles-desc role-codes-error">
                        <c:forEach var="role" items="${roles}">
                            <label class="checkbox">
                                <input class="checkbox__input" type="checkbox" name="roleCodes" value="<c:out value='${role.code}'/>"
                                       id="role-${role.code}"${form != null && form.hasRole(role.code) ? ' checked' : ''}>
                                <span class="checkbox__box" aria-hidden="true"></span>
                                <span class="checkbox__label"><c:out value="${role.name}"/></span>
                            </label>
                        </c:forEach>
                    </div>
                    <p class="form-group__error" id="role-codes-error"${empty errors.roleCodes ? ' hidden' : ''}><c:out value="${errors.roleCodes}"/></p>

                    <div class="account-form__grid account-form__grid--assignments">
                        <div class="form-group${not empty errors.warehouseId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="warehouse-id">Kho phụ trách *</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="warehouse-id" name="warehouseId"
                                        aria-describedby="warehouse-hint warehouse-error" aria-invalid="${not empty errors.warehouseId}">
                                    <option value="">Chọn kho phụ trách</option>
                                    <c:forEach var="warehouse" items="${warehouses}">
                                        <option value="${warehouse.id}"${form.warehouseId == warehouse.id ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__hint" id="warehouse-hint">Bắt buộc khi có vai trò Quản lý kho hoặc Nhân viên kho.</p>
                            <p class="form-group__error" id="warehouse-error"${empty errors.warehouseId ? ' hidden' : ''}><c:out value="${errors.warehouseId}"/></p>
                        </div>

                        <div class="form-group${not empty errors.regionId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="region-id">Địa bàn phụ trách${editing ? ' *' : ''}</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="region-id" name="regionId"
                                        aria-describedby="region-hint region-error" aria-invalid="${not empty errors.regionId}">
                                    <option value="">Chọn địa bàn phụ trách</option>
                                    <c:forEach var="region" items="${regions}">
                                        <option value="${region.id}"${form.regionId == region.id ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__hint" id="region-hint">Bắt buộc khi có vai trò Nhân viên kinh doanh.</p>
                            <p class="form-group__error" id="region-error"${empty errors.regionId ? ' hidden' : ''}><c:out value="${errors.regionId}"/></p>
                        </div>
                    </div>
                </section>

                <footer class="account-form__footer">
                    <a class="button button--secondary" id="cancel-account-form" href="<c:url value='/accounts'/>">Hủy</a>
                    <c:choose>
                        <c:when test="${not editing}">
                            <button class="button button--primary" type="submit" id="create-account-submit">Tạo tài khoản</button>
                        </c:when>
                        <%-- Tài khoản tự đăng ký đang chờ duyệt: thêm nút lưu kèm kích hoạt --%>
                        <c:when test="${account.pending}">
                            <button class="button button--secondary" type="submit" id="save-account-submit">Lưu thay đổi</button>
                            <button class="button button--primary" type="submit" id="save-activate-account-submit"
                                    name="action" value="activate">Lưu và kích hoạt</button>
                        </c:when>
                        <c:otherwise>
                            <button class="button button--primary" type="submit" id="save-account-submit">Lưu thay đổi</button>
                        </c:otherwise>
                    </c:choose>
                </footer>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/account-form.js'/>"></script>
</body>
</html>
