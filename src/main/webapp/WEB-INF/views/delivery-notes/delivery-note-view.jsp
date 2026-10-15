<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="pick-lists"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Phiếu soạn hàng"/>
<c:choose>
    <c:when test="${empty note.pickListId}"><c:set var="breadcrumbParentUrl" value="/pick-lists"/></c:when>
    <c:otherwise><c:set var="breadcrumbParentUrl" value="/pick-lists/view?id=${note.pickListId}"/></c:otherwise>
</c:choose>
<c:set var="breadcrumbPage" value="Phiếu xuất ${note.deliveryNo}"/>
<c:url var="orderUrl" value="/orders/view"><c:param name="id" value="${note.orderId}"/></c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu xuất <c:out value="${note.deliveryNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Phiếu xuất kho <c:out value="${note.deliveryNo}"/></h1>
                    <p class="inv-header__subtitle">Đơn hàng <c:out value="${note.orderNo}"/> • <c:out value="${note.warehouseName}"/> • Xuất lúc ${note.issuedAtText}</p>
                </div>
                <div class="inv-header__actions">
                    <c:if test="${not empty note.pickListId}">
                        <c:url var="pickUrl" value="/pick-lists/view"><c:param name="id" value="${note.pickListId}"/></c:url>
                        <a class="inv-button inv-button--outline" id="note-back" href="${fn:escapeXml(pickUrl)}">
                            <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Phiếu soạn <c:out value="${note.pickNo}"/>
                        </a>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="note-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="inv-card issue-head" aria-label="Phiếu xuất kho">
                <div>
                    <h2 class="issue-head__title">Phiếu xuất <c:out value="${note.deliveryNo}"/></h2>
                    <p class="issue-head__meta">Đơn hàng <c:out value="${note.orderNo}"/> • <c:out value="${note.warehouseName}"/> • Ngày giao ${note.deliveryDateText}</p>
                </div>
                <div class="issue-head__pills">
                    <span class="inv-pill issue-pill issue-pill--${note.tone}" id="note-status">${note.statusLabel}</span>
                    <c:if test="${note.shortLines > 0}">
                        <span class="inv-pill issue-pill issue-pill--short">${note.shortLines} dòng xuất thiếu</span>
                    </c:if>
                    <span class="inv-pill issue-pill issue-pill--locked" title="Phiếu xuất đã xác nhận không sửa được">Đã xác nhận · không sửa được</span>
                </div>
            </section>

            <div class="issue-grid">
                <section class="inv-card issue-info" aria-labelledby="note-info-title">
                    <h2 class="issue-card__title" id="note-info-title">Thông tin phiếu xuất</h2>
                    <dl class="issue-info__grid">
                        <div><dt>Mã phiếu xuất</dt><dd><c:out value="${note.deliveryNo}"/></dd></div>
                        <div><dt>Phiếu soạn</dt><dd><c:out value="${empty note.pickNo ? '—' : note.pickNo}"/></dd></div>
                        <div><dt>Đơn hàng</dt><dd><a class="issue-info__link" href="${fn:escapeXml(orderUrl)}"><c:out value="${note.orderNo}"/></a></dd></div>
                        <div><dt>Khách hàng</dt><dd><c:out value="${note.customerName}"/></dd></div>
                        <div><dt>Người xuất</dt><dd><c:out value="${empty note.issuedByName ? '—' : note.issuedByName}"/></dd></div>
                        <div><dt>Thời điểm xuất</dt><dd>${note.issuedAtText}</dd></div>
                        <div><dt>Tổng cần xuất</dt><dd><fmt:formatNumber value="${note.totalRequired}" maxFractionDigits="3"/> sản phẩm</dd></div>
                        <div><dt>Tổng thực xuất</dt><dd><fmt:formatNumber value="${note.totalIssued}" maxFractionDigits="3"/> sản phẩm</dd></div>
                        <div><dt>Tổng thiếu</dt><dd class="issue-info__short"><fmt:formatNumber value="${note.totalShort}" maxFractionDigits="3"/> sản phẩm</dd></div>
                    </dl>
                </section>

                <section class="inv-card issue-shortage" aria-labelledby="note-shortage-title">
                    <h2 class="issue-card__title" id="note-shortage-title">Xử lý phần thiếu</h2>
                    <c:choose>
                        <c:when test="${empty note.shortageAction}">
                            <p class="issue-shortage__none">Xuất đủ, không có phần thiếu.</p>
                        </c:when>
                        <c:otherwise>
                            <dl class="issue-result">
                                <div><dt>Cách xử lý</dt><dd><span class="inv-pill issue-pill ${note.backorder ? 'issue-pill--short' : 'issue-pill--muted'}">${note.shortageLabel}</span></dd></div>
                                <c:if test="${note.backorder}">
                                    <div><dt>Lịch giao bổ sung</dt><dd>${note.backorderDueText}</dd></div>
                                </c:if>
                                <div><dt>Ghi chú xử lý</dt><dd><c:out value="${empty note.shortageNote ? '—' : note.shortageNote}"/></dd></div>
                            </dl>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${note.status == 'DELIVERED' or note.status == 'FAILED'}">
                        <h2 class="issue-card__title issue-card__title--spaced">Kết quả giao hàng</h2>
                        <dl class="issue-result">
                            <c:choose>
                                <c:when test="${note.status == 'DELIVERED'}">
                                    <div><dt>Người nhận</dt><dd><c:out value="${note.recipientName}"/></dd></div>
                                    <div><dt>Thời điểm giao</dt><dd>${note.deliveredAtText}</dd></div>
                                </c:when>
                                <c:otherwise>
                                    <div><dt>Lý do không giao được</dt><dd><c:out value="${note.failureReason}"/></dd></div>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty note.deliveryRemark}">
                                <div><dt>Ghi chú</dt><dd><c:out value="${note.deliveryRemark}"/></dd></div>
                            </c:if>
                        </dl>
                    </c:if>
                </section>
            </div>

            <section class="inv-card issue-lines" aria-labelledby="note-lines-title">
                <div class="issue-lines__head">
                    <h2 class="issue-card__title" id="note-lines-title">Số lượng thực xuất theo dòng</h2>
                    <p class="issue-lines__count">${fn:length(note.lines)} dòng hàng</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table issue-table" id="note-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã sản phẩm</th>
                                <th scope="col">Tên sản phẩm</th>
                                <th scope="col">Lô xuất</th>
                                <th scope="col" class="is-number">Cần xuất</th>
                                <th scope="col" class="is-number">Thực xuất</th>
                                <th scope="col" class="is-number">Thiếu</th>
                                <th scope="col">Lý do thiếu</th>
                                <th scope="col">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="line" items="${note.lines}">
                                <tr>
                                    <td><span class="issue-table__sku"><c:out value="${line.sku}"/></span></td>
                                    <td>
                                        <span class="inv-table__main"><c:out value="${line.productName}"/></span>
                                        <span class="inv-table__sub"><c:out value="${line.unitName}"/></span>
                                    </td>
                                    <td><span class="issue-table__lots"><c:out value="${empty line.lots ? '—' : line.lots}"/></span></td>
                                    <td class="is-number issue-table__qty"><fmt:formatNumber value="${line.qtyRequired}" maxFractionDigits="3"/></td>
                                    <td class="is-number issue-table__qty"><fmt:formatNumber value="${line.qtyIssued}" maxFractionDigits="3"/></td>
                                    <td class="is-number issue-table__qty${line.shortage ? ' issue-table__short' : ''}"><fmt:formatNumber value="${line.qtyShort}" maxFractionDigits="3"/></td>
                                    <td><c:out value="${empty line.shortReason ? '—' : line.shortReason}"/></td>
                                    <td>
                                        <span class="inv-pill issue-pill ${line.shortage ? 'issue-pill--short' : 'issue-pill--ok'}">${line.shortage ? 'Thiếu' : 'Đủ'}</span>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
