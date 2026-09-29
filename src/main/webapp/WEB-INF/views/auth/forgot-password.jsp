<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quên mật khẩu | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card" aria-labelledby="forgot-password-title">
            <header class="auth-card__header">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="forgot-password-title">Quên mật khẩu</h1>
            </header>

            <form class="auth-form" id="forgot-password-form" action="<c:url value='/forgot-password'/>" method="post">
                <div class="auth-form__fields">
                    <c:set var="invalid" value="${not empty error}"/>
                    <c:if test="${invalid}">
                        <p class="auth-form__error" id="forgot-password-error" role="alert"><c:out value="${error}"/></p>
                    </c:if>

                    <div class="form-field">
                        <label class="form-field__label" for="email">Nhập email của bạn</label>
                        <div class="form-field__control${invalid ? ' form-field__control--invalid' : ''}">
                            <span class="form-field__icon form-field__icon--user" aria-hidden="true"></span>
                            <input class="form-field__input" type="email" id="email" name="email"
                                   value="<c:out value='${email}'/>" placeholder="ví dụ: abc123@gmail.com"
                                   autocomplete="email" maxlength="150" required autofocus
                                   aria-invalid="${invalid}"${invalid ? ' aria-describedby="forgot-password-error"' : ''}>
                        </div>
                    </div>
                </div>

                <div class="auth-form__actions">
                    <button class="auth-form__submit" type="submit" id="forgot-password-submit">Gửi liên kết</button>
                    <p class="auth-form__footer">
                        <a class="auth-form__footer-link auth-form__footer-link--regular" id="back-to-login-link" href="<c:url value='/login'/>">Quay lại trang đăng nhập</a>
                    </p>
                </div>
            </form>
        </section>
    </main>
</body>
</html>
