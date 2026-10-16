<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="credit-notes"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Chi tiết chứng từ giảm trừ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chứng từ <c:out value="${note.creditNoteNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chứng từ giảm trừ <c:out value="${note.creditNoteNo}"/></h1>
                    <p class="inv-header__subtitle"><c:out value="${note.customerName}"/> · Hoá đơn gốc
                        <a href="<c:url value='/invoices/view'><c:param name='id' value='${note.invoiceId}'/></c:url>"><c:out value="${note.invoiceNo}"/></a>
                        · Phiếu trả <c:out value="${note.returnNo}"/> · Lập bởi <c:out value="${note.issuedByName}"/> lúc <c:out value="${note.issuedAtText}"/></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/credit-notes'/>">← Danh sách chứng từ</a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="credit-note-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="credit-note-items-title">
                <h2 class="inv-card__title" id="credit-note-items-title">Dòng giảm trừ (theo giá đã bán trên hoá đơn gốc)</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="credit-note-items">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col" class="is-number">Số lượng (đơn vị cơ sở)</th>
                                <th scope="col" class="is-number">Đơn giá bình quân</th>
                                <th scope="col" class="is-number">Giảm trừ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${note.items}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub"><c:out value="${item.sku}"/></span></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.qtyBase}" maxFractionDigits="3"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/> ₫</td>
                                    <td class="is-number"><fmt:formatNumber value="${item.amount}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <dl class="invoice-totals">
                    <div class="invoice-totals__grand"><dt>Tổng giảm trừ công nợ</dt><dd id="credit-note-total"><fmt:formatNumber value="${note.total}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
                <p class="invoice-note"><c:out value="${note.reason}"/></p>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
