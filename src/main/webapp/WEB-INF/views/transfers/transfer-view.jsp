<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="transfers"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Phiếu chuyển kho"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu <c:out value="${transfer.transferNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/transfers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Phiếu chuyển kho <c:out value="${transfer.transferNo}"/></h1>
                    <p class="inv-header__subtitle"><c:out value="${transfer.fromWarehouseName}"/> → <c:out value="${transfer.toWarehouseName}"/>
                        · Tạo bởi <c:out value="${transfer.createdByName}"/> lúc <c:out value="${transfer.createdAtText}"/></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/transfers'/>">← Danh sách phiếu</a>
                    <span class="transfer-status transfer-status--${transfer.statusTone}" id="transfer-status"><c:out value="${transfer.statusLabel}"/></span>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="transfer-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="transfer-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${transfer.inTransit}">
                <div class="transfer-note" id="transfer-transit-info">
                    <strong>Hàng đang trên đường</strong> từ <c:out value="${transfer.shippedAtText}"/> (xuất bởi <c:out value="${transfer.shippedByName}"/>).
                    Tồn kho đến chỉ được cộng khi xác nhận nhận hàng bên dưới.
                </div>
            </c:if>
            <c:if test="${transfer.status == 'RECEIVED'}">
                <div class="transfer-note transfer-note--done" id="transfer-received-info">
                    <strong>Đã nhận lúc <c:out value="${transfer.receivedAtText}"/> bởi <c:out value="${transfer.receivedByName}"/>.</strong>
                </div>
            </c:if>
            <c:if test="${not empty transfer.note}">
                <p class="transfer-text"><strong>Ghi chú:</strong> <c:out value="${transfer.note}"/></p>
            </c:if>

            <section class="inv-card" aria-labelledby="transfer-items-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="transfer-items-title">Dòng hàng chuyển</h2>
                        <p class="inv-card__subtitle">Số lượng tính theo đơn vị cơ sở của SKU.</p>
                    </div>
                    <p class="inv-card__meta"><strong>${transfer.itemCount}</strong> dòng</p>
                </div>

                <c:choose>
                    <c:when test="${transfer.inTransit and canManage}">
                        <form id="transfer-receive-form" method="post" action="<c:url value='/transfers/receive'/>">
                            <input type="hidden" name="id" value="${transfer.id}">
                            <div class="inv-table-scroll">
                                <table class="inv-table" id="transfer-items">
                                    <thead>
                                        <tr>
                                            <th scope="col">Sản phẩm / SKU</th>
                                            <th scope="col">Lô</th>
                                            <th scope="col" class="is-number">Đã xuất</th>
                                            <th scope="col" class="is-number">Số nhận</th>
                                            <th scope="col">Lý do chênh lệch</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${transfer.items}">
                                            <tr>
                                                <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub">SKU: <c:out value="${item.sku}"/></span></td>
                                                <td><c:out value="${item.lotNo}"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${item.qtySent}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                                <td class="is-number">
                                                    <input type="hidden" name="itemId" value="${item.id}">
                                                    <input class="inv-control transfer-items__qty" type="text" name="received" inputmode="decimal" maxlength="17"
                                                           value="${item.qtySent.stripTrailingZeros().toPlainString()}" aria-label="Số nhận <c:out value='${item.sku}'/>">
                                                </td>
                                                <td>
                                                    <input class="inv-control" type="text" name="reason" maxlength="${reasonMaxLength}"
                                                           placeholder="Bắt buộc nếu nhận khác số đã xuất" aria-label="Lý do chênh lệch <c:out value='${item.sku}'/>">
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div class="transfer-actions">
                                <button class="inv-button inv-button--primary" type="submit" id="transfer-receive">Xác nhận nhận hàng</button>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="inv-table-scroll">
                            <table class="inv-table" id="transfer-items">
                                <thead>
                                    <tr>
                                        <th scope="col">Sản phẩm / SKU</th>
                                        <th scope="col">Lô</th>
                                        <th scope="col" class="is-number">Số xuất</th>
                                        <th scope="col" class="is-number">Số nhận</th>
                                        <th scope="col">Lý do chênh lệch</th>
                                        <c:if test="${transfer.draft and canManage}"><th scope="col"><span class="visually-hidden">Xoá</span></th></c:if>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="item" items="${transfer.items}">
                                        <tr class="${item.missing ? 'transfer-items__row--short' : ''}">
                                            <td><span class="inv-table__main"><c:out value="${item.productName}"/></span><span class="inv-table__sub">SKU: <c:out value="${item.sku}"/></span></td>
                                            <td><c:out value="${item.lotNo}"/></td>
                                            <td class="is-number"><fmt:formatNumber value="${item.qtySent}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                            <td class="is-number"><c:choose><c:when test="${item.received}"><fmt:formatNumber value="${item.qtyReceived}" maxFractionDigits="3"/></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                                            <td><c:out value="${empty item.varianceReason ? '—' : item.varianceReason}"/></td>
                                            <c:if test="${transfer.draft and canManage}">
                                                <td>
                                                    <form method="post" action="<c:url value='/transfers/line/remove'/>">
                                                        <input type="hidden" name="id" value="${transfer.id}">
                                                        <input type="hidden" name="item" value="${item.id}">
                                                        <button class="inv-button inv-button--small" type="submit">Xoá</button>
                                                    </form>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty transfer.items}">
                                        <tr><td class="inv-table__empty" colspan="6">Phiếu chưa có dòng hàng nào.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <c:if test="${transfer.draft and canManage}">
                <section class="inv-card" aria-labelledby="transfer-add-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="transfer-add-title">Thêm dòng hàng</h2>
                            <p class="inv-card__subtitle">Chọn lô đang có hàng ở kho đi và số lượng cần chuyển.</p>
                        </div>
                    </div>
                    <form class="transfer-new" id="transfer-add-form" method="post" action="<c:url value='/transfers/line/add'/>">
                        <input type="hidden" name="id" value="${transfer.id}">
                        <div class="inv-field transfer-new__note">
                            <label class="inv-field__label" for="transfer-lot">Lô hàng ở kho đi</label>
                            <select class="inv-control" id="transfer-lot" name="lot" required>
                                <option value="">Chọn SKU / lô</option>
                                <c:forEach var="lot" items="${lotOptions}">
                                    <option value="${lot.lotId}"><c:out value="${lot.sku}"/> · <c:out value="${lot.productName}"/> · lô <c:out value="${lot.lotNo}"/> (tồn <fmt:formatNumber value="${lot.qtyOnHand}" maxFractionDigits="3"/>)</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="inv-field__label" for="transfer-qty">Số lượng chuyển</label>
                            <input class="inv-control" type="text" id="transfer-qty" name="qty" inputmode="decimal" maxlength="17" required placeholder="0">
                        </div>
                        <button class="inv-button inv-button--primary" type="submit" id="transfer-add">Thêm dòng</button>
                    </form>
                </section>

                <div class="transfer-actions">
                    <form method="post" action="<c:url value='/transfers/cancel'/>">
                        <input type="hidden" name="id" value="${transfer.id}">
                        <button class="inv-button" type="submit" id="transfer-cancel">Huỷ phiếu</button>
                    </form>
                    <form method="post" action="<c:url value='/transfers/ship'/>">
                        <input type="hidden" name="id" value="${transfer.id}">
                        <button class="inv-button inv-button--primary" type="submit" id="transfer-ship">Xuất kho đi</button>
                    </form>
                </div>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
