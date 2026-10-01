<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%-- Không gửi token trong Referer khi người dùng bấm sang trang khác --%>
    <meta name="referrer" content="no-referrer">
    <title>Đặt lại mật khẩu | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card" aria-labelledby="reset-password-title">
            <header class="auth-card__header">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="reset-password-title">Đặt lại mật khẩu</h1>
                <c:if test="${tokenValid}">
                    <p class="auth-card__subtitle">Nhập mật khẩu mới cho tài khoản của bạn</p>
                </c:if>
            </header>

            <c:choose>
                <c:when test="${tokenValid}">
                    <form class="auth-form" id="reset-password-form" action="<c:url value='/reset-password'/>" method="post">
                        <input type="hidden" name="token" value="<c:out value='${token}'/>">
                        <div class="auth-form__fields">
                            <c:if test="${not empty error}">
                                <p class="auth-form__error" id="reset-password-error" role="alert"><c:out value="${error}"/></p>
                            </c:if>

                            <div class="form-field">
                                <label class="form-field__label" for="newPassword">Mật khẩu mới</label>
                                <div class="form-field__control">
                                    <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                                    <input class="form-field__input" type="password" id="newPassword" name="newPassword"
                                           autocomplete="new-password" minlength="8" maxlength="64"
                                           aria-describedby="new-password-hint" required autofocus>
                                    <button class="form-field__toggle" type="button" data-password-toggle="newPassword"
                                            aria-controls="newPassword" aria-pressed="false" aria-label="Hiện mật khẩu mới">
                                        <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                                    </button>
                                </div>
                                <p class="form-field__hint" id="new-password-hint">Tối thiểu 8 ký tự, có chữ và số</p>
                            </div>

                            <div class="form-field">
                                <label class="form-field__label" for="confirmPassword">Xác nhận mật khẩu mới</label>
                                <div class="form-field__control">
                                    <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                                    <input class="form-field__input" type="password" id="confirmPassword" name="confirmPassword"
                                           autocomplete="new-password" maxlength="64" required>
                                    <button class="form-field__toggle" type="button" data-password-toggle="confirmPassword"
                                            aria-controls="confirmPassword" aria-pressed="false" aria-label="Hiện mật khẩu xác nhận">
                                        <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div class="auth-form__actions">
                            <button class="auth-form__submit" type="submit" id="reset-password-submit">Đặt lại mật khẩu</button>
                            <p class="auth-form__footer">
                                <a class="auth-form__footer-link auth-form__footer-link--regular" id="back-to-login-link" href="<c:url value='/login'/>">Quay lại trang đăng nhập</a>
                            </p>
                        </div>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="auth-form">
                        <div class="auth-form__fields">
                            <p class="auth-form__error" id="reset-link-invalid" role="alert">
                                Liên kết đặt lại mật khẩu không hợp lệ, đã được dùng hoặc đã hết hạn.
                            </p>
                        </div>
                        <div class="auth-form__actions">
                            <a class="auth-form__submit auth-form__submit--link" id="request-new-link" href="<c:url value='/forgot-password'/>">Gửi lại liên kết mới</a>
                            <p class="auth-form__footer">
                                <a class="auth-form__footer-link auth-form__footer-link--regular" id="back-to-login-link" href="<c:url value='/login'/>">Quay lại trang đăng nhập</a>
                            </p>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>

    <script src="<c:url value='/assets/js/password-toggle.js'/>"></script>
</body>
</html>
