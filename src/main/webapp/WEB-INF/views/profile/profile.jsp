<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="profile"/>
<c:set var="breadcrumbSection" value="Tài khoản của tôi"/>
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
    <%-- Dùng lại khung thông tin, form và nút của nhóm màn hình Quản lý tài khoản --%>
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/profile.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Hồ sơ cá nhân</h1>
                <p class="page-header__subtitle">Cập nhật họ tên và số điện thoại để kho và đồng nghiệp liên hệ được khi cần xác nhận đơn.</p>
            </header>

            <c:if test="${not empty success}">
                <div class="account-flash" id="profile-updated-message" role="status">
                    <p><c:out value="${success}"/></p>
                </div>
            </c:if>

            <section class="account-detail" aria-labelledby="profile-name">
                <div class="account-profile">
                    <%-- Ảnh đại diện (S2-03): hồ sơ luôn của người đang đăng nhập nên lấy ảnh từ currentUser --%>
                    <div class="avatar-upload">
                        <c:choose>
                            <c:when test="${currentUser.hasAvatar}">
                                <img class="account-profile__avatar avatar-upload__image" id="avatar-image" width="72" height="72"
                                     src="<c:url value='/avatar'><c:param name='id' value='${currentUser.avatarFileId}'/><c:param name='size' value='full'/></c:url>"
                                     alt="Ảnh đại diện của ${fn:escapeXml(profile.fullName)}">
                            </c:when>
                            <c:otherwise>
                                <span class="account-profile__avatar" id="avatar-initials" aria-hidden="true"><c:out value="${profile.initials}"/></span>
                                <img class="account-profile__avatar avatar-upload__image" id="avatar-image" width="72" height="72" alt="" hidden>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div>
                        <h2 class="account-profile__name" id="profile-name"><c:out value="${profile.fullName}"/></h2>
                        <p class="account-profile__username">@<c:out value="${profile.username}"/></p>
                        <div class="account-detail__roles">
                            <c:forEach var="role" items="${profile.roles}">
                                <span class="role-badge role-badge--${fn:toLowerCase(role.code)}"><c:out value="${role.name}"/></span>
                            </c:forEach>
                        </div>

                        <%-- novalidate: profile-avatar.js kiểm tra định dạng, dung lượng và hiện ảnh xem trước; server kiểm tra lại --%>
                        <form class="avatar-upload__form" id="avatar-form" method="post" enctype="multipart/form-data" novalidate
                              action="<c:url value='/profile/avatar'/>">
                            <input class="visually-hidden" type="file" id="avatar-file" name="avatar" accept="image/jpeg,image/png"
                                   aria-describedby="avatar-hint avatar-error">
                            <label class="button button--secondary avatar-upload__choose" for="avatar-file">
                                ${currentUser.hasAvatar ? 'Đổi ảnh đại diện' : 'Chọn ảnh đại diện'}
                            </label>
                            <button class="button button--primary" type="submit" id="avatar-submit" hidden>Tải ảnh lên</button>
                            <p class="form-group__hint avatar-upload__hint" id="avatar-hint">
                                JPG hoặc PNG, tối đa 2MB. Ảnh được cắt vuông ở giữa.
                            </p>
                            <p class="form-group__error avatar-upload__error" id="avatar-error" role="alert"${empty avatarError ? ' hidden' : ''}><c:out value="${avatarError}"/></p>
                        </form>
                    </div>
                </div>

                <h3 class="account-detail__title">Thông tin do quản trị viên quản lý</h3>
                <p class="account-detail__muted">Liên hệ quản trị hệ thống nếu cần đổi các thông tin này.</p>
                <dl class="detail-grid">
                    <div>
                        <dt>Tên đăng nhập</dt>
                        <dd><c:out value="${profile.username}"/></dd>
                    </div>
                    <div>
                        <dt>Email</dt>
                        <dd><c:out value="${profile.email}"/></dd>
                    </div>
                    <div>
                        <dt>Kho phụ trách</dt>
                        <dd><c:out value="${empty profile.warehouseNamesText ? '—' : profile.warehouseNamesText}"/></dd>
                    </div>
                    <div>
                        <dt>Địa bàn phụ trách</dt>
                        <dd><c:out value="${empty profile.regionNamesText ? '—' : profile.regionNamesText}"/></dd>
                    </div>
                </dl>
            </section>

            <%-- novalidate: kiểm tra bằng profile.js để hiện lỗi dưới từng ô; server luôn kiểm tra lại --%>
            <form class="account-form" id="profile-form" method="post" novalidate action="<c:url value='/profile'/>">
                <section class="account-form__section" aria-labelledby="contact-title">
                    <h2 class="account-form__section-title" id="contact-title">Thông tin liên hệ</h2>
                    <p class="account-form__section-desc">Bạn tự cập nhật được hai thông tin này.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.fullName ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="full-name">Họ và tên *</label>
                            <input class="form-group__control" type="text" id="full-name" name="fullName"
                                   value="<c:out value='${form.fullName}'/>" placeholder="Nhập họ và tên"
                                   maxlength="150" autocomplete="name" required
                                   aria-describedby="full-name-error" aria-invalid="${not empty errors.fullName}">
                            <p class="form-group__error" id="full-name-error"${empty errors.fullName ? ' hidden' : ''}><c:out value="${errors.fullName}"/></p>
                        </div>

                        <div class="form-group${not empty errors.phone ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="phone">Số điện thoại *</label>
                            <input class="form-group__control" type="tel" id="phone" name="phone"
                                   value="<c:out value='${form.phone}'/>" placeholder="Vd 0912 345 678"
                                   maxlength="20" autocomplete="tel" required
                                   aria-describedby="phone-hint phone-error" aria-invalid="${not empty errors.phone}">
                            <p class="form-group__hint" id="phone-hint">Số di động 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.</p>
                            <p class="form-group__error" id="phone-error"${empty errors.phone ? ' hidden' : ''}><c:out value="${errors.phone}"/></p>
                        </div>
                    </div>
                </section>

                <footer class="account-form__footer">
                    <button class="button button--primary" type="submit" id="save-profile-submit">Lưu thay đổi</button>
                </footer>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/profile.js'/>"></script>
    <script src="<c:url value='/assets/js/profile-avatar.js'/>"></script>
</body>
</html>
