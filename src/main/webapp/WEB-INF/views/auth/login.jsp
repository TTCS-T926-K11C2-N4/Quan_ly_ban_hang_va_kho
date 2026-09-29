<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <%-- lockSeconds: số giây còn bị khóa (Servlet gán khi users.locked_until còn hiệu lực) --%>
    <c:set var="invalid" value="${not empty error}"/>
    <c:set var="locked" value="${lockSeconds > 0}"/>

    <main class="auth-main">
        <section class="auth-card${locked ? ' auth-card--locked' : ''}" id="login-card" aria-labelledby="login-title">
            <header class="auth-card__header">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="login-title">Đăng nhập hệ thống</h1>
                <p class="auth-card__subtitle">Đăng nhập để tiếp tục quản lý hệ thống</p>
            </header>

            <form class="auth-form" id="login-form" action="<c:url value='/login'/>" method="post"
                  data-lock-seconds="<c:out value='${locked ? lockSeconds : ""}'/>">
                <div class="auth-form__fields">
                    <c:if test="${not empty success}">
                        <p class="auth-form__success" id="login-success" role="status"><c:out value="${success}"/></p>
                    </c:if>
                    <c:if test="${invalid}">
                        <p class="auth-form__error" id="login-error" role="alert"><c:out value="${error}"/></p>
                    </c:if>

                    <div class="form-field">
                        <label class="form-field__label" for="username">Tên đăng nhập hoặc Email</label>
                        <div class="form-field__control${invalid ? ' form-field__control--invalid' : ''}${locked ? ' form-field__control--disabled' : ''}">
                            <span class="form-field__icon form-field__icon--user" aria-hidden="true"></span>
                            <input class="form-field__input" type="text" id="username" name="username"
                                   value="<c:out value='${username}'/>" placeholder="ví dụ: admin"
                                   autocomplete="username" maxlength="150" required autofocus
                                   aria-invalid="${invalid}"${invalid ? ' aria-describedby="login-error"' : ''}${locked ? ' disabled' : ''}>
                        </div>
                    </div>

                    <div class="form-field">
                        <label class="form-field__label" for="password">Mật khẩu</label>
                        <div class="form-field__control${invalid ? ' form-field__control--invalid' : ''}${locked ? ' form-field__control--disabled' : ''}">
                            <span class="form-field__icon form-field__icon--lock" aria-hidden="true"></span>
                            <input class="form-field__input" type="password" id="password" name="password"
                                   placeholder="••••••••••••" autocomplete="current-password" required
                                   aria-invalid="${invalid}"${invalid ? ' aria-describedby="login-error"' : ''}${locked ? ' disabled' : ''}>
                            <button class="form-field__toggle" type="button" id="toggle-password"
                                    aria-controls="password" aria-pressed="false"${locked ? ' disabled' : ''}>
                                <span class="form-field__toggle-text" id="toggle-password-text">Hiện</span>
                                <img src="<c:url value='/assets/img/icons/eye.svg'/>" alt="" width="18" height="18">
                            </button>
                        </div>
                    </div>

                    <div class="auth-form__row">
                        <a class="auth-form__link" id="forgot-password-link" href="<c:url value='/forgot-password'/>">Quên mật khẩu?</a>
                    </div>
                </div>

                <div class="auth-form__actions">
                    <button class="auth-form__submit" type="submit" id="login-submit"${locked ? ' disabled' : ''}>Đăng nhập</button>
                    <p class="auth-form__footer">
                        <span>Chưa có tài khoản cá nhân ?</span>
                        <a class="auth-form__footer-link" id="register-link" href="<c:url value='/register'/>">Đăng ký tài khoản</a>
                    </p>
                </div>
            </form>
        </section>
    </main>

    <script src="<c:url value='/assets/js/login.js'/>"></script>
</body>
</html>
