<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="returns"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Lập phiếu trả hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lập phiếu trả hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Lập phiếu trả hàng</h1>
                    <p class="inv-header__subtitle">
                        <c:choose>
                            <c:when test="${empty invoiceId}">Chọn hoá đơn gốc của lô hàng cần trả.</c:when>
                            <c:otherwise>Hoá đơn gốc <strong><c:out value="${invoiceNo}"/></strong>. Hàng hỏng nhập vào kho hàng lỗi, hàng tốt nhập lại đúng lô đã giao.</c:otherwise>
                        </c:choose>
                    </p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/returns'/>">← Danh sách phiếu trả</a>
                </div>
            </header>

            <c:if test="${not empty formError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="return-error" role="alert"><c:out value="${formError}"/></div>
            </c:if>

            <c:choose>
                <c:when test="${empty invoiceId}">
                    <section class="inv-card" aria-labelledby="return-pick-title">
                        <h2 class="inv-card__title" id="return-pick-title">Chọn hoá đơn gốc</h2>
                        <div class="inv-table-scroll">
                            <table class="inv-table" id="return-invoices">
                                <thead>
                                    <tr>
                                        <th scope="col">Số hoá đơn</th>
                                        <th scope="col">Đại lý</th>
                                        <th scope="col">Ngày lập</th>
                                        <th scope="col"><span class="visually-hidden">Chọn</span></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="option" items="${invoices}">
                                        <tr>
                                            <td><c:out value="${option.invoiceNo}"/></td>
                                            <td><c:out value="${option.customerName}"/></td>
                                            <td><c:out value="${option.issueDate}"/></td>
                                            <td><a class="inv-button" href="<c:url value='/returns/new'><c:param name='invoice' value='${option.id}'/></c:url>">Chọn</a></td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty invoices}">
                                        <tr><td class="inv-table__empty" colspan="4">Chưa có hoá đơn nào để trả hàng.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </section>
                </c:when>
                <c:otherwise>
                    <form action="<c:url value='/returns/new'/>" method="post" id="return-form">
                        <input type="hidden" name="invoice" value="${invoiceId}">
                        <section class="inv-card" aria-labelledby="return-lines-title">
                            <h2 class="inv-card__title" id="return-lines-title">Dòng hàng trả</h2>
                            <p class="inv-card__subtitle">Nhập số lượng muốn trả (để trống nếu không trả dòng đó). Không vượt số còn trả được.</p>
                            <div class="inv-table-scroll">
                                <table class="inv-table" id="return-lines">
                                    <thead>
                                        <tr>
                                            <th scope="col">Sản phẩm</th>
                                            <th scope="col">Lô</th>
                                            <th scope="col" class="is-number">Đã giao</th>
                                            <th scope="col" class="is-number">Đã trả</th>
                                            <th scope="col" class="is-number">Còn trả được</th>
                                            <th scope="col">Số lượng trả</th>
                                            <th scope="col">Tình trạng</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="line" items="${lines}">
                                            <tr>
                                                <td><span class="inv-table__main"><c:out value="${line.productName}"/></span><span class="inv-table__sub"><c:out value="${line.sku}"/></span></td>
                                                <td><c:out value="${line.lotNo}"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${line.qty}" maxFractionDigits="3"/> <c:out value="${line.unitName}"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${line.returned}" maxFractionDigits="3"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${line.returnable}" maxFractionDigits="3"/></td>
                                                <td>
                                                    <input type="hidden" name="item" value="${line.invoiceItemId}">
                                                    <input class="inv-control" type="text" inputmode="decimal" name="qty_${line.invoiceItemId}" aria-label="Số lượng trả ${line.sku}"${line.returnableBase.signum() == 0 ? ' disabled' : ''}>
                                                </td>
                                                <td>
                                                    <select class="inv-control" name="condition_${line.invoiceItemId}" aria-label="Tình trạng ${line.sku}">
                                                        <option value="GOOD">Hàng tốt</option>
                                                        <option value="DAMAGED">Hàng hỏng</option>
                                                    </select>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </section>

                        <section class="inv-card" aria-labelledby="return-reason-title">
                            <h2 class="inv-card__title" id="return-reason-title">Lý do trả hàng</h2>
                            <div class="invoice-filters">
                                <div class="inv-field">
                                    <label class="inv-field__label" for="return-reason">Lý do</label>
                                    <select class="inv-control" id="return-reason" name="reason" required>
                                        <option value="">— Chọn lý do —</option>
                                        <c:forEach var="entry" items="${reasons}">
                                            <option value="${entry.key}"${reason == entry.key ? ' selected' : ''}><c:out value="${entry.value}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                            <div class="invoice-issue">
                                <input class="inv-control" type="text" name="note" maxlength="1000" placeholder="Ghi chú (bắt buộc nếu chọn lý do khác)" aria-label="Ghi chú" value="<c:out value='${note}'/>">
                                <button class="inv-button inv-button--primary" type="submit" id="return-submit">Lập phiếu trả hàng</button>
                            </div>
                        </section>
                    </form>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
