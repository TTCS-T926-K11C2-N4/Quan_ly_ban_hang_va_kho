<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="pick-lists"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Phiếu soạn hàng"/>
<c:set var="breadcrumbParentUrl" value="/pick-lists/view?id=${draft.pickListId}"/>
<c:set var="breadcrumbPage" value="Xác nhận xuất kho & giao thiếu"/>
<c:url var="backUrl" value="/pick-lists/view"><c:param name="id" value="${draft.pickListId}"/></c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác nhận xuất kho <c:out value="${draft.orderNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/delivery-notes.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page issue-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Xác nhận xuất kho &amp; giao thiếu</h1>
                    <p class="inv-header__subtitle">Nhập số lượng thực xuất cho từng dòng, chọn lý do khi xuất thiếu và xử lý phần thiếu trước khi xác nhận xuất kho.</p>
                </div>
            </header>

            <c:if test="${not empty errors}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="issue-error" role="alert">
                    <c:choose>
                        <c:when test="${not empty errors['lines']}"><c:out value="${errors['lines']}"/></c:when>
                        <c:otherwise>Chưa xác nhận được: kiểm tra các ô được đánh dấu đỏ.</c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <form class="issue-form" id="issue-form" action="<c:url value='/delivery-notes/confirm'/>" method="post" novalidate
                  data-order-no="<c:out value='${draft.orderNo}'/>">
                <input type="hidden" name="pickList" value="${draft.pickListId}">
                <input type="hidden" name="order" value="${draft.orderId}">

                <section class="inv-card issue-head" aria-label="Phiếu soạn hàng">
                    <div>
                        <h2 class="issue-head__title">Phiếu soạn hàng <c:out value="${draft.pickNo}"/></h2>
                        <p class="issue-head__meta">Đơn hàng <c:out value="${draft.orderNo}"/> • <c:out value="${draft.warehouseName}"/> • Ngày giao ${draft.deliveryDateText}</p>
                    </div>
                    <div class="issue-head__pills">
                        <span class="inv-pill issue-pill issue-pill--pending">Trạng thái: Chờ xác nhận</span>
                        <span class="inv-pill issue-pill issue-pill--short" id="issue-short-pill" hidden><span data-short-lines>0</span>&nbsp;dòng xuất thiếu</span>
                    </div>
                </section>

                <div class="issue-grid">
                    <section class="inv-card issue-info" aria-labelledby="issue-info-title">
                        <h2 class="issue-card__title" id="issue-info-title">Thông tin phiếu soạn</h2>
                        <dl class="issue-info__grid">
                            <div><dt>Mã phiếu soạn</dt><dd><c:out value="${draft.pickNo}"/></dd></div>
                            <div><dt>Đơn hàng</dt><dd><c:out value="${draft.orderNo}"/></dd></div>
                            <div><dt>Khách hàng</dt><dd><c:out value="${draft.customerName}"/></dd></div>
                            <div><dt>Kho</dt><dd><c:out value="${draft.warehouseName}"/></dd></div>
                            <div><dt>Ngày giao</dt><dd>${draft.deliveryDateText}</dd></div>
                            <div><dt>Người soạn</dt><dd><c:out value="${empty draft.pickerName ? '—' : draft.pickerName}"/></dd></div>
                            <div><dt>Tổng cần xuất</dt><dd><span id="total-required"><fmt:formatNumber value="${draft.totalRequired}" maxFractionDigits="3"/></span> sản phẩm</dd></div>
                            <div><dt>Tổng thực xuất</dt><dd><span id="total-issued"><fmt:formatNumber value="${draft.totalPicked}" maxFractionDigits="3"/></span> sản phẩm</dd></div>
                            <div><dt>Tổng thiếu</dt><dd class="issue-info__short"><span id="total-short">0</span> sản phẩm</dd></div>
                        </dl>
                    </section>

                    <section class="inv-card issue-shortage" id="issue-shortage" aria-labelledby="issue-shortage-title">
                        <h2 class="issue-card__title" id="issue-shortage-title">Xử lý phần thiếu</h2>
                        <p class="issue-shortage__none" id="issue-shortage-none">Không có dòng xuất thiếu, không cần xử lý.</p>
                        <fieldset class="issue-shortage__body" id="issue-shortage-body">
                            <legend class="issue-field__label">Chọn xử lý phần thiếu</legend>
                            <div class="issue-choice${not empty errors['shortageAction'] ? ' is-invalid' : ''}">
                                <c:forEach var="action" items="${shortageLabels}">
                                    <label class="issue-choice__option">
                                        <input type="radio" name="shortageAction" value="${action.key}"${action.key == form.shortageAction ? ' checked' : ''} data-shortage-action>
                                        <span class="issue-choice__mark" aria-hidden="true"></span>
                                        <span>${action.value}</span>
                                    </label>
                                </c:forEach>
                            </div>
                            <c:if test="${not empty errors['shortageAction']}"><p class="issue-field__error"><c:out value="${errors['shortageAction']}"/></p></c:if>
                            <div class="issue-shortage__schedule" id="issue-schedule">
                                <div class="inv-field${not empty errors['backorderDate'] ? ' inv-field--invalid' : ''}">
                                    <label class="issue-field__label" for="backorder-date">Ngày giao bổ sung</label>
                                    <span class="issue-control-icon">
                                        <span class="inv-icon issue-icon--calendar" aria-hidden="true"></span>
                                        <input class="inv-control" type="date" id="backorder-date" name="backorderDate" min="${today}"
                                               value="<c:out value='${form.backorderDate}'/>">
                                    </span>
                                    <c:if test="${not empty errors['backorderDate']}"><p class="issue-field__error"><c:out value="${errors['backorderDate']}"/></p></c:if>
                                </div>
                                <div class="inv-field${not empty errors['backorderTime'] ? ' inv-field--invalid' : ''}">
                                    <label class="issue-field__label" for="backorder-time">Giờ giao bổ sung</label>
                                    <span class="issue-control-icon">
                                        <span class="inv-icon issue-icon--clock" aria-hidden="true"></span>
                                        <input class="inv-control" type="time" id="backorder-time" name="backorderTime"
                                               value="<c:out value='${form.backorderTime}'/>">
                                    </span>
                                    <c:if test="${not empty errors['backorderTime']}"><p class="issue-field__error"><c:out value="${errors['backorderTime']}"/></p></c:if>
                                </div>
                            </div>
                            <div class="inv-field${not empty errors['shortageNote'] ? ' inv-field--invalid' : ''}">
                                <label class="issue-field__label" for="shortage-note">Ghi chú xử lý</label>
                                <textarea class="inv-control inv-control--textarea" id="shortage-note" name="shortageNote" rows="2" maxlength="1000"
                                          placeholder="Giao bổ sung cho dòng thiếu sau khi xác nhận xuất kho chính."><c:out value="${form.shortageNote}"/></textarea>
                                <c:if test="${not empty errors['shortageNote']}"><p class="issue-field__error"><c:out value="${errors['shortageNote']}"/></p></c:if>
                            </div>
                        </fieldset>
                    </section>
                </div>

                <section class="inv-card issue-lines" aria-labelledby="issue-lines-title">
                    <div class="issue-lines__head">
                        <h2 class="issue-card__title" id="issue-lines-title">Nhập số lượng thực xuất theo dòng</h2>
                        <p class="issue-lines__count">${fn:length(draft.lines)} dòng hàng</p>
                    </div>
                    <div class="inv-table-scroll">
                        <table class="inv-table issue-table" id="issue-table">
                            <thead>
                                <tr>
                                    <th scope="col">Mã sản phẩm</th>
                                    <th scope="col">Tên sản phẩm</th>
                                    <th scope="col" class="is-number">Cần xuất</th>
                                    <th scope="col" class="issue-table__input-col">Thực xuất</th>
                                    <th scope="col" class="is-number">Thiếu</th>
                                    <th scope="col">Lý do thiếu</th>
                                    <th scope="col">Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="line" items="${draft.lines}">
                                    <c:set var="input" value="${form.lines[line.orderItemId]}"/>
                                    <c:set var="errorKey" value="line_${line.orderItemId}"/>
                                    <c:set var="lineError" value="${errors[errorKey]}"/>
                                    <tr class="issue-row${not empty lineError ? ' is-invalid' : ''}" data-issue-line data-sku="<c:out value='${line.sku}'/>"
                                        data-name="<c:out value='${line.productName}'/>" data-required="${line.qtyRequiredText}" data-picked="${line.qtyPickedText}">
                                        <td><span class="issue-table__sku"><c:out value="${line.sku}"/></span></td>
                                        <td>
                                            <span class="inv-table__main"><c:out value="${line.productName}"/></span>
                                            <span class="inv-table__sub">Đã soạn ${line.qtyPickedText} <c:out value="${line.unitName}"/></span>
                                        </td>
                                        <td class="is-number issue-table__qty">${line.qtyRequiredText}</td>
                                        <td class="issue-table__input-col">
                                            <label class="visually-hidden" for="qty-${line.orderItemId}">Thực xuất <c:out value="${line.sku}"/></label>
                                            <input class="inv-control issue-table__input" type="text" inputmode="decimal" id="qty-${line.orderItemId}"
                                                   name="qty_${line.orderItemId}" value="<c:out value='${input.qty}'/>" maxlength="16" autocomplete="off" data-issue-qty>
                                        </td>
                                        <td class="is-number issue-table__qty issue-table__short" data-issue-short>0</td>
                                        <td>
                                            <label class="visually-hidden" for="reason-${line.orderItemId}">Lý do thiếu <c:out value="${line.sku}"/></label>
                                            <select class="inv-control issue-table__reason" id="reason-${line.orderItemId}" name="reason_${line.orderItemId}" data-issue-reason>
                                                <option value="">Chọn lý do</option>
                                                <c:forEach var="reason" items="${shortReasons}">
                                                    <option value="<c:out value='${reason}'/>"${reason == input.reason ? ' selected' : ''}><c:out value="${reason}"/></option>
                                                </c:forEach>
                                            </select>
                                        </td>
                                        <td><span class="inv-pill issue-pill issue-pill--ok" data-issue-state>Đủ</span></td>
                                    </tr>
                                    <c:if test="${not empty lineError}">
                                        <tr class="issue-row-error"><td colspan="7"><c:out value="${lineError}"/></td></tr>
                                    </c:if>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <div class="issue-warning" id="issue-warning" role="status" hidden>
                        <span class="inv-icon inv-icon--triangle-alert" aria-hidden="true"></span>
                        <p id="issue-warning-text"></p>
                    </div>
                    <div class="issue-actions">
                        <a class="inv-button inv-button--outline" id="issue-cancel" href="${fn:escapeXml(backUrl)}">Hủy</a>
                        <button class="inv-button inv-button--primary" type="submit" id="issue-submit">
                            <span class="inv-icon issue-icon--package-check" aria-hidden="true"></span>Xác nhận xuất kho
                        </button>
                    </div>
                </section>
            </form>

            <%-- Hộp thoại kiểm tra lại trước khi xác nhận (app chạy không JS thì form gửi thẳng, server vẫn kiểm tra) --%>
            <dialog class="issue-dialog" id="issue-dialog" aria-labelledby="issue-dialog-title">
                <div class="issue-dialog__head">
                    <span class="issue-dialog__icon" aria-hidden="true"><span class="inv-icon issue-icon--package-check"></span></span>
                    <div>
                        <h2 class="issue-dialog__title" id="issue-dialog-title">Xác nhận xuất kho?</h2>
                        <p class="issue-dialog__text">Kiểm tra lại số lượng trước khi xác nhận. Phiếu xuất đã xác nhận không sửa được.</p>
                    </div>
                </div>
                <dl class="issue-dialog__summary">
                    <div><dt>Tổng cần xuất</dt><dd data-dialog-required>0</dd></div>
                    <div><dt>Tổng thực xuất</dt><dd data-dialog-issued>0</dd></div>
                    <div><dt>Tổng thiếu</dt><dd class="issue-info__short" data-dialog-short>0</dd></div>
                </dl>
                <div class="issue-dialog__shortage" data-dialog-shortage>
                    <h3 class="issue-dialog__subtitle">Chi tiết giao thiếu</h3>
                    <ul class="issue-dialog__list" data-dialog-list></ul>
                    <h3 class="issue-dialog__subtitle">Nguyên nhân và xử lý</h3>
                    <p class="issue-dialog__handling" data-dialog-handling></p>
                </div>
                <p class="issue-dialog__info">
                    <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                    <span>Tồn kho được trừ theo số thực xuất của từng lô đã soạn, giữ chỗ của đơn chuyển thành xuất thực và đơn chuyển sang "Đã xuất kho". Phần thiếu chọn giao bổ sung sẽ thành hàng còn nợ và hiện ở mục "Đơn còn nợ hàng".</span>
                </p>
                <div class="issue-dialog__actions">
                    <button class="inv-button inv-button--outline" type="button" id="issue-dialog-back" data-dialog-close>Quay lại</button>
                    <button class="inv-button inv-button--primary" type="button" id="issue-dialog-confirm" data-dialog-confirm>Xác nhận xuất kho</button>
                </div>
            </dialog>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/delivery-notes.js'/>"></script>
</body>
</html>
