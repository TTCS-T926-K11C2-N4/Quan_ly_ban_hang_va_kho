<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký tài khoản | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card auth-card--wide" aria-labelledby="register-title">
            <header class="auth-card__header">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="register-title">Đăng ký tài khoản</h1>
                <p class="auth-card__subtitle">Tạo tài khoản để bắt đầu sử dụng hệ thống.</p>
            </header>

            <%-- novalidate: kiểm tra bằng register.js để hiện lỗi dưới từng ô; server luôn kiểm tra lại --%>
            <form class="auth-form" id="register-form" action="<c:url value='/register'/>" method="post" novalidate>
                <div class="auth-form__fields auth-form__fields--compact">
                    <div class="form-field">
                        <label class="form-field__label" for="full-name">Họ và tên</label>
                        <div class="form-field__control${not empty errors.fullName ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--user" aria-hidden="true"></span>
                            <input class="form-field__input" type="text" id="full-name" name="fullName"
                                   value="<c:out value='${form.fullName}'/>" placeholder="Nhập họ và tên đầy đủ"
                                   maxlength="150" autocomplete="name" required autofocus
                                   aria-describedby="full-name-error" aria-invalid="${not empty errors.fullName}">
                        </div>
                        <p class="form-field__error" id="full-name-error"${empty errors.fullName ? ' hidden' : ''}><c:out value="${errors.fullName}"/></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="email">Email</label>
                        <div class="form-field__control${not empty errors.email ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--email" aria-hidden="true"></span>
                            <input class="form-field__input" type="email" id="email" name="email"
                                   value="<c:out value='${form.email}'/>" placeholder="Nhập địa chỉ email"
                                   maxlength="150" autocomplete="email" required
                                   aria-describedby="email-error" aria-invalid="${not empty errors.email}">
                        </div>
                        <p class="form-field__error" id="email-error"${empty errors.email ? ' hidden' : ''}><c:out value="${errors.email}"/></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="username">Tên đăng nhập</label>
                        <div class="form-field__control${not empty errors.username ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--user" aria-hidden="true"></span>
                            <input class="form-field__input" type="text" id="username" name="username"
                                   value="<c:out value='${form.username}'/>" placeholder="Nhập tên đăng nhập"
                                   maxlength="50" autocomplete="username" required
                                   aria-describedby="username-error" aria-invalid="${not empty errors.username}">
                        </div>
                        <p class="form-field__error" id="username-error"${empty errors.username ? ' hidden' : ''}><c:out value="${errors.username}"/></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="password">Mật khẩu</label>
                        <div class="form-field__control${not empty errors.password ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="password" name="password"
                                   placeholder="Tối thiểu 8 ký tự, có chữ và số" maxlength="64"
                                   autocomplete="new-password" required
                                   aria-describedby="password-error" aria-invalid="${not empty errors.password}">
                            <button class="form-field__toggle" type="button" data-password-toggle="password"
                                    aria-controls="password" aria-pressed="false" aria-label="Hiện mật khẩu">
                                <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                            </button>
                        </div>
                        <p class="form-field__error" id="password-error"${empty errors.password ? ' hidden' : ''}><c:out value="${errors.password}"/></p>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="confirm-password">Xác nhận mật khẩu</label>
                        <div class="form-field__control${not empty errors.confirmPassword ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="confirm-password" name="confirmPassword"
                                   placeholder="Nhập lại mật khẩu" maxlength="64" autocomplete="new-password" required
                                   aria-describedby="confirm-password-error" aria-invalid="${not empty errors.confirmPassword}">
                            <button class="form-field__toggle" type="button" data-password-toggle="confirm-password"
                                    aria-controls="confirm-password" aria-pressed="false" aria-label="Hiện mật khẩu xác nhận">
                                <img src="<c:url value='/assets/img/icons/eye-open.svg'/>" alt="" width="20" height="20">
                            </button>
                        </div>
                        <p class="form-field__error" id="confirm-password-error"${empty errors.confirmPassword ? ' hidden' : ''}><c:out value="${errors.confirmPassword}"/></p>
                    </div>

                    <div>
                        <div class="form-check${not empty errors.acceptTerms ? ' form-check--invalid' : ''}">
                            <input class="form-check__input" type="checkbox" id="accept-terms" name="acceptTerms" value="true"
                                   aria-describedby="accept-terms-error" aria-invalid="${not empty errors.acceptTerms}"
                                   required${form.termsAccepted ? ' checked' : ''}>
                            <label class="form-check__label" for="accept-terms">
                                Tôi đồng ý với <span class="form-check__highlight">Điều khoản sử dụng</span>
                                và <span class="form-check__highlight">Chính sách bảo mật</span>
                            </label>
                        </div>
                        <p class="form-field__error" id="accept-terms-error"${empty errors.acceptTerms ? ' hidden' : ''}><c:out value="${errors.acceptTerms}"/></p>
                    </div>
                </div>

                <div class="auth-form__actions">
                    <button class="auth-form__submit" type="submit" id="register-submit">Đăng ký tài khoản</button>
                    <p class="auth-form__footer auth-form__footer--divided">
                        <span>Đã có tài khoản?</span>
                        <a class="auth-form__footer-link" id="login-link" href="<c:url value='/login'/>">Đăng nhập ngay</a>
                    </p>
                </div>
            </form>
        </section>
    </main>

    <script src="<c:url value='/assets/js/password-toggle.js'/>"></script>
    <script src="<c:url value='/assets/js/register.js'/>"></script>
</body>
</html>
