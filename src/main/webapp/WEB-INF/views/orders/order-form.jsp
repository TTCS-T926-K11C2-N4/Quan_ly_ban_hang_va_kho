<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="sales"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Tạo đơn hàng"/>
<%-- Đang mở lại đơn nháp của đại lý bị khoá: vẫn sửa/gửi được nhưng có cảnh báo (AC3 của S3-07) --%>
<c:set var="draftOfBlocked" value="${not empty draft and not empty selectedCustomer and selectedCustomer.blocked and draft.customerId == selectedCustomer.id}"/>
<%-- S4-02 AC3: nợ quá hạn chặn đơn mới; đơn nháp đã có của đại lý thì lưu được nhưng không gửi được --%>
<c:set var="ownDraft" value="${not empty draft and not empty selectedCustomer and draft.customerId == selectedCustomer.id}"/>
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
            <%-- S4-05: đơn bị Quản lý kinh doanh trả lại sửa: ý kiến của lần trả lại gần nhất --%>
            <c:if test="${not empty returnComment}">
                <div class="sales-flash sales-flash--warning" id="return-comment" role="status" data-toast="off">
                    <strong>Quản lý trả lại đơn để sửa.</strong> <c:out value="${returnComment}"/>
                </div>
            </c:if>
            <div class="sales-flash sales-flash--warning" id="blocked-warning" role="status" data-toast="off"${draftOfBlocked ? '' : ' hidden'}>
                <strong>Đại lý đang bị khoá giao dịch.</strong>
                <span id="blocked-warning-text"><c:if test="${draftOfBlocked}">Lý do: <c:out value="${selectedCustomer.blockReason}"/>. Đơn nháp này vẫn được xử lý tiếp, nhưng không tạo được đơn mới cho đại lý.</c:if></span>
            </div>
            <div class="sales-flash sales-flash--error" id="overdue-warning" role="status" data-toast="off"${empty overdueMessage ? ' hidden' : ''}>
                <strong>Đại lý có nợ quá hạn.</strong>
                <span id="overdue-warning-text"><c:if test="${not empty overdueMessage}"><c:out value="${overdueMessage}"/>. ${ownDraft ? 'Đơn nháp này vẫn lưu được nhưng chưa gửi được cho tới khi đại lý thanh toán khoản quá hạn.' : 'Không tạo được đơn mới cho đại lý này.'}</c:if></span>
            </div>

            <form class="sales-card order-form" id="order-form" method="post" action="<c:url value='/orders/new'/>" novalidate
                  data-quote-url="<c:url value='/orders/quote'/>" data-customer-url="<c:url value='/orders/customer-data'/>"
                  data-draft-customer="${draft.customerId}">
                <c:if test="${not empty draft}">
                    <input type="hidden" name="draftId" value="${draft.form.draftId}">
                    <input type="hidden" name="version" value="<c:out value='${form.version}'/>">
                </c:if>

                <h2 class="sales-card__title order-form__title">Thông tin đơn hàng</h2>

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

                <%-- S4-02 AC1: công nợ hiện tại, hạn mức và phần còn lại của đại lý, gọn trong một dải;
                     orders.js cập nhật sau mỗi lần tính tiền --%>
                <section class="order-credit${overCreditLimit ? ' order-credit--over' : ''}" id="order-credit" aria-labelledby="order-credit-title" aria-live="polite"${empty credit ? ' hidden' : ''}>
                    <div class="order-credit__row">
                        <h3 class="order-credit__title" id="order-credit-title">Công nợ đại lý</h3>
                        <dl class="order-credit__stats">
                            <div class="order-credit__stat">
                                <dt>Hạn mức</dt>
                                <dd id="credit-limit">${credit.limitGranted ? '' : 'Chưa cấp hạn mức'}<c:if test="${credit.limitGranted}"><fmt:formatNumber value="${credit.creditLimit}" maxFractionDigits="0"/> ₫</c:if></dd>
                            </div>
                            <div class="order-credit__stat">
                                <dt>Công nợ hiện tại</dt>
                                <dd id="credit-ledger"><fmt:formatNumber value="${empty credit ? 0 : credit.ledgerBalance}" maxFractionDigits="0"/> ₫</dd>
                            </div>
                            <div class="order-credit__stat">
                                <dt>Đơn chưa xuất hoá đơn</dt>
                                <dd id="credit-open"><fmt:formatNumber value="${empty credit ? 0 : credit.openOrders}" maxFractionDigits="0"/> ₫</dd>
                            </div>
                            <div class="order-credit__stat order-credit__stat--main">
                                <dt>Còn lại</dt>
                                <dd id="credit-available"${not empty credit and credit.available.signum() < 0 ? ' class="order-credit__value--negative"' : ''}><fmt:formatNumber value="${empty credit ? 0 : credit.available}" maxFractionDigits="0"/> ₫</dd>
                            </div>
                            <div class="order-credit__stat">
                                <dt>Ngày nợ tối đa</dt>
                                <dd id="credit-terms">${empty credit ? 0 : credit.maxDebtDays} ngày</dd>
                            </div>
                        </dl>
                    </div>
                    <div class="order-credit__foot">
                        <progress class="order-credit__bar" id="credit-bar" max="100" value="${empty credit ? 0 : credit.usedPercent}" aria-label="Phần hạn mức đã dùng">${credit.usedPercent}%</progress>
                        <p class="order-credit__after" id="credit-after">
                            <c:choose>
                                <c:when test="${overCreditLimit}">Vượt hạn mức <strong><fmt:formatNumber value="${credit.getExceededBy(quote.total)}" maxFractionDigits="0"/> ₫</strong> nếu gửi đơn này: đơn sẽ được đánh dấu cần duyệt.</c:when>
                                <c:when test="${not empty credit}">Sau đơn này còn <strong><fmt:formatNumber value="${credit.getAvailableAfter(quote.total)}" maxFractionDigits="0"/> ₫</strong>.</c:when>
                            </c:choose>
                        </p>
                    </div>
                </section>

                <section class="order-lines" aria-labelledby="order-lines-title">
                    <div class="section-head">
                        <div>
                            <h3 class="section-head__title order-lines__title" id="order-lines-title">Danh sách hàng hóa</h3>
                            <%-- S4-03 AC1: tồn khả dụng tính theo kho phục vụ của đại lý --%>
                            <p class="section-head__desc" id="order-stock-warehouse"${empty stock.warehouseName ? ' hidden' : ''}>Tồn khả dụng tại <strong id="order-stock-warehouse-name"><c:out value="${stock.warehouseName}"/></strong> (kho phục vụ đại lý)</p>
                        </div>
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
                                    <th scope="col" class="order-table__price">Đơn giá</th>
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
                                    <c:set var="lineStock" value="${empty stock ? null : stock.lines[loop.index]}"/>
                                    <c:set var="stockShown" value="${not empty lineStock and lineStock.checked}"/>
                                    <tr class="order-line${not empty errors[lineKey] ? ' order-line--invalid' : ''}${stockShown and lineStock.shortage ? ' order-line--short' : ''}">
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
                                            <div class="order-line__qty-cell">
                                                <input class="order-line__control order-line__qty" type="text" name="qty" inputmode="decimal" maxlength="16"
                                                       value="<c:out value='${line.qty}'/>" placeholder="0" aria-label="Số lượng dòng ${loop.index + 1}">
                                                <p class="order-line__stock${stockShown and lineStock.shortage ? ' order-line__stock--short' : ''}"${stockShown ? '' : ' hidden'}>
                                                    <span class="order-line__stock-text"><c:choose>
                                                        <c:when test="${not stockShown}"></c:when>
                                                        <c:when test="${lineStock.soldOut}">Hết hàng ở kho này</c:when>
                                                        <c:when test="${lineStock.shortage}">Vượt tồn · khả dụng <c:out value="${lineStock.availableText}"/></c:when>
                                                        <c:otherwise>Khả dụng: <c:out value="${lineStock.availableText}"/></c:otherwise>
                                                    </c:choose></span>
                                                    <button class="order-line__stock-max" type="button" data-qty="${lineStock.maxQtyText}"${stockShown and lineStock.shortage and lineStock.maxQty.signum() > 0 ? '' : ' hidden'}>Đặt tối đa <c:out value="${lineStock.maxText}"/></button>
                                                </p>
                                            </div>
                                        </td>
                                        <%-- S4-01: để trống là giá bảng giá (hiện mờ làm gợi ý); gõ giá khác là sửa tay --%>
                                        <c:set var="pricedOk" value="${not empty priced and empty priced.error}"/>
                                        <td class="order-table__price" data-label="Đơn giá">
                                            <div class="order-line__price-cell">
                                                <div class="order-line__price-box">
                                                    <input class="order-line__control order-line__price" type="text" name="price" inputmode="numeric" maxlength="19"
                                                           value="<c:out value='${line.price}'/>" placeholder="<c:if test='${pricedOk}'><fmt:formatNumber value='${priced.listPrice}' maxFractionDigits='0'/></c:if>"
                                                           aria-label="Đơn giá dòng ${loop.index + 1}">
                                                    <button class="order-line__reset" type="button" title="Về giá bảng giá" aria-label="Về giá bảng giá dòng ${loop.index + 1}"${empty line.price ? ' hidden' : ''}>↺</button>
                                                </div>
                                                <span class="order-line__tag order-line__tag--manual"${pricedOk and priced.manualPrice and not priced.belowFloor ? '' : ' hidden'}>Sửa tay</span>
                                                <span class="order-line__tag order-line__tag--below"${pricedOk and priced.belowFloor ? '' : ' hidden'}>Dưới giá sàn · cần duyệt</span>
                                            </div>
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

                <%-- S4-01: dòng sửa giá tay / dưới giá sàn; orders.js dựng lại sau mỗi lần tính tiền --%>
                <c:set var="needsApproval" value="${quote.needsApproval or overCreditLimit}"/>
                <section class="order-alerts${needsApproval ? ' order-alerts--warning' : ''}" id="order-alerts" aria-labelledby="order-alerts-title" aria-live="polite"${not empty quote and (quote.priceFlagged or overCreditLimit) ? '' : ' hidden'}>
                    <h3 class="order-alerts__title" id="order-alerts-title">Thông báo &amp; cảnh báo</h3>
                    <ul class="order-alerts__list" id="order-alerts-list">
                        <c:if test="${overCreditLimit}">
                            <li class="order-alerts__item order-alerts__item--warning"><strong>Hạn mức công nợ:</strong> tổng đơn cộng công nợ hiện tại vượt hạn mức <fmt:formatNumber value="${credit.getExceededBy(quote.total)}" maxFractionDigits="0"/> ₫.</li>
                        </c:if>
                        <c:forEach var="priced" items="${quote.lines}" varStatus="loop">
                            <c:if test="${empty priced.error and (priced.manualPrice or priced.belowFloor)}">
                                <li class="order-alerts__item order-alerts__item--${priced.belowFloor ? 'warning' : 'info'}">
                                    <strong>Dòng <fmt:formatNumber value="${loop.index + 1}" minIntegerDigits="2"/> · <c:out value="${priced.product.sku}"/>:</strong>
                                    <c:choose>
                                        <c:when test="${priced.belowFloor}">giá thực bán sau chiết khấu <fmt:formatNumber value="${priced.netUnitPrice}" maxFractionDigits="0"/> ₫ thấp hơn giá sàn <fmt:formatNumber value="${priced.floorPrice}" maxFractionDigits="0"/> ₫.</c:when>
                                        <c:otherwise>đã sửa giá tay <fmt:formatNumber value="${priced.unitPrice}" maxFractionDigits="0"/> ₫ (bảng giá <fmt:formatNumber value="${priced.listPrice}" maxFractionDigits="0"/> ₫).</c:otherwise>
                                    </c:choose>
                                </li>
                            </c:if>
                        </c:forEach>
                    </ul>
                    <p class="order-alerts__note" id="order-alerts-note"${needsApproval ? '' : ' hidden'}>Đơn sẽ được đánh dấu <strong>Cần duyệt</strong> để quản lý kinh doanh xem xét.</p>
                </section>

                <%-- Thanh dưới cố định: ghi chú (mở ra khi cần), tổng tiền và nút hành động luôn nằm trong màn hình --%>
                <div class="order-bar">
                    <details class="order-note" id="order-note-box"${not empty form.note or not empty errors.note ? ' open' : ''}>
                        <summary class="order-note__toggle">${empty form.note ? '+ Thêm ghi chú' : 'Ghi chú đơn hàng'}</summary>
                        <div class="sales-field order-note__body${not empty errors.note ? ' sales-field--invalid' : ''}">
                            <label class="sales-field__label" for="order-note">Ghi chú đơn hàng</label>
                            <textarea class="sales-field__control sales-field__control--textarea" id="order-note" name="note" maxlength="1000"
                                      placeholder="Nhập ghi chú nếu có..."><c:out value="${form.note}"/></textarea>
                            <c:if test="${not empty errors.note}"><p class="sales-field__error"><c:out value="${errors.note}"/></p></c:if>
                        </div>
                    </details>

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

                    <div class="sales-actions order-form__actions">
                        <a class="sales-button" href="<c:url value='${currentUser.homePath}'/>">Hủy</a>
                        <button class="sales-button sales-button--soft" type="submit" name="action" value="draft" id="save-draft"${not empty overdueMessage and not ownDraft ? ' disabled' : ''}>
                            <img class="sales-button__icon" src="<c:url value='/assets/img/icons/save.svg'/>" alt="" width="15" height="15">
                            Lưu nháp
                        </button>
                        <button class="sales-button" type="button" id="open-drafts" aria-haspopup="dialog">Mở lại</button>
                        <button class="sales-button sales-button--primary" type="submit" name="action" value="submit" id="submit-order"${not empty overdueMessage ? ' disabled' : ''}>
                            Tạo đơn hàng
                            <img class="sales-button__icon" src="<c:url value='/assets/img/icons/arrow-right.svg'/>" alt="" width="15" height="15">
                        </button>
                    </div>
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
                        <div class="order-line__qty-cell">
                            <input class="order-line__control order-line__qty" type="text" name="qty" inputmode="decimal" maxlength="16"
                                   placeholder="0" aria-label="Số lượng">
                            <p class="order-line__stock" hidden>
                                <span class="order-line__stock-text"></span>
                                <button class="order-line__stock-max" type="button" hidden></button>
                            </p>
                        </div>
                    </td>
                    <td class="order-table__price" data-label="Đơn giá">
                        <div class="order-line__price-cell">
                            <div class="order-line__price-box">
                                <input class="order-line__control order-line__price" type="text" name="price" inputmode="numeric" maxlength="19"
                                       aria-label="Đơn giá">
                                <button class="order-line__reset" type="button" title="Về giá bảng giá" aria-label="Về giá bảng giá" hidden>↺</button>
                            </div>
                            <span class="order-line__tag order-line__tag--manual" hidden>Sửa tay</span>
                            <span class="order-line__tag order-line__tag--below" hidden>Dưới giá sàn · cần duyệt</span>
                        </div>
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
                    <h2 class="sales-panel__title" id="order-drafts-title">Mở lại đơn nháp / xem đơn đã gửi</h2>
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
                <%-- Đơn đã gửi: bấm để xem chi tiết và vòng đời đơn (S4-06) --%>
                <h3 class="order-drafts__section" id="order-recent-title">Đơn đã gửi gần đây</h3>
                <c:choose>
                    <c:when test="${empty recentOrders}">
                        <p class="sales-empty">Chưa có đơn nào được gửi.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="order-drafts__list" aria-labelledby="order-recent-title">
                            <c:forEach var="item" items="${recentOrders}">
                                <li>
                                    <a class="order-drafts__item" href="<c:url value='/orders/view'><c:param name='id' value='${item.id}'/></c:url>">
                                        <span class="order-drafts__no"><c:out value="${item.orderNo}"/> <span class="order-status order-status--${item.statusTone}">${item.statusLabel}</span></span>
                                        <span class="order-drafts__customer"><c:out value="${item.customerName}"/></span>
                                        <span class="order-drafts__meta">Cập nhật <c:out value="${item.updatedAtText}"/> · <fmt:formatNumber value="${item.total}" maxFractionDigits="0"/> ₫</span>
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
