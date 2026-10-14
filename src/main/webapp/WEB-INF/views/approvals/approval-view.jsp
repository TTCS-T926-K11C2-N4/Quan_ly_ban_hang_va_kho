<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="approvals"/>
<c:set var="breadcrumbSection" value="Duyệt đơn"/>
<c:set var="breadcrumbPage" value="${order.orderNo}"/>
<c:set var="creditViolation" value="${empty pending ? null : pending.creditViolation}"/>
<c:set var="floorViolations" value="${empty pending ? null : pending.floorViolations}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Duyệt đơn <c:out value="${order.orderNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/orders.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/approvals.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page order-detail approvals">
            <header class="page-header customer-list-header">
                <div>
                    <h1 class="page-header__title">Duyệt đơn hàng</h1>
                    <p class="page-header__subtitle">
                        <c:choose>
                            <c:when test="${canDecide}">Xem lý do cần duyệt rồi chọn Duyệt, Từ chối hoặc Trả lại cho nhân viên sửa.</c:when>
                            <c:when test="${order.status == 'PENDING_APPROVAL'}">Bạn chỉ có quyền xem. Quản lý kinh doanh mới duyệt được đơn.</c:when>
                            <c:otherwise>Đơn đang ở trạng thái "${order.statusLabel}", không còn chờ duyệt.</c:otherwise>
                        </c:choose>
                    </p>
                </div>
                <div class="profile-actions approval-toolbar">
                    <c:if test="${canDecide}">
                        <button class="approval-action approval-action--approve" type="button" data-open-dialog="approve-dialog">✓ Duyệt đơn</button>
                        <button class="approval-action approval-action--reject" type="button" data-open-dialog="reject-dialog">✕ Từ chối</button>
                        <button class="approval-action approval-action--return" type="button" data-open-dialog="return-dialog">↶ Trả lại sửa</button>
                    </c:if>
                    <a class="sales-button" href="<c:url value='/orders/view'><c:param name='id' value='${order.id}'/></c:url>">Chi tiết đơn</a>
                    <a class="sales-button" href="<c:url value='/approvals'/>">← Đơn chờ duyệt</a>
                </div>
            </header>

            <c:if test="${not empty flashError}">
                <div class="sales-flash sales-flash--error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <div class="approval-grid">
                <div class="approval-grid__main">
                    <section class="sales-card order-card" aria-labelledby="order-no">
                        <div class="order-card__head">
                            <div>
                                <p class="order-card__eyebrow">Mã đơn hàng</p>
                                <h2 class="order-card__code" id="order-no"><c:out value="${order.orderNo}"/></h2>
                                <p class="order-card__sub">Ngày tạo: <c:out value="${order.createdAtText}"/></p>
                            </div>
                            <span class="order-status order-status--${order.statusTone}">${order.statusLabel}</span>
                        </div>
                        <dl class="order-info">
                            <div><dt>Đại lý</dt><dd><c:out value="${order.customerName}"/> <span class="order-card__sub">(<c:out value="${order.customerCode}"/>)</span></dd></div>
                            <div><dt>Nhân viên phụ trách</dt><dd><c:out value="${empty order.salesRepName ? 'Chưa phân công' : order.salesRepName}"/></dd></div>
                            <div><dt>Người tạo</dt><dd><c:out value="${order.createdByName}"/></dd></div>
                            <div><dt>Kho phục vụ</dt><dd><c:out value="${order.warehouseName}"/></dd></div>
                            <div><dt>Tổng tiền đơn</dt><dd class="approval-amount"><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</dd></div>
                            <c:if test="${not empty credit}">
                                <div><dt>Công nợ + đơn chưa xuất hoá đơn</dt><dd class="approval-amount${credit.available.signum() < 0 ? ' approval-amount--danger' : ''}"><fmt:formatNumber value="${credit.used}" maxFractionDigits="0"/> ₫</dd></div>
                                <div class="order-info__wide"><dt>Hạn mức công nợ</dt><dd>${credit.limitGranted ? '' : 'Chưa cấp hạn mức'}<c:if test="${credit.limitGranted}"><fmt:formatNumber value="${credit.creditLimit}" maxFractionDigits="0"/> ₫ · còn lại <fmt:formatNumber value="${credit.available}" maxFractionDigits="0"/> ₫ (đã gồm đơn này)</c:if></dd></div>
                            </c:if>
                        </dl>
                    </section>

                    <%-- AC1: lý do cần duyệt và mức vi phạm, chụp lại lúc nhân viên gửi đơn --%>
                    <c:if test="${not empty pending}">
                        <section class="approval-alert" aria-labelledby="approval-alert-title">
                            <h3 class="approval-alert__title" id="approval-alert-title">Lý do cần duyệt</h3>
                            <ul class="approval-alert__list">
                                <c:if test="${not empty creditViolation}">
                                    <li><strong>Vượt hạn mức công nợ <fmt:formatNumber value="${creditViolation.exceededBy}" maxFractionDigits="0"/> ₫.</strong>
                                        Hạn mức <fmt:formatNumber value="${creditViolation.creditLimit}" maxFractionDigits="0"/> ₫, công nợ và đơn chưa xuất hoá đơn lúc gửi <fmt:formatNumber value="${creditViolation.used}" maxFractionDigits="0"/> ₫.</li>
                                </c:if>
                                <c:forEach var="v" items="${floorViolations}">
                                    <li><strong>Dòng <fmt:formatNumber value="${v.lineNo}" minIntegerDigits="2"/> · <c:out value="${v.sku}"/> dưới giá sàn.</strong>
                                        Giá thực bán <fmt:formatNumber value="${v.netPrice}" maxFractionDigits="0"/> ₫, giá sàn <fmt:formatNumber value="${v.floorPrice}" maxFractionDigits="0"/> ₫.</li>
                                </c:forEach>
                            </ul>
                            <p class="approval-alert__meta">Gửi duyệt lúc <c:out value="${pending.requestedAtText}"/> bởi <c:out value="${pending.requestedByName}"/></p>
                        </section>
                    </c:if>
                </div>

                <section class="sales-card order-items-card approval-grid__items" aria-labelledby="order-items-title">
                    <h3 class="order-card__title" id="order-items-title">Danh sách hàng hoá (${order.items.size()})</h3>
                    <div class="order-items-scroll">
                        <table class="order-items">
                            <thead>
                                <tr>
                                    <th scope="col">STT</th>
                                    <th scope="col">Sản phẩm</th>
                                    <th scope="col">Số lượng</th>
                                    <th scope="col" class="order-items__money">Đơn giá</th>
                                    <th scope="col" class="order-items__money">Chiết khấu</th>
                                    <th scope="col" class="order-items__money">Thành tiền</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${order.items}">
                                    <tr${item.belowFloor ? ' class="approval-row--below"' : ''}>
                                        <td data-label="STT">${item.lineNo}</td>
                                        <td data-label="Sản phẩm">
                                            <span class="order-items__name"><c:out value="${item.productName}"/></span>
                                            <span class="order-items__sku"><c:out value="${item.sku}"/></span>
                                        </td>
                                        <td data-label="Số lượng">
                                            <c:out value="${item.qtyText}"/> <c:out value="${item.unitName}"/>
                                            <c:if test="${item.converted}"><span class="order-items__base">= <c:out value="${item.qtyBaseText}"/> <c:out value="${item.baseUnitName}"/></span></c:if>
                                        </td>
                                        <td data-label="Đơn giá" class="order-items__money">
                                            <fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/> ₫
                                            <c:choose>
                                                <c:when test="${item.belowFloor}"><span class="order-line__tag order-line__tag--below">Sàn <fmt:formatNumber value="${item.floorPrice}" maxFractionDigits="0"/> ₫</span></c:when>
                                                <c:when test="${item.manualPrice}"><span class="order-line__tag order-line__tag--manual">Sửa tay</span></c:when>
                                            </c:choose>
                                        </td>
                                        <td data-label="Chiết khấu" class="order-items__money">
                                            <c:choose>
                                                <c:when test="${item.discount.signum() == 0}">—</c:when>
                                                <c:otherwise>−<fmt:formatNumber value="${item.discount}" maxFractionDigits="0"/> ₫</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td data-label="Thành tiền" class="order-items__money order-items__total"><fmt:formatNumber value="${item.lineTotal}" maxFractionDigits="0"/> ₫</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <dl class="order-totals">
                        <div><dt>Tiền hàng</dt><dd><fmt:formatNumber value="${order.subtotal}" maxFractionDigits="0"/> ₫</dd></div>
                        <div><dt>Chiết khấu</dt><dd>${order.discount.signum() == 0 ? '' : '−'}<fmt:formatNumber value="${order.discount}" maxFractionDigits="0"/> ₫</dd></div>
                        <div class="order-totals__grand"><dt>Tổng phải thu</dt><dd><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</dd></div>
                    </dl>
                </section>

                <%-- AC4: lịch sử duyệt chỉ xem; CSDL chặn sửa / xoá (trigger trg_approval_requests_decide_once) --%>
                <details class="sales-card approval-history">
                    <summary class="approval-history__summary">
                        <span class="order-card__title" id="approval-history-title">Lịch sử duyệt đơn</span>
                        <span class="approval-history__count">${history.size()} lần gửi duyệt</span>
                    </summary>
                    <c:choose>
                        <c:when test="${empty history}">
                            <p class="sales-empty">Đơn này chưa từng gửi duyệt.</p>
                        </c:when>
                        <c:otherwise>
                            <div class="order-items-scroll">
                                <table class="order-items approval-history__table">
                                    <thead>
                                        <tr>
                                            <th scope="col">Thời gian</th>
                                            <th scope="col">Trạng thái</th>
                                            <th scope="col">Người thực hiện</th>
                                            <th scope="col">Ý kiến</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="entry" items="${history}">
                                            <c:if test="${not entry.pending}">
                                                <tr>
                                                    <td data-label="Thời gian"><c:out value="${entry.decidedAtText}"/></td>
                                                    <td data-label="Trạng thái"><span class="order-status order-status--${entry.statusTone}">${entry.statusLabel}</span></td>
                                                    <td data-label="Người thực hiện"><c:out value="${entry.decidedByName}"/></td>
                                                    <td data-label="Ý kiến"><c:out value="${empty entry.comment ? '—' : entry.comment}"/></td>
                                                </tr>
                                            </c:if>
                                            <tr>
                                                <td data-label="Thời gian"><c:out value="${entry.requestedAtText}"/></td>
                                                <td data-label="Trạng thái"><span class="order-status order-status--pending-approval">Gửi duyệt</span></td>
                                                <td data-label="Người thực hiện"><c:out value="${entry.requestedByName}"/></td>
                                                <td data-label="Ý kiến">${entry.violations.size()} vi phạm</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <p class="approval-history__note">Lịch sử chỉ xem, không sửa hoặc xoá được.</p>
                </details>

            </div>

            <c:if test="${canDecide}">
                <dialog class="order-dialog approval-dialog" id="approve-dialog" aria-labelledby="approve-title">
                    <form method="post" action="<c:url value='/approvals/decide'/>" novalidate>
                        <input type="hidden" name="id" value="${order.id}">
                        <input type="hidden" name="version" value="${order.version}">
                        <input type="hidden" name="decision" value="approve">
                        <div class="order-dialog__head">
                            <span class="order-dialog__icon approval-dialog__icon--approve" aria-hidden="true">✓</span>
                            <div>
                                <h2 class="order-dialog__title" id="approve-title">Duyệt đơn <c:out value="${order.orderNo}"/>?</h2>
                                <ul class="order-dialog__list">
                                    <li>Đại lý: <strong><c:out value="${order.customerName}"/></strong> · Tổng tiền <strong><fmt:formatNumber value="${order.total}" maxFractionDigits="0"/> ₫</strong></li>
                                    <li>Tồn đang giữ chỗ cho đơn bắt đầu tính hạn giữ chỗ; đơn chuyển sang chờ kho soạn hàng.</li>
                                </ul>
                            </div>
                        </div>
                        <div class="sales-field">
                            <label class="sales-field__label" for="approve-comment">Ý kiến (không bắt buộc)</label>
                            <textarea class="sales-field__control order-dialog__reason" id="approve-comment" name="comment" rows="2"
                                      maxlength="${commentMaxLength}" placeholder="Vd: Đồng ý giá đặc biệt cho đại lý lâu năm"><c:if test="${decision == 'approve'}"><c:out value="${comment}"/></c:if></textarea>
                        </div>
                        <div class="order-dialog__actions">
                            <button class="sales-button" type="button" data-close-dialog>Huỷ</button>
                            <button class="sales-button approval-dialog__approve" type="submit">Duyệt đơn</button>
                        </div>
                    </form>
                </dialog>

                <c:forEach var="kind" items="reject,return">
                    <c:set var="isReject" value="${kind == 'reject'}"/>
                    <dialog class="order-dialog approval-dialog" id="${kind}-dialog" aria-labelledby="${kind}-title"${decision == kind ? ' data-open="1"' : ''}>
                        <form method="post" action="<c:url value='/approvals/decide'/>" novalidate>
                            <input type="hidden" name="id" value="${order.id}">
                            <input type="hidden" name="version" value="${order.version}">
                            <input type="hidden" name="decision" value="${kind}">
                            <div class="order-dialog__head">
                                <span class="order-dialog__icon${isReject ? '' : ' approval-dialog__icon--return'}" aria-hidden="true">${isReject ? '!' : '↶'}</span>
                                <div>
                                    <h2 class="order-dialog__title" id="${kind}-title">${isReject ? 'Từ chối đơn' : 'Trả lại đơn'} <c:out value="${order.orderNo}"/></h2>
                                    <ul class="order-dialog__list">
                                        <c:choose>
                                            <c:when test="${isReject}">
                                                <li>Đơn chuyển sang <strong>Bị từ chối</strong> và kết thúc, không gửi lại được.</li>
                                            </c:when>
                                            <c:otherwise>
                                                <li>Đơn về <strong>Nháp</strong>; nhân viên thấy ý kiến của bạn khi mở lại, sửa rồi gửi duyệt lại.</li>
                                            </c:otherwise>
                                        </c:choose>
                                        <li>Tồn đang giữ chỗ cho đơn được nhả để bán cho đơn khác.</li>
                                    </ul>
                                </div>
                            </div>
                            <div class="sales-field${decision == kind and not empty decisionError ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="${kind}-comment">${isReject ? 'Lý do từ chối' : 'Nội dung cần sửa'}<span class="sales-field__required">*</span></label>
                                <textarea class="sales-field__control order-dialog__reason" id="${kind}-comment" name="comment" rows="3" required
                                          maxlength="${commentMaxLength}" data-counter="${kind}-count"
                                          placeholder="${isReject ? 'Vd: Giá bán thấp hơn giá vốn, không đồng ý' : 'Vd: Tăng đơn giá dòng 02 lên tối thiểu bằng giá sàn'}"><c:if test="${decision == kind}"><c:out value="${comment}"/></c:if></textarea>
                                <p class="approval-dialog__count" id="${kind}-count" aria-live="polite">0/${commentMaxLength}</p>
                                <c:if test="${decision == kind and not empty decisionError}"><p class="sales-field__error"><c:out value="${decisionError}"/></p></c:if>
                            </div>
                            <div class="order-dialog__actions">
                                <button class="sales-button" type="button" data-close-dialog>Huỷ</button>
                                <button class="sales-button ${isReject ? 'order-dialog__danger' : 'sales-button--primary'}" type="submit">${isReject ? 'Xác nhận từ chối' : 'Xác nhận trả lại'}</button>
                            </div>
                        </form>
                    </dialog>
                </c:forEach>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/approvals.js'/>"></script>
</body>
</html>
