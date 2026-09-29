<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiên đăng nhập đã hết hạn | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
</head>
<body class="auth-page">
    <%@ include file="hero.jspf" %>

    <main class="auth-main">
        <section class="auth-card auth-card--centered" aria-labelledby="session-expired-title">
            <header class="auth-card__header">
                <img class="auth-card__illustration" src="<c:url value='/assets/img/icons/session-expired.svg'/>" alt="" width="64" height="64">
                <p class="auth-card__eyebrow">Hệ thống quản lý bán hàng &amp; kho</p>
                <h1 class="auth-card__title" id="session-expired-title">Phiên đăng nhập đã hết hạn</h1>
                <p class="auth-card__subtitle">
                    Phiên làm việc của bạn đã kết thúc vì không hoạt động.<br>
                    Vui lòng đăng nhập lại để tiếp tục sử dụng hệ thống.
                </p>
            </header>

            <a class="auth-form__submit auth-form__submit--link" id="relogin-link" href="<c:url value='/login'/>">Đăng nhập lại</a>
        </section>
    </main>
</body>
</html>
