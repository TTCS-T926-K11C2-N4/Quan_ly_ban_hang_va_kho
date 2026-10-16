<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="adjustments"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Chi tiết phiếu điều chỉnh"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu <c:out value="${adjustment.adjustmentNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Phiếu <c:out value="${adjustment.adjustmentNo}"/>
                        <span class="invoice-pill invoice-pill--${adjustment.statusTone}" id="adjustment-status"><c:out value="${adjustment.statusLabel}"/></span></h1>
                    <p class="inv-header__subtitle"><c:out value="${adjustment.warehouseName}"/> · <c:out value="${adjustment.reasonLabel}"/> · Lập bởi <c:out value="${adjustment.createdByName}"/> lúc <c:out value="${adjustment.createdAtText}"/><c:if test="${not empty adjustment.approvedByName}"> · Xử lý bởi <c:out value="${adjustment.approvedByName}"/> lúc <c:out value="${adjustment.approvedAtText}"/></c:if></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/adjustments'/>">← Danh sách phiếu</a>
                    <c:if test="${adjustment.pending and canDecide}">
                        <form action="<c:url value='/adjustments/approve'/>" method="post">
                            <input type="hidden" name="id" value="${adjustment.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="adjustment-approve">Duyệt, trừ tồn</button>
                        </form>
                        <form class="invoice-pending-action" action="<c:url value='/adjustments/reject'/>" method="post">
                            <input type="hidden" name="id" value="${adjustment.id}">
                            <input class="inv-control" type="text" name="reason" maxlength="1000" placeholder="Lý do từ chối" aria-label="Lý do từ chối" required>
                            <button class="inv-button" type="submit" id="adjustment-reject">Từ chối</button>
                        </form>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="adjustment-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="adjustment-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="adjustment-items-title">
                <h2 class="inv-card__title" id="adjustment-items-title">Dòng điều chỉnh</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="adjustment-items">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col">Lô</th>
                                <th scope="col" class="is-number">Số lượng thay đổi</th>
                                <th scope="col" class="is-number">Giá trị tổn thất</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${adjustment.items}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub"><c:out value="${item.sku}"/></span></td>
                                    <td><c:out value="${item.lotNo}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.qtyBase}" maxFractionDigits="3"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.lossValue}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <dl class="invoice-totals">
                    <div class="invoice-totals__grand"><dt>Tổng giá trị tổn thất</dt><dd id="adjustment-total"><fmt:formatNumber value="${adjustment.totalLoss}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
                <p class="invoice-note"><strong>Lý do:</strong> <c:out value="${adjustment.note}"/></p>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
