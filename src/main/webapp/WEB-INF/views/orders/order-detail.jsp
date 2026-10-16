<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="sales"/>
<c:set var="activeSubmenu" value="order-list"/>
<c:set var="breadcrumbSection" value="Bán hàng"/>
<c:set var="breadcrumbPage" value="Chi tiết đơn hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn <c:out value="${order.orderNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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

        <main class="app-content sales-page order-detail">
            <header class="page-header customer-list-header">
                <div>
                    <h1 class="page-header__title">Chi tiết đơn hàng</h1>
                    <p class="page-header__subtitle">Theo dõi đơn đang ở khâu nào trong vòng đời và huỷ đơn khi chưa xuất kho.</p>
                </div>
                <div class="profile-actions">
                    <a class="sales-button" href="<c:url value='/orders'/>">← Danh sách đơn</a>
                    <c:if test="${currentUser.can('ORDER_MANAGE')}">
                        <form method="post" action="<c:url value='/orders/copy'/>">
                            <input type="hidden" name="id" value="${order.id}">
                            <button class="sales-button" type="submit" id="copy-order">Sao chép đơn</button>
                        </form>
                    </c:if>
                    <c:if test="${canCancel}">
                        <button class="sales-button profile-actions__delete" type="button" id="open-cancel-order">Huỷ đơn</button>
                    </c:if>
                    <a class="sales-button" id="print-order-link" href="<c:url value='/orders/print'><c:param name='id' value='${order.id}'/></c:url>" target="_blank" rel="noopener">In / Xuất PDF</a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="order-detail-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="sales-flash sales-flash--error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <%-- S4-10: đơn do đại lý tự đặt trên cổng chờ nhân viên phụ trách xác nhận --%>
            <c:if test="${canRepConfirm}">
                <section class="sales-card order-confirm" id="order-rep-confirm" aria-labelledby="order-confirm-title">
                    <h3 class="order-card__title" id="order-confirm-title">Đơn do đại lý tự đặt, chờ bạn xác nhận</h3>
                    <form method="post" action="<c:url value='/orders/confirm'/>" class="order-confirm__form">
                        <input type="hidden" name="id" value="${order.id}">
                        <label class="sales-field__label" for="order-confirm-comment">Ý kiến (bắt buộc khi từ chối)</label>
                        <textarea class="sales-field__control" id="order-confirm-comment" name="comment" rows="2" maxlength="500"></textarea>
                        <div class="order-confirm__actions">
                            <button class="sales-button" type="submit" name="decision" value="reject" id="order-reject">Từ chối</button>
                            <button class="sales-button sales-button--primary" type="submit" name="decision" value="approve" id="order-confirm">Xác nhận đơn</button>
                        </div>
                    </form>
                </section>
            </c:if>

            <%-- Hàng đầu: đơn hàng, đại lý, điểm giao (thiết kế S4-06; khối Xuất kho / Thanh toán gắn thêm ở Sprint 6, 7) --%>
            <div class="order-overview">
                <section class="sales-card order-card" aria-labelledby="order-no">
                    <div class="order-card__head">
                        <div>
                            <p class="order-card__eyebrow">Mã đơn hàng</p>
                            <h2 class="order-card__code" id="order-no"><c:out value="${order.orderNo}"/></h2>
                        </div>
                        <span class="order-status order-status--${order.statusTone}" id="order-status">${order.statusLabel}</span>
                    </div>
                    <c:if test="${order.needsApproval and order.status == 'PENDING_APPROVAL'}">
                        <p class="order-card__flag">Cần quản lý duyệt (vượt hạn mức hoặc dưới giá sàn)</p>
                    </c:if>
                    <dl class="order-info">
                        <div><dt>Ngày tạo</dt><dd><c:out value="${order.createdAtText}"/></dd></div>
                        <div><dt>Ngày giao mong muốn</dt><dd><c:out value="${empty order.requestedDateText ? '—' : order.requestedDateText}"/></dd></div>
                        <div><dt>Nhân viên phụ trách</dt><dd><c:out value="${empty order.salesRepName ? 'Chưa phân công' : order.salesRepName}"/></dd></div>
                        <div><dt>Kho phục vụ</dt><dd><c:out value="${order.warehouseName}"/></dd></div>
                    </dl>
                </section>

                <section class="sales-card order-card" aria-labelledby="order-customer-title">
                    <h3 class="order-card__title" id="order-customer-title">Đại lý</h3>
                    <div>
                        <p class="order-card__name">
                            <c:choose>
                                <c:when test="${currentUser.can('CUSTOMER_VIEW')}">
                                    <a class="order-card__link" href="<c:url value='/customers/profile'><c:param name='id' value='${order.customerId}'/></c:url>"><c:out value="${order.customerName}"/></a>
                                </c:when>
                                <c:otherwise><c:out value="${order.customerName}"/></c:otherwise>
                            </c:choose>
                        </p>
                        <p class="order-card__sub"><c:out value="${order.customerCode}"/> · <c:out value="${order.customerGroupName}"/></p>
                    </div>
                    <dl class="order-info">
                        <div><dt>Mã số thuế</dt><dd><c:out value="${empty order.customerTaxCode ? '—' : order.customerTaxCode}"/></dd></div>
                        <div><dt>Số điện thoại</dt><dd>
                            <c:choose>
                                <c:when test="${empty order.customerPhone}">—</c:when>
                                <c:otherwise><a class="order-card__link" href="tel:<c:out value='${order.customerPhone}'/>"><c:out value="${order.customerPhone}"/></a></c:otherwise>
                            </c:choose>
                        </dd></div>
                        <div class="order-info__wide"><dt>Địa chỉ</dt><dd><c:out value="${empty order.customerAddress ? '—' : order.customerAddress}"/></dd></div>
                    </dl>
                </section>

                <section class="sales-card order-card" aria-labelledby="order-address-title">
                    <h3 class="order-card__title" id="order-address-title">Điểm giao hàng</h3>
                    <div>
                        <p class="order-card__name"><c:out value="${empty order.addressLabel ? 'Điểm giao' : order.addressLabel}"/></p>
                        <p class="order-card__sub"><c:out value="${order.deliveryAddress}"/></p>
                    </div>
                    <dl class="order-info">
                        <div><dt>Người nhận</dt><dd><c:out value="${order.receiverName}"/></dd></div>
                        <div><dt>Số điện thoại</dt><dd><a class="order-card__link" href="tel:<c:out value='${order.receiverPhone}'/>"><c:out value="${order.receiverPhone}"/></a></dd></div>
                        <c:if test="${not empty order.routeNote}">
                            <div class="order-info__wide"><dt>Ghi chú đường đi</dt><dd><c:out value="${order.routeNote}"/></dd></div>
                        </c:if>
                    </dl>
                </section>
            </div>

            <%-- Thanh vòng đời (AC1): đơn huỷ / bị từ chối dừng ở bước đã qua rồi rẽ nhánh màu đỏ --%>
            <section class="sales-card order-progress" aria-labelledby="order-progress-title">
                <div class="order-progress__head">
                    <h3 class="order-card__title" id="order-progress-title">Vòng đời đơn hàng</h3>
                    <button class="order-progress__legend-button" type="button" id="open-status-legend">Ý nghĩa các trạng thái</button>
                </div>
                <ol class="order-steps">
                    <c:forEach var="step" items="${order.steps}" varStatus="loop">
                        <li class="order-step order-step--${step.state}"${step.state == 'current' or step.state == 'branch' ? ' aria-current="step"' : ''}>
                            <span class="order-step__dot" aria-hidden="true">${step.state == 'done' ? '✓' : step.state == 'branch' ? '✕' : loop.index + 1}</span>
                            <span class="order-step__label"><c:out value="${step.label}"/></span>
                            <span class="order-step__time"><c:out value="${step.atText}"/></span>
                        </li>
                    </c:forEach>
                </ol>
                <c:if test="${order.cancelled}">
                    <div class="order-progress__note order-progress__note--danger" id="order-cancelled-info">
                        <strong>Đơn đã huỷ<c:if test="${not empty order.cancelledAtText}"> lúc <c:out value="${order.cancelledAtText}"/></c:if><c:if test="${not empty order.cancelledByName}"> bởi <c:out value="${order.cancelledByName}"/></c:if>.</strong>
                        Lý do: <c:out value="${order.cancelReason}"/>
                    </div>
                </c:if>
                <c:if test="${order.shipped}">
                    <div class="order-progress__note">Đơn đã xuất kho nên không huỷ được. Muốn trả hàng, vui lòng lập phiếu trả hàng.</div>
                </c:if>
            </section>

            <div class="order-body">
                <section class="sales-card order-items-card" aria-labelledby="order-items-title">
                    <h3 class="order-card__title" id="order-items-title">Hàng hoá (${order.items.size()})</h3>
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
                                    <tr>
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
                                                <c:when test="${item.belowFloor}"><span class="order-line__tag order-line__tag--below">Dưới giá sàn</span></c:when>
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

                <aside class="order-side">
                    <c:if test="${not empty order.note}">
                        <section class="sales-card order-note" aria-labelledby="order-note-title">
                            <h3 class="order-card__title" id="order-note-title">Ghi chú đơn hàng</h3>
                            <p class="order-note__text"><c:out value="${order.note}"/></p>
                        </section>
                    </c:if>

                    <%-- Lịch sử đổi trạng thái, mới nhất trước; chỉ xem, không sửa/xoá (order_status_history chỉ thêm) --%>
                    <section class="sales-card order-history" aria-labelledby="order-history-title">
                        <h3 class="order-card__title" id="order-history-title">Lịch sử trạng thái</h3>
                        <c:choose>
                            <c:when test="${empty order.history}">
                                <p class="sales-empty">Đơn tạo trước khi có lịch sử trạng thái; thời điểm các bước xem trên thanh vòng đời.</p>
                            </c:when>
                            <c:otherwise>
                                <ol class="order-timeline">
                                    <c:forEach var="row" items="${order.historyNewestFirst}">
                                        <li class="order-timeline__item">
                                            <span class="order-status order-status--${row.toTone}">${row.toLabel}</span>
                                            <span class="order-timeline__meta"><c:out value="${row.createdAtText}"/><c:if test="${not empty row.actorName}"> · <c:out value="${row.actorName}"/></c:if></span>
                                            <c:if test="${not empty row.note}"><span class="order-timeline__note"><c:out value="${row.note}"/></span></c:if>
                                        </li>
                                    </c:forEach>
                                </ol>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </aside>
            </div>

            <%-- Huỷ đơn (AC2): bắt buộc lý do, nhả tồn đang giữ chỗ; server kiểm tra lại trạng thái và quyền --%>
            <c:if test="${canCancel}">
                <dialog class="order-dialog" id="cancel-order-dialog" aria-labelledby="cancel-order-title"${not empty cancelError ? ' data-open="1"' : ''}>
                    <form method="post" action="<c:url value='/orders/cancel'/>" novalidate>
                        <input type="hidden" name="id" value="${order.id}">
                        <input type="hidden" name="version" value="${order.version}">
                        <div class="order-dialog__head">
                            <span class="order-dialog__icon" aria-hidden="true">!</span>
                            <div>
                                <h2 class="order-dialog__title" id="cancel-order-title">Huỷ đơn <c:out value="${order.orderNo}"/>?</h2>
                                <ul class="order-dialog__list">
                                    <li>Đơn chuyển sang <strong>Đã huỷ</strong> và không khôi phục được.</li>
                                    <li>Tồn kho đang giữ chỗ cho đơn (nếu có) được nhả để bán cho đơn khác.</li>
                                    <li>Lý do huỷ được ghi vào lịch sử trạng thái và nhật ký thao tác.</li>
                                </ul>
                            </div>
                        </div>
                        <div class="sales-field${not empty cancelError ? ' sales-field--invalid' : ''}">
                            <label class="sales-field__label" for="cancel-reason">Lý do huỷ<span class="sales-field__required">*</span></label>
                            <textarea class="sales-field__control order-dialog__reason" id="cancel-reason" name="reason" rows="3"
                                      maxlength="${reasonMaxLength}" required placeholder="Vd: Đại lý đổi ý, đặt nhầm mặt hàng, trùng đơn…"><c:out value="${cancelReason}"/></textarea>
                            <c:if test="${not empty cancelError}"><p class="sales-field__error"><c:out value="${cancelError}"/></p></c:if>
                        </div>
                        <div class="order-dialog__actions">
                            <button class="sales-button" type="button" data-close-dialog>Không huỷ</button>
                            <button class="sales-button order-dialog__danger" type="submit" id="confirm-cancel-order">Xác nhận huỷ đơn</button>
                        </div>
                    </form>
                </dialog>
            </c:if>

            <dialog class="order-dialog order-dialog--legend" id="status-legend-dialog" aria-labelledby="status-legend-title">
                <div class="order-dialog__bar">
                    <h2 class="order-dialog__title" id="status-legend-title">Các trạng thái của đơn hàng</h2>
                    <button class="order-drafts__close" type="button" data-close-dialog aria-label="Đóng">×</button>
                </div>
                <ul class="order-legend">
                    <c:forEach var="item" items="${statusLegend}">
                        <li><span class="order-status order-status--${item.tone()}">${item.label()}</span><span>${item.description()}</span></li>
                    </c:forEach>
                </ul>
            </dialog>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/orders.js'/>"></script>
</body>
</html>
