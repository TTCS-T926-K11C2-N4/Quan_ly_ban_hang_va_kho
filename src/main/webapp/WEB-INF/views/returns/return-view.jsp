<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="returns"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Chi tiết phiếu trả"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu trả <c:out value="${salesReturn.returnNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Phiếu trả <c:out value="${salesReturn.returnNo}"/>
                        <span class="invoice-pill invoice-pill--${salesReturn.statusTone}" id="return-status"><c:out value="${salesReturn.statusLabel}"/></span></h1>
                    <p class="inv-header__subtitle">Hoá đơn gốc <c:out value="${salesReturn.invoiceNo}"/> · <c:out value="${salesReturn.customerName}"/> · Lý do: <c:out value="${salesReturn.reasonLabel}"/><c:if test="${not empty salesReturn.reasonNote}"> — <c:out value="${salesReturn.reasonNote}"/></c:if></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/returns'/>">← Danh sách phiếu trả</a>
                    <c:if test="${salesReturn.pending and access.canReceive}">
                        <form action="<c:url value='/returns/receive'/>" method="post">
                            <input type="hidden" name="id" value="${salesReturn.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="return-receive">Xác nhận đã nhận hàng</button>
                        </form>
                    </c:if>
                    <c:if test="${salesReturn.status == 'RECEIVED' and not empty creditNoteId}">
                        <a class="inv-button" id="return-credit-note" href="<c:url value='/credit-notes/view'><c:param name='id' value='${creditNoteId}'/></c:url>">Xem chứng từ giảm trừ</a>
                    </c:if>
                    <c:if test="${salesReturn.status == 'RECEIVED' and empty creditNoteId and canIssueCredit}">
                        <form action="<c:url value='/credit-notes/issue'/>" method="post">
                            <input type="hidden" name="return" value="${salesReturn.id}">
                            <button class="inv-button inv-button--primary" type="submit" id="return-issue-credit">Lập chứng từ giảm trừ công nợ</button>
                        </form>
                    </c:if>
                    <c:if test="${salesReturn.pending}">
                        <form action="<c:url value='/returns/cancel'/>" method="post">
                            <input type="hidden" name="id" value="${salesReturn.id}">
                            <button class="inv-button" type="submit" id="return-cancel">Huỷ phiếu</button>
                        </form>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="return-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="return-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="return-items-title">
                <h2 class="inv-card__title" id="return-items-title">Hàng trả</h2>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="return-items">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col">Lô</th>
                                <th scope="col" class="is-number">Số lượng</th>
                                <th scope="col">Tình trạng</th>
                                <th scope="col">Nhập về kho</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${salesReturn.items}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub"><c:out value="${item.sku}"/></span></td>
                                    <td><c:out value="${item.lotNo}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${item.qty}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                    <td><c:out value="${item.conditionLabel}"/></td>
                                    <td><c:out value="${item.toWarehouseName}"/></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <p class="invoice-note">Lập bởi <c:out value="${salesReturn.createdByName}"/> lúc <c:out value="${salesReturn.createdAtText}"/>.<c:if test="${not empty salesReturn.confirmedByName}"> Nhận hàng bởi <c:out value="${salesReturn.confirmedByName}"/> lúc <c:out value="${salesReturn.confirmedAtText}"/>.</c:if></p>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
