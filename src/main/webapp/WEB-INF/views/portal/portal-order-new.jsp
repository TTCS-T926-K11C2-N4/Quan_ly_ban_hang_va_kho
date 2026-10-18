<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="portal-order-new"/>
<c:set var="breadcrumbSection" value="Cổng đại lý"/>
<c:set var="breadcrumbPage" value="Đặt hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đặt hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/portal.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page portal-orders portal-cart">
            <header class="page-header">
                <h1 class="page-header__title">Đặt hàng</h1>
                <p class="page-header__subtitle">Chọn mặt hàng và số lượng. Đơn gửi đi sẽ chờ nhân viên phụ trách xác nhận trước khi giao.</p>
            </header>

            <c:if test="${not empty notices}">
                <div class="sales-flash sales-flash--warning" id="portal-cart-notices" role="status">
                    <ul class="portal-cart__notices">
                        <c:forEach var="notice" items="${notices}"><li><c:out value="${notice}"/></li></c:forEach>
                    </ul>
                </div>
            </c:if>
            <c:if test="${not empty errors.form}">
                <div class="sales-flash sales-flash--error" role="alert"><c:out value="${errors.form}"/></div>
            </c:if>
            <c:if test="${not empty errors.customerId}">
                <div class="sales-flash sales-flash--error" id="portal-cart-blocked" role="alert"><c:out value="${errors.customerId}"/></div>
            </c:if>

            <c:choose>
                <c:when test="${empty products}">
                    <p class="sales-empty" id="portal-cart-empty">Hiện chưa có sản phẩm nào trong bảng giá của bạn để đặt hàng. Vui lòng liên hệ nhân viên phụ trách.</p>
                </c:when>
                <c:otherwise>
                    <form class="sales-card portal-orders__card" id="portal-cart-form" method="post" action="<c:url value='/portal/order/new'/>" novalidate>
                        <div class="portal-cart__head">
                            <div class="sales-field${not empty errors.deliveryAddressId ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="portal-address">Điểm giao<span class="sales-field__required">*</span></label>
                                <select class="sales-field__control" id="portal-address" name="deliveryAddressId" required>
                                    <option value="">Chọn điểm giao</option>
                                    <c:forEach var="address" items="${addresses}">
                                        <option value="${address.id}"${form.deliveryAddressId == address.id or (empty form.deliveryAddressId and address.defaultAddress) ? ' selected' : ''}><c:out value="${address.text}"/></option>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty errors.deliveryAddressId}"><p class="sales-field__error"><c:out value="${errors.deliveryAddressId}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.requestedDate ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="portal-date">Ngày giao mong muốn<span class="sales-field__required">*</span></label>
                                <input class="sales-field__control" type="date" id="portal-date" name="requestedDate" min="${today}"
                                       value="<c:out value='${form.requestedDate}'/>">
                                <c:if test="${not empty errors.requestedDate}"><p class="sales-field__error"><c:out value="${errors.requestedDate}"/></p></c:if>
                            </div>
                        </div>

                        <h2 class="portal-orders__title">Giỏ hàng</h2>
                        <c:if test="${not empty errors.lines}"><p class="sales-field__error" role="alert"><c:out value="${errors.lines}"/></p></c:if>
                        <div class="portal-cart__lines" id="portal-cart-lines">
                            <c:forEach var="line" items="${form.lines}" varStatus="loop">
                                <c:set var="lineKey">lines.${loop.index}</c:set>
                                <c:set var="priced" value="${quote.lines[loop.index]}"/>
                                <div class="portal-cart__line${not empty errors[lineKey] ? ' portal-cart__line--invalid' : ''}">
                                    <select class="sales-field__control portal-cart__product" name="productId" aria-label="Sản phẩm dòng ${loop.index + 1}">
                                        <option value="">Chọn sản phẩm</option>
                                        <c:forEach var="product" items="${products}">
                                            <option value="${product.id}"${line.productId == product.id ? ' selected' : ''}><c:out value="${product.sku}"/> - <c:out value="${product.name}"/></option>
                                        </c:forEach>
                                    </select>
                                    <select class="sales-field__control portal-cart__unit" name="unitId" data-selected="${line.unitId}" aria-label="Đơn vị dòng ${loop.index + 1}"></select>
                                    <input class="sales-field__control portal-cart__qty" type="text" name="qty" inputmode="decimal" maxlength="16"
                                           value="<c:out value='${line.qty}'/>" placeholder="Số lượng" aria-label="Số lượng dòng ${loop.index + 1}">
                                    <span class="portal-cart__amount">
                                        <c:if test="${not empty priced and empty priced.error}">
                                            <fmt:formatNumber value="${priced.total}" maxFractionDigits="0"/> ₫
                                        </c:if>
                                    </span>
                                    <button class="portal-cart__remove" type="button" aria-label="Xoá dòng ${loop.index + 1}">×</button>
                                    <c:if test="${not empty errors[lineKey]}"><p class="portal-cart__error"><c:out value="${errors[lineKey]}"/></p></c:if>
                                </div>
                            </c:forEach>
                        </div>
                        <button class="sales-button sales-button--soft" type="button" id="portal-add-line">+ Thêm mặt hàng</button>

                        <div class="sales-field">
                            <label class="sales-field__label" for="portal-note">Ghi chú</label>
                            <textarea class="sales-field__control" id="portal-note" name="note" rows="2" maxlength="1000"
                                      placeholder="Ghi chú cho nhân viên (nếu có)"><c:out value="${form.note}"/></textarea>
                        </div>

                        <%-- Tổng tiền và công nợ còn lại hiện sau khi bấm "Xem tổng tiền" hoặc khi gửi bị lỗi --%>
                        <c:if test="${not empty quote and not empty form.lines}">
                            <section class="portal-cart__summary" id="portal-cart-summary" aria-label="Tổng tiền và công nợ">
                                <dl class="order-totals">
                                    <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${quote.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
                                    <div><dt>Chiết khấu</dt><dd>${quote.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${quote.discount}" maxFractionDigits="0"/> ₫</dd></div>
                                    <div class="order-totals__grand"><dt>Tổng phải thu</dt><dd id="portal-cart-total"><fmt:formatNumber value="${quote.total}" maxFractionDigits="0"/> ₫</dd></div>
                                </dl>
                                <c:if test="${not empty credit}">
                                    <p class="portal-cart__credit${overCreditLimit ? ' portal-cart__credit--over' : ''}" id="portal-cart-credit">
                                        <c:choose>
                                            <c:when test="${not credit.limitGranted}">Bạn chưa được cấp hạn mức công nợ, đơn sẽ được nhân viên xem xét.</c:when>
                                            <c:when test="${overCreditLimit}">Công nợ còn lại <strong><fmt:formatNumber value="${credit.available}" maxFractionDigits="0"/> ₫</strong>; đơn này vượt hạn mức <strong><fmt:formatNumber value="${credit.getExceededBy(quote.total)}" maxFractionDigits="0"/> ₫</strong> nên cần quản lý duyệt.</c:when>
                                            <c:otherwise>Công nợ còn lại <strong><fmt:formatNumber value="${credit.available}" maxFractionDigits="0"/> ₫</strong>; sau đơn này còn <strong><fmt:formatNumber value="${credit.getAvailableAfter(quote.total)}" maxFractionDigits="0"/> ₫</strong>.</c:otherwise>
                                        </c:choose>
                                    </p>
                                </c:if>
                            </section>
                        </c:if>

                        <div class="portal-cart__actions">
                            <button class="sales-button" type="submit" name="action" value="preview" id="portal-preview">Xem tổng tiền</button>
                            <button class="sales-button sales-button--primary" type="submit" name="action" value="submit" id="portal-submit">Gửi đơn hàng</button>
                        </div>
                    </form>

                    <%-- Danh mục đơn vị của từng sản phẩm; portal-order.js nạp vào ô Đơn vị khi chọn sản phẩm --%>
                    <div id="portal-unit-catalog" hidden>
                        <c:forEach var="product" items="${products}">
                            <select data-product="${product.id}">
                                <c:forEach var="unit" items="${product.units}">
                                    <option value="${unit.id}"><c:out value="${unit.name}"/></option>
                                </c:forEach>
                            </select>
                        </c:forEach>
                    </div>
                    <template id="portal-line-template">
                        <div class="portal-cart__line">
                            <select class="sales-field__control portal-cart__product" name="productId" aria-label="Sản phẩm">
                                <option value="">Chọn sản phẩm</option>
                                <c:forEach var="product" items="${products}">
                                    <option value="${product.id}"><c:out value="${product.sku}"/> - <c:out value="${product.name}"/></option>
                                </c:forEach>
                            </select>
                            <select class="sales-field__control portal-cart__unit" name="unitId" aria-label="Đơn vị"></select>
                            <input class="sales-field__control portal-cart__qty" type="text" name="qty" inputmode="decimal" maxlength="16" placeholder="Số lượng" aria-label="Số lượng">
                            <span class="portal-cart__amount"></span>
                            <button class="portal-cart__remove" type="button" aria-label="Xoá dòng">×</button>
                        </div>
                    </template>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/portal-order.js'/>"></script>
</body>
</html>
