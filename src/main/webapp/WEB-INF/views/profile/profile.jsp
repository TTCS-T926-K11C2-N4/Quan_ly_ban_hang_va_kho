<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="profile"/>
<c:set var="breadcrumbSection" value="${currentUser.customer ? 'Trang chủ' : 'Tổng quan'}"/>
<c:set var="breadcrumbPage" value="Hồ sơ cá nhân"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hồ sơ cá nhân | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/profile.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content profile-page">
            <header class="page-header">
                <h1 class="page-header__title">Hồ sơ cá nhân</h1>
            </header>

            <c:if test="${not empty flashMessage}">
                <p class="profile-flash" id="profile-flash" role="status"><c:out value="${flashMessage}"/></p>
            </c:if>

            <section class="profile-card" aria-label="Thông tin cá nhân">
                <%-- Tải ảnh: chọn tệp là gửi luôn (profile.js), không cần nút Lưu riêng --%>
                <form class="profile-avatar" id="profile-avatar-form" action="<c:url value='/profile/avatar'/>"
                      method="post" enctype="multipart/form-data" data-max-bytes="2097152">
                    <c:choose>
                        <c:when test="${not empty currentUser.avatarFileId}">
                            <img class="profile-avatar__image" id="profile-avatar-image" alt="Ảnh đại diện" width="160" height="160"
                                 src="<c:url value='/avatar'><c:param name='user' value='${currentUser.id}'/><c:param name='size' value='full'/><c:param name='v' value='${currentUser.avatarFileId}'/></c:url>">
                        </c:when>
                        <c:otherwise>
                            <img class="profile-avatar__image" id="profile-avatar-image" alt="Chưa có ảnh đại diện" width="160" height="160"
                                 src="<c:url value='/assets/img/avatar-placeholder.svg'/>">
                        </c:otherwise>
                    </c:choose>
                    <p class="profile-avatar__hint">JPG/PNG tối đa 2MB</p>
                    <input class="visually-hidden" type="file" id="profile-avatar-file" name="avatar"
                           accept="image/jpeg,image/png,.jpg,.jpeg,.png">
                    <label class="profile-button" for="profile-avatar-file" id="profile-avatar-upload">
                        <img src="<c:url value='/assets/img/icons/upload.svg'/>" alt="" width="16" height="16">
                        <span id="profile-avatar-upload-text">Tải ảnh lên</span>
                    </label>
                    <p class="profile-field__error profile-avatar__error" id="profile-avatar-error"${empty flashError ? ' hidden' : ''} role="alert"><c:out value="${flashError}"/></p>
                </form>

                <%-- Chỉ gửi họ tên và số điện thoại; các ô khác chỉ để xem nên không có name --%>
                <form class="profile-fields" id="profile-form" action="<c:url value='/profile'/>" method="post">
                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-full-name">Họ và tên<span class="profile-field__required">*</span></label>
                        <div class="profile-field__control${not empty errors.fullName ? ' profile-field__control--invalid' : ''}">
                            <span class="profile-field__icon profile-field__icon--user" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-full-name" name="fullName"
                                   value="<c:out value='${fullName}'/>" maxlength="150" required autocomplete="name"
                                   aria-describedby="profile-full-name-error" aria-invalid="${not empty errors.fullName}">
                        </div>
                        <p class="profile-field__error" id="profile-full-name-error"${empty errors.fullName ? ' hidden' : ''}><c:out value="${errors.fullName}"/></p>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-phone">Số điện thoại<span class="profile-field__required">*</span></label>
                        <div class="profile-field__control${not empty errors.phone ? ' profile-field__control--invalid' : ''}">
                            <span class="profile-field__icon profile-field__icon--phone" aria-hidden="true"></span>
                            <input class="profile-field__input" type="tel" id="profile-phone" name="phone"
                                   value="<c:out value='${phone}'/>" maxlength="20" required autocomplete="tel" inputmode="tel"
                                   aria-describedby="profile-phone-hint profile-phone-error" aria-invalid="${not empty errors.phone}">
                        </div>
                        <p class="profile-field__hint" id="profile-phone-hint">* Số điện thoại di động Việt Nam: bắt đầu bằng 0 hoặc +84, đủ 10 số.</p>
                        <p class="profile-field__error" id="profile-phone-error"${empty errors.phone ? ' hidden' : ''}><c:out value="${errors.phone}"/></p>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-username">Tài khoản</label>
                        <div class="profile-field__control profile-field__control--readonly">
                            <span class="profile-field__icon profile-field__icon--user" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-username"
                                   value="<c:out value='${profile.username}'/>" readonly>
                        </div>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-email">Email</label>
                        <div class="profile-field__control profile-field__control--readonly">
                            <span class="profile-field__icon profile-field__icon--lock" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-email"
                                   value="<c:out value='${profile.email}'/>" readonly>
                        </div>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-roles">Vai trò</label>
                        <div class="profile-field__control profile-field__control--readonly">
                            <span class="profile-field__icon profile-field__icon--lock" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-roles"
                                   value="<c:forEach var='role' items='${profile.roles}' varStatus='loop'><c:out value='${role.name}'/>${loop.last ? '' : ', '}</c:forEach>" readonly>
                        </div>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-warehouse">Kho</label>
                        <div class="profile-field__control profile-field__control--readonly">
                            <span class="profile-field__icon profile-field__icon--lock" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-warehouse"
                                   value="<c:out value='${profile.warehouseNamesText}'/>" placeholder="Chưa gắn kho" readonly>
                        </div>
                    </div>

                    <div class="profile-field">
                        <label class="profile-field__label" for="profile-region">Địa bàn</label>
                        <div class="profile-field__control profile-field__control--readonly">
                            <span class="profile-field__icon profile-field__icon--lock" aria-hidden="true"></span>
                            <input class="profile-field__input" type="text" id="profile-region"
                                   value="<c:out value='${profile.regionNamesText}'/>" placeholder="Chưa gắn địa bàn" readonly>
                        </div>
                    </div>

                    <p class="profile-field__hint">Tài khoản, email, vai trò, kho và địa bàn do quản trị viên quản lý.</p>

                    <div class="profile-actions">
                        <button class="profile-button" type="submit" id="profile-save">Lưu thay đổi</button>
                    </div>
                </form>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/profile.js'/>"></script>
</body>
</html>
