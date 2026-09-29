<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đã gửi liên kết khôi phục | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card" aria-labelledby="forgot-password-sent-title">
            <header class="auth-card__header">
                <span class="auth-card__status-icon">
                    <img src="<c:url value='/assets/img/icons/check-box.svg'/>" alt="" width="20" height="20">
                </span>
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="forgot-password-sent-title">Đã gửi liên kết khôi phục</h1>
                <p class="auth-card__subtitle">Chúng tôi đã gửi email hướng dẫn đặt lại mật khẩu đến địa chỉ bạn cung cấp</p>
            </header>

            <p class="auth-card__footer">
                <a class="auth-form__footer-link auth-form__footer-link--regular" id="back-to-login-link" href="<c:url value='/login'/>">Quay lại trang đăng nhập</a>
            </p>
        </section>
    </main>
</body>
</html>
