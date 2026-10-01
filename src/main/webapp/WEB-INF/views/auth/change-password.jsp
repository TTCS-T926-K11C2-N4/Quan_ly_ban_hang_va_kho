<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đổi mật khẩu | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card" aria-labelledby="change-password-title">
            <header class="auth-card__header">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="change-password-title">Đổi mật khẩu</h1>
                <p class="auth-card__subtitle">Quản lý mật khẩu để bảo mật tài khoản của bạn</p>
            </header>

            <%-- novalidate: kiểm tra bằng change-password.js để hiện lỗi ngay dưới từng ô --%>
            <form class="auth-form" id="change-password-form" action="<c:url value='/change-password'/>" method="post" novalidate>
                <div class="auth-form__fields">
                    <c:if test="${not empty success}">
                        <p class="auth-form__success" id="change-password-success" role="status"><c:out value="${success}"/></p>
                    </c:if>
                    <c:if test="${not empty error}">
                        <p class="auth-form__error" id="change-password-error" role="alert"><c:out value="${error}"/></p>
                    </c:if>

                    <div class="form-field">
                        <label class="form-field__label" for="currentPassword">Mật khẩu hiện tại</label>
                        <div class="form-field__control">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="currentPassword" name="currentPassword"
                                   autocomplete="current-password" aria-describedby="current-password-error" required autofocus>
                            <button class="form-field__toggle" type="button" data-password-toggle="currentPassword"
                                    aria-controls="currentPassword" aria-pressed="false" aria-label="Hiện mật khẩu hiện tại">
                                <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                            </button>
                        </div>
                        <p class="form-field__error" id="current-password-error" hidden></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="newPassword">Mật khẩu mới</label>
                        <div class="form-field__control">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="newPassword" name="newPassword"
                                   autocomplete="new-password" minlength="8"
                                   aria-describedby="new-password-hint new-password-error" required>
                            <button class="form-field__toggle" type="button" data-password-toggle="newPassword"
                                    aria-controls="newPassword" aria-pressed="false" aria-label="Hiện mật khẩu mới">
                                <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                            </button>
                        </div>
                        <p class="form-field__hint" id="new-password-hint">Tối thiểu 8 ký tự, có chữ và số</p>
                        <p class="form-field__error" id="new-password-error" hidden></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="confirmPassword">Xác nhận mật khẩu mới</label>
                        <div class="form-field__control">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="confirmPassword" name="confirmPassword"
                                   autocomplete="new-password" aria-describedby="confirm-password-error" required>
                            <button class="form-field__toggle" type="button" data-password-toggle="confirmPassword"
                                    aria-controls="confirmPassword" aria-pressed="false" aria-label="Hiện mật khẩu xác nhận">
                                <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                            </button>
                        </div>
                        <p class="form-field__error" id="confirm-password-error" hidden></p>
                    </div>
                </div>

                <div class="auth-form__actions">
                    <button class="auth-form__submit" type="submit" id="change-password-submit">Lưu thay đổi</button>
                    <p class="auth-form__footer">
                        <%-- Đang dùng mật khẩu tạm thì chưa vào được trang khác, chỉ có thể đăng xuất --%>
                        <c:choose>
                            <c:when test="${currentUser.mustChangePassword}">
                                <button class="auth-form__footer-link auth-form__footer-link--regular auth-form__footer-link--button"
                                        type="submit" id="back-to-login-button" form="logout-form">Quay lại trang đăng nhập</button>
                            </c:when>
                            <c:otherwise>
                                <a class="auth-form__footer-link auth-form__footer-link--regular" id="back-to-dashboard-link"
                                   href="<c:url value='${currentUser.homePath}'/>">Quay lại trang chủ</a>
                            </c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </form>

            <%-- Form riêng để không gửi kèm các ô mật khẩu khi đăng xuất --%>
            <form id="logout-form" action="<c:url value='/logout'/>" method="post" hidden></form>
        </section>
    </main>

    <script src="<c:url value='/assets/js/change-password.js'/>"></script>
</body>
</html>
