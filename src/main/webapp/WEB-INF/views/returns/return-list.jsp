<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="returns"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Trả hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Trả hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Trả hàng từ đại lý</h1>
                    <p class="inv-header__subtitle">Phiếu trả lập từ hoá đơn gốc. Tồn kho chỉ tăng khi Quản lý kho xác nhận đã nhận hàng.</p>
                </div>
                <c:if test="${access.canCreate}">
                    <div class="inv-header__actions">
                        <a class="inv-button inv-button--primary" id="return-new" href="<c:url value='/returns/new'/>">Lập phiếu trả hàng</a>
                    </div>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="return-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="return-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="return-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="return-table-title">Phiếu trả hàng</h2>
                    <p class="inv-card__meta"><strong>${returns.size()}</strong> phiếu</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="return-table">
                        <thead>
                            <tr>
                                <th scope="col">Số phiếu</th>
                                <th scope="col">Hoá đơn gốc</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Lý do</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Lập lúc</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${returns}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/returns/view'><c:param name='id' value='${item.id}'/></c:url>"><c:out value="${item.returnNo}"/></a></td>
                                    <td><c:out value="${item.invoiceNo}"/></td>
                                    <td><c:out value="${item.customerName}"/></td>
                                    <td><c:out value="${item.reasonLabel}"/></td>
                                    <td><span class="invoice-pill invoice-pill--${item.statusTone}"><c:out value="${item.statusLabel}"/></span></td>
                                    <td><c:out value="${item.createdAtText}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty returns}">
                                <tr><td class="inv-table__empty" colspan="6">Chưa có phiếu trả hàng nào.</td></tr>
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
