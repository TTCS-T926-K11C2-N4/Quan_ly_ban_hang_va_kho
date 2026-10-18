<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="invoices"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Chi tiết hoá đơn"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hoá đơn <c:out value="${invoice.invoiceNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Hoá đơn <c:out value="${invoice.invoiceNo}"/>
                        <span class="invoice-pill invoice-pill--${invoice.statusTone}" id="invoice-status"><c:out value="${invoice.statusLabel}"/></span></h1>
                    <p class="inv-header__subtitle"><c:out value="${invoice.customerName}"/> (<c:out value="${invoice.customerCode}"/>)
                        · Lập ngày <c:out value="${invoice.issueDateText}"/> · Hạn thanh toán <c:out value="${invoice.dueDateText}"/>
                        (<c:out value="${invoice.paymentTermDays}"/> ngày)</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/invoices'/>">← Danh sách hoá đơn</a>
                    <a class="inv-button inv-button--primary" id="invoice-print" target="_blank" rel="noopener" href="<c:url value='/invoices/print'><c:param name='id' value='${invoice.id}'/></c:url>">In / Xuất PDF</a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="invoice-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="invoice-items-title">
                <h2 class="inv-card__title" id="invoice-items-title">Dòng hàng (theo số lượng thực giao)</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="invoice-items">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col">Nguồn</th>
                                <th scope="col" class="is-number">Số lượng</th>
                                <th scope="col" class="is-number">Đơn giá</th>
                                <th scope="col" class="is-number">Chiết khấu</th>
                                <th scope="col" class="is-number">Thành tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${invoice.items}">
                                <tr>
                                    <td>${item.lineNo}</td>
                                    <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub"><c:out value="${item.sku}"/></span></td>
                                    <td><c:out value="${item.deliveryNo}"/><span class="inv-table__sub">Đơn <c:out value="${item.orderNo}"/></span></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.qty}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/> ₫</td>
                                    <td class="is-number"><c:choose><c:when test="${item.discount.signum() == 0}">—</c:when><c:otherwise>−<fmt:formatNumber value="${item.discount}" maxFractionDigits="0"/> ₫</c:otherwise></c:choose></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.lineTotal}" maxFractionDigits="0"/> ₫</td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <dl class="invoice-totals">
                    <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${invoice.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Chiết khấu</dt><dd>${invoice.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${invoice.discount}" maxFractionDigits="0"/> ₫</dd></div>
                    <div class="invoice-totals__grand"><dt>Tổng hoá đơn</dt><dd><fmt:formatNumber value="${invoice.total}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Đã thanh toán</dt><dd><fmt:formatNumber value="${invoice.paid}" maxFractionDigits="0"/> ₫</dd></div>
                    <div><dt>Còn nợ</dt><dd id="invoice-outstanding"><fmt:formatNumber value="${invoice.outstanding}" maxFractionDigits="0"/> ₫</dd></div>
                </dl>
                <c:if test="${not empty invoice.note}"><p class="invoice-note"><strong>Ghi chú:</strong> <c:out value="${invoice.note}"/></p></c:if>
                <p class="invoice-note">Phát hành bởi <c:out value="${invoice.issuedByName}"/> lúc <c:out value="${invoice.issuedAtText}"/>.</p>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
