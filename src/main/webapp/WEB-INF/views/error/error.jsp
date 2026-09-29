<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Đã đăng nhập: dùng giao diện ứng dụng (sidebar, topbar). Chưa đăng nhập: dùng giao diện trang đăng nhập. --%>
<c:set var="breadcrumbSection" value="Hệ thống"/>
<c:set var="breadcrumbPage" value="${errorTitle}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${errorTitle}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <c:choose>
        <c:when test="${not empty currentUser}">
            <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
            <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
        </c:when>
        <c:otherwise>
            <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap">
            <link rel="stylesheet" href="<c:url value='/assets/css/auth.css'/>">
        </c:otherwise>
    </c:choose>
</head>
<c:choose>
    <c:when test="${not empty currentUser}">
        <body class="app-page">
            <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

            <div class="app-shell">
                <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

                <main class="app-content">
                    <section class="panel error-state" aria-labelledby="error-title">
                        <p class="error-state__code" aria-hidden="true">${errorStatus}</p>
                        <h1 class="error-state__title" id="error-title"><c:out value="${errorTitle}"/></h1>
                        <p class="error-state__message"><c:out value="${errorMessage}"/></p>
                        <div class="error-state__actions">
                            <a class="error-state__button" id="error-home-link" href="<c:url value='${currentUser.homePath}'/>">Về trang chủ</a>
                            <c:if test="${errorStatus == 403}">
                                <form action="<c:url value='/logout'/>" method="post">
                                    <button class="error-state__button error-state__button--secondary" type="submit" id="error-switch-account">Đăng nhập tài khoản khác</button>
                                </form>
                            </c:if>
                        </div>
                    </section>
                </main>
            </div>

            <script src="<c:url value='/assets/js/app.js'/>"></script>
        </body>
    </c:when>
    <c:otherwise>
        <body class="auth-page">
            <%@ include file="/WEB-INF/views/auth/hero.jspf" %>

            <main class="auth-main">
                <section class="auth-card auth-card--centered" aria-labelledby="error-title">
                    <header class="auth-card__header">
                        <p class="auth-card__eyebrow">Lỗi ${errorStatus}</p>
                        <h1 class="auth-card__title" id="error-title"><c:out value="${errorTitle}"/></h1>
                        <p class="auth-card__subtitle"><c:out value="${errorMessage}"/></p>
                    </header>

                    <a class="auth-form__submit auth-form__submit--link" id="error-login-link" href="<c:url value='/login'/>">Về trang đăng nhập</a>
                </section>
            </main>
        </body>
    </c:otherwise>
</c:choose>
</html>
