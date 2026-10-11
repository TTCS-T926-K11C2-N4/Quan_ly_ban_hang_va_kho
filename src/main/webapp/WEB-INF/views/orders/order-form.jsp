<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="sales"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Tạo đơn hàng"/>
<%-- Đang mở lại đơn nháp của đại lý bị khoá: vẫn sửa/gửi được nhưng có cảnh báo (AC3 của S3-07) --%>
<c:set var="draftOfBlocked" value="${not empty draft and not empty selectedCustomer and selectedCustomer.blocked and draft.customerId == selectedCustomer.id}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tạo đơn hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page">
            <header class="page-header">
                <h1 class="page-header__title">Tạo đơn hàng</h1>
                <p class="page-header__subtitle">
                    <c:choose>
                        <c:when test="${not empty draft}">Đang gõ tiếp đơn nháp <strong><c:out value="${draft.orderNo}"/></strong>.</c:when>
                        <c:otherwise>Chọn đại lý, điểm giao và thêm sản phẩm để tạo đơn hàng.</c:otherwise>
                    </c:choose>
                </p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="order-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty errors.form}">
                <div class="sales-flash sales-flash--error" role="alert"><c:out value="${errors.form}"/></div>
            </c:if>
            <c:if test="${not empty errors and empty errors.form}">
                <div class="sales-flash sales-flash--error" role="alert">Đơn hàng chưa được lưu. Vui lòng kiểm tra các ô báo lỗi bên dưới.</div>
            </c:if>
            <div class="sales-flash sales-flash--warning" id="blocked-warning" role="status"${draftOfBlocked ? '' : ' hidden'}>
                <strong>Đại lý đang bị khoá giao dịch.</strong>
                <span id="blocked-warning-text"><c:if test="${draftOfBlocked}">Lý do: <c:out value="${selectedCustomer.blockReason}"/>. Đơn nháp này vẫn được xử lý tiếp, nhưng không tạo được đơn mới cho đại lý.</c:if></span>
            </div>

            <form class="sales-card order-form" id="order-form" method="post" action="<c:url value='/orders/new'/>" novalidate
                  data-quote-url="<c:url value='/orders/quote'/>" data-customer-url="<c:url value='/orders/customer-data'/>"
                  data-draft-customer="${draft.customerId}">
                <c:if test="${not empty draft}">
                    <input type="hidden" name="draftId" value="${draft.form.draftId}">
                    <input type="hidden" name="version" value="<c:out value='${form.version}'/>">
                </c:if>

                <div>
                    <h2 class="sales-card__title">Thông tin đơn hàng</h2>
                    <p class="sales-card__subtitle">Nhập đầy đủ thông tin trước khi lưu hoặc xác nhận đơn hàng.</p>
                </div>

                <div class="sales-field__row order-form__info">
                    <div class="sales-field${not empty errors.customerId ? ' sales-field--invalid' : ''}">
                        <label class="sales-field__label" for="order-customer">Đại lý<span class="sales-field__required">*</span></label>
                        <select class="sales-field__control" id="order-customer" name="customerId" required
                                aria-describedby="order-customer-error" aria-invalid="${not empty errors.customerId}">
                            <option value="">Chọn đại lý</option>
                            <c:forEach var="customer" items="${customers}">
                                <%-- Đại lý bị khoá chỉ chọn được khi đang mở lại đơn nháp của chính đại lý đó --%>
                                <option value="${customer.id}"${form.customerId == customer.id ? ' selected' : ''}${customer.blocked and draft.customerId != customer.id ? ' disabled' : ''}><c:out value="${customer.name}"/> (<c:out value="${customer.code}"/>)${customer.blocked ? ' — đang khoá giao dịch' : ''}</option>
                            </c:forEach>
                        </select>
                        <p class="sales-field__error" id="order-customer-error"${empty errors.customerId ? ' hidden' : ''}><c:out value="${errors.customerId}"/></p>
                        <c:if test="${empty customers}">
                            <p class="sales-field__hint">Chưa có đại lý nào trong phạm vi bạn phụ trách.</p>
                        </c:if>
                    </div>

                    <div class="sales-field${not empty errors.deliveryAddressId ? ' sales-field--invalid' : ''}">
                        <label class="sales-field__label" for="order-address">Điểm giao<span class="sales-field__required">*</span></label>
                        <select class="sales-field__control" id="order-address" name="deliveryAddressId" required
                                aria-describedby="order-address-error" aria-invalid="${not empty errors.deliveryAddressId}">
                            <option value="">Chọn điểm giao</option>
                            <c:forEach var="address" items="${addresses}">
                                <option value="${address.id}"${form.deliveryAddressId == address.id or (empty form.deliveryAddressId and address.defaultAddress) ? ' selected' : ''}><c:out value="${address.text}"/></option>
                            </c:forEach>
                        </select>
                        <p class="sales-field__error" id="order-address-error"${empty errors.deliveryAddressId ? ' hidden' : ''}><c:out value="${errors.deliveryAddressId}"/></p>
                    </div>

                    <div class="sales-field${not empty errors.requestedDate ? ' sales-field--invalid' : ''}">
                        <label class="sales-field__label" for="order-date">Ngày giao<span class="sales-field__required">*</span></label>
                        <input class="sales-field__control" type="date" id="order-date" name="requestedDate" min="${today}"
                               value="<c:out value='${form.requestedDate}'/>" aria-describedby="order-date-error"
                               aria-invalid="${not empty errors.requestedDate}">
                        <p class="sales-field__error" id="order-date-error"${empty errors.requestedDate ? ' hidden' : ''}><c:out value="${errors.requestedDate}"/></p>
                    </div>
                </div>

                <section class="order-lines" aria-labelledby="order-lines-title">
                    <div class="section-head">
                        <h3 class="section-head__title order-lines__title" id="order-lines-title">Danh sách hàng hóa</h3>
                        <button class="sales-button sales-button--soft sales-button--small" type="button" id="add-order-line">
                            <img class="sales-button__icon" src="<c:url value='/assets/img/icons/plus.svg'/>" alt="" width="14" height="14">
                            Thêm dòng hàng
                        </button>
                    </div>
                    <c:if test="${not empty errors.lines}">
                        <p class="sales-field__error" role="alert"><c:out value="${errors.lines}"/></p>
                    </c:if>

                    <div class="order-table">
                        <table class="order-table__table">
                            <thead>
                                <tr>
                                    <th scope="col" class="order-table__index">STT</th>
                                    <th scope="col" class="order-table__product">Mã hàng / Tên hàng</th>
                                    <th scope="col" class="order-table__unit">Đơn vị tính</th>
                                    <th scope="col" class="order-table__qty">Số lượng</th>
                                    <th scope="col">Tiền hàng</th>
                                    <th scope="col">Chiết khấu</th>
                                    <th scope="col">Thành tiền</th>
                                    <th scope="col" class="order-table__action"><span class="visually-hidden">Xoá dòng</span></th>
                                </tr>
                            </thead>
                            <tbody id="order-lines">
                                <c:forEach var="line" items="${form.lines}" varStatus="loop">
                                    <c:set var="lineKey">lines.${loop.index}</c:set>
                                    <c:set var="priced" value="${quote.lines[loop.index]}"/>
                                    <c:set var="lineProduct" value="${productsById[line.productId]}"/>
                                    <tr class="order-line${not empty errors[lineKey] ? ' order-line--invalid' : ''}">
                                        <td class="order-table__index order-line__index" data-label="STT"><fmt:formatNumber value="${loop.index + 1}" minIntegerDigits="2"/></td>
                                        <td class="order-table__product" data-label="Mã hàng / Tên hàng">
                                            <input type="hidden" class="order-line__product-id" name="productId" value="<c:out value='${line.productId}'/>">
                                            <input class="order-line__control order-line__product" type="text" name="productText" list="order-products"
                                                   value="<c:out value='${line.productText}'/>" placeholder="Tìm mã hàng hoặc tên hàng" autocomplete="off"
                                                   aria-label="Mã hàng hoặc tên hàng dòng ${loop.index + 1}">
                                            <p class="order-line__error"${empty errors[lineKey] ? ' hidden' : ''}><c:out value="${errors[lineKey]}"/></p>
                                        </td>
                                        <td class="order-table__unit" data-label="Đơn vị tính">
                                            <select class="order-line__control order-line__unit" name="unitId" aria-label="Đơn vị tính dòng ${loop.index + 1}">
                                                <c:forEach var="unit" items="${lineProduct.units}">
                                                    <option value="${unit.id}"${line.unitId == unit.id ? ' selected' : ''}><c:out value="${unit.name}"/></option>
                                                </c:forEach>
                                            </select>
                                        </td>
                                        <td class="order-table__qty" data-label="Số lượng">
                                            <input class="order-line__control order-line__qty" type="text" name="qty" inputmode="decimal" maxlength="16"
                                                   value="<c:out value='${line.qty}'/>" placeholder="0" aria-label="Số lượng dòng ${loop.index + 1}">
                                        </td>
                                        <td class="order-line__amount" data-label="Tiền hàng">${empty priced or not empty priced.error ? '—' : ''}<c:if test="${not empty priced and empty priced.error}"><fmt:formatNumber value="${priced.amount}" maxFractionDigits="0"/> ₫</c:if></td>
                                        <td class="order-line__discount" data-label="Chiết khấu">${empty priced or not empty priced.error ? '—' : ''}<c:if test="${not empty priced and empty priced.error}"><fmt:formatNumber value="${priced.discount}" maxFractionDigits="0"/> ₫</c:if></td>
                                        <td class="order-line__total" data-label="Thành tiền">${empty priced or not empty priced.error ? '—' : ''}<c:if test="${not empty priced and empty priced.error}"><fmt:formatNumber value="${priced.total}" maxFractionDigits="0"/> ₫</c:if></td>
                                        <td class="order-table__action">
                                            <button class="order-line__remove" type="button" aria-label="Xoá dòng ${loop.index + 1}" title="Xoá dòng">
                                                <img src="<c:url value='/assets/img/icons/trash.svg'/>" alt="" width="14" height="14">
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        <p class="order-table__empty" id="order-lines-empty"${empty form.lines ? '' : ' hidden'}>Chưa có dòng hàng nào. Bấm "Thêm dòng hàng" rồi tìm theo mã hoặc tên hàng.</p>
                    </div>
                </section>

                <div class="order-form__bottom">
                    <div class="sales-field order-form__note${not empty errors.note ? ' sales-field--invalid' : ''}">
                        <label class="sales-field__label" for="order-note">Ghi chú đơn hàng</label>
                        <textarea class="sales-field__control sales-field__control--textarea" id="order-note" name="note" maxlength="1000"
                                  placeholder="Nhập ghi chú nếu có..."><c:out value="${form.note}"/></textarea>
                        <c:if test="${not empty errors.note}"><p class="sales-field__error"><c:out value="${errors.note}"/></p></c:if>
                    </div>

                    <dl class="order-summary" aria-live="polite">
                        <div class="order-summary__row">
                            <dt>Tiền hàng</dt>
                            <dd id="order-subtotal"><fmt:formatNumber value="${empty quote ? 0 : quote.subtotal}" maxFractionDigits="0"/> ₫</dd>
                        </div>
                        <div class="order-summary__row">
                            <dt>Chiết khấu</dt>
                            <dd id="order-discount">${not empty quote and quote.discount > 0 ? '-' : ''}<fmt:formatNumber value="${empty quote ? 0 : quote.discount}" maxFractionDigits="0"/> ₫</dd>
                        </div>
                        <div class="order-summary__row order-summary__row--total">
                            <dt>Tổng phải thu ngay</dt>
                            <dd id="order-total"><fmt:formatNumber value="${empty quote ? 0 : quote.total}" maxFractionDigits="0"/> ₫</dd>
                        </div>
                    </dl>
                </div>

                <div class="sales-actions order-form__actions">
                    <a class="sales-button" href="<c:url value='${currentUser.homePath}'/>">Hủy</a>
                    <button class="sales-button sales-button--soft" type="submit" name="action" value="draft" id="save-draft">
                        <img class="sales-button__icon" src="<c:url value='/assets/img/icons/save.svg'/>" alt="" width="15" height="15">
                        Lưu nháp
                    </button>
                    <button class="sales-button" type="button" id="open-drafts" aria-haspopup="dialog">Mở lại</button>
                    <button class="sales-button sales-button--primary" type="submit" name="action" value="submit" id="submit-order">
                        Tạo đơn hàng
                        <img class="sales-button__icon" src="<c:url value='/assets/img/icons/arrow-right.svg'/>" alt="" width="15" height="15">
                    </button>
                </div>
            </form>

            <%-- Gợi ý khi gõ mã/tên hàng; orders.js dò productId theo đúng chữ của gợi ý --%>
            <datalist id="order-products">
                <c:forEach var="product" items="${products}">
                    <option value="<c:out value='${product.label}'/>" data-id="${product.id}"></option>
                </c:forEach>
            </datalist>
            <%-- Đơn vị tính đã khai báo của từng sản phẩm; orders.js chép vào ô Đơn vị tính khi chọn hàng --%>
            <div id="order-unit-catalog" hidden>
                <c:forEach var="product" items="${products}">
                    <select data-product="${product.id}" aria-hidden="true" tabindex="-1">
                        <c:forEach var="unit" items="${product.units}">
                            <option value="${unit.id}"><c:out value="${unit.name}"/></option>
                        </c:forEach>
                    </select>
                </c:forEach>
            </div>

            <template id="order-line-template">
                <tr class="order-line">
                    <td class="order-table__index order-line__index" data-label="STT"></td>
                    <td class="order-table__product" data-label="Mã hàng / Tên hàng">
                        <input type="hidden" class="order-line__product-id" name="productId" value="">
                        <input class="order-line__control order-line__product" type="text" name="productText" list="order-products"
                               placeholder="Tìm mã hàng hoặc tên hàng" autocomplete="off" aria-label="Mã hàng hoặc tên hàng">
                        <p class="order-line__error" hidden></p>
                    </td>
                    <td class="order-table__unit" data-label="Đơn vị tính">
                        <select class="order-line__control order-line__unit" name="unitId" aria-label="Đơn vị tính"></select>
                    </td>
                    <td class="order-table__qty" data-label="Số lượng">
                        <input class="order-line__control order-line__qty" type="text" name="qty" inputmode="decimal" maxlength="16"
                               placeholder="0" aria-label="Số lượng">
                    </td>
                    <td class="order-line__amount" data-label="Tiền hàng">—</td>
                    <td class="order-line__discount" data-label="Chiết khấu">—</td>
                    <td class="order-line__total" data-label="Thành tiền">—</td>
                    <td class="order-table__action">
                        <button class="order-line__remove" type="button" aria-label="Xoá dòng" title="Xoá dòng">
                            <img src="<c:url value='/assets/img/icons/trash.svg'/>" alt="" width="14" height="14">
                        </button>
                    </td>
                </tr>
            </template>

            <%-- "Mở lại": chọn đơn nháp đã lưu để gõ tiếp (AC4 của S3-09) --%>
            <dialog class="order-drafts" id="order-drafts" aria-labelledby="order-drafts-title">
                <div class="order-drafts__head">
                    <h2 class="sales-panel__title" id="order-drafts-title">Mở lại đơn nháp</h2>
                    <button class="order-drafts__close" type="button" id="close-drafts" aria-label="Đóng">×</button>
                </div>
                <c:choose>
                    <c:when test="${empty drafts}">
                        <p class="sales-empty">Chưa có đơn nháp nào.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="order-drafts__list">
                            <c:forEach var="item" items="${drafts}">
                                <li>
                                    <a class="order-drafts__item${draft.form.draftId == item.id ? ' order-drafts__item--current' : ''}"
                                       href="<c:url value='/orders/new'><c:param name='draft' value='${item.id}'/></c:url>">
                                        <span class="order-drafts__no"><c:out value="${item.orderNo}"/></span>
                                        <span class="order-drafts__customer"><c:out value="${item.customerName}"/><c:if test="${item.customerBlocked}"> · đang khoá giao dịch</c:if></span>
                                        <span class="order-drafts__meta">Sửa lúc <c:out value="${item.updatedAtText}"/> · <fmt:formatNumber value="${item.total}" maxFractionDigits="0"/> ₫</span>
                                    </a>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </dialog>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/orders.js'/>"></script>
</body>
</html>
