<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="adjustments"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Điều chỉnh tồn kho"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Điều chỉnh tồn kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/invoices.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Điều chỉnh tồn kho</h1>
                    <p class="inv-header__subtitle">Xử lý hàng hỏng, hàng mất, hàng hết hạn phải huỷ. Người duyệt là Quản lý kho và không được là người lập phiếu.</p>
                </div>
                <c:if test="${canCreate}">
                    <div class="inv-header__actions">
                        <a class="inv-button inv-button--primary" id="adjustment-new" href="<c:url value='/adjustments/new'/>">Lập phiếu điều chỉnh</a>
                    </div>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="adjustment-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="adjustment-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="adjustment-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="adjustment-table-title">Phiếu điều chỉnh</h2>
                    <p class="inv-card__meta"><strong>${adjustments.size()}</strong> phiếu</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="adjustment-table">
                        <thead>
                            <tr>
                                <th scope="col">Số phiếu</th>
                                <th scope="col">Kho</th>
                                <th scope="col">Lý do</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Người lập</th>
                                <th scope="col" class="is-number">Giá trị tổn thất</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${adjustments}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/adjustments/view'><c:param name='id' value='${item.id}'/></c:url>"><c:out value="${item.adjustmentNo}"/></a></td>
                                    <td><c:out value="${item.warehouseName}"/></td>
                                    <td><c:out value="${item.reasonLabel}"/></td>
                                    <td><span class="invoice-pill invoice-pill--${item.statusTone}"><c:out value="${item.statusLabel}"/></span></td>
                                    <td><c:out value="${item.createdByName}"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${item.totalLoss}" maxFractionDigits="0"/> ₫</strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty adjustments}">
                                <tr><td class="inv-table__empty" colspan="6">Chưa có phiếu điều chỉnh nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
