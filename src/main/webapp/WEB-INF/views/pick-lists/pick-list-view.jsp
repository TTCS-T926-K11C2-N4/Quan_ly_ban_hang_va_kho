<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="pick-lists"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Phiếu soạn hàng"/>
<c:set var="breadcrumbParentUrl" value="/pick-lists"/>
<c:set var="breadcrumbPage" value="${pickList.pickNo}"/>
<%-- Nút xác nhận xuất kho (S6-05) chỉ hiện khi ứng dụng đã có màn đó --%>
<c:set var="hasIssueScreen" value="${not empty pageContext.servletContext.servletRegistrations['com.oms.controller.DeliveryNoteConfirmServlet']}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu soạn <c:out value="${pickList.pickNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/pick-lists.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page pick-view">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Phiếu soạn hàng <c:out value="${pickList.pickNo}"/></h1>
                    <p class="inv-header__subtitle">
                        <c:out value="${pickList.warehouseName}"/> • Ngày giao ${pickList.deliveryDateText} • ${fn:length(pickList.orders)} đơn hàng
                    </p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button inv-button--outline" id="pick-back" href="<c:url value='/pick-lists'/>">
                        <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Danh sách phiếu
                    </a>
                    <button class="inv-button" type="button" id="pick-print" data-print>
                        <span class="inv-icon pick-icon--printer" aria-hidden="true"></span>In phiếu
                    </button>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="pick-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="pick-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card pick-summary" aria-label="Thông tin phiếu">
                <div class="pick-summary__id">
                    <div>
                        <p class="pick-summary__label">Mã phiếu</p>
                        <p class="pick-summary__code" id="pick-no"><c:out value="${pickList.pickNo}"/></p>
                    </div>
                    <span class="inv-pill pick-pill pick-pill--${pickList.tone}" id="pick-status">${pickList.statusLabel}</span>
                </div>
                <dl class="pick-summary__meta">
                    <div>
                        <dt>Ngày</dt>
                        <dd>${pickList.createdDateText}</dd>
                    </div>
                    <div>
                        <dt>Nhân viên</dt>
                        <dd id="pick-assignee">
                            <c:choose>
                                <c:when test="${not empty pickList.assignedName}"><c:out value="${pickList.assignedName}"/></c:when>
                                <c:otherwise>Chưa có người nhận</c:otherwise>
                            </c:choose>
                        </dd>
                    </div>
                    <div>
                        <dt>Kho</dt>
                        <dd><c:out value="${pickList.warehouseName}"/></dd>
                    </div>
                    <div>
                        <dt>Người lập</dt>
                        <dd><c:out value="${pickList.createdByName}"/></dd>
                    </div>
                </dl>
                <div class="pick-progress">
                    <div class="pick-progress__row">
                        <span class="pick-progress__label">Tiến độ</span>
                        <span class="pick-progress__text" id="pick-progress-text"><strong data-picked-count>${pickList.pickedLines}</strong> / ${fn:length(pickList.lines)} dòng</span>
                    </div>
                    <div class="pick-progress__bar" role="progressbar" aria-label="Tiến độ soạn" aria-valuemin="0"
                         aria-valuemax="${fn:length(pickList.lines)}" aria-valuenow="${pickList.pickedLines}">
                        <span class="pick-progress__fill" style="width: ${pickList.progressPercent}%"></span>
                    </div>
                    <c:if test="${pickList.shortLines > 0}">
                        <p class="pick-progress__short" id="pick-short-count">${pickList.shortLines} dòng soạn thiếu</p>
                    </c:if>
                </div>
            </section>

            <section class="inv-card pick-orders" aria-labelledby="pick-orders-title">
                <div class="pick-section__head pick-orders__head">
                    <div>
                        <h2 class="pick-section__title" id="pick-orders-title">Đơn trong phiếu</h2>
                        <p class="pick-section__count">${fn:length(pickList.orders)} đơn gộp trong một lượt soạn</p>
                    </div>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table pick-table" id="pick-orders-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã đơn</th>
                                <th scope="col">Khách hàng</th>
                                <th scope="col">Ngày giao</th>
                                <th scope="col">Xuất kho</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="order" items="${pickList.orders}">
                                <c:url var="orderUrl" value="/orders/view"><c:param name="id" value="${order.orderId}"/></c:url>
                                <tr>
                                    <td><a class="inv-table__code" href="${fn:escapeXml(orderUrl)}"><c:out value="${order.orderNo}"/></a></td>
                                    <td><span class="inv-table__main"><c:out value="${order.customerName}"/></span></td>
                                    <td>${order.deliveryDateText}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${order.issued and hasIssueScreen}">
                                                <c:url var="noteUrl" value="/delivery-notes/view"><c:param name="id" value="${order.deliveryNoteId}"/></c:url>
                                                <a class="pick-orders__note" href="${fn:escapeXml(noteUrl)}">
                                                    <span class="inv-icon inv-icon--circle-check" aria-hidden="true"></span><c:out value="${order.deliveryNo}"/>
                                                </a>
                                            </c:when>
                                            <c:when test="${order.issued}"><span class="inv-pill pick-pill pick-pill--issued">Đã xuất kho</span></c:when>
                                            <c:when test="${canIssue and hasIssueScreen}">
                                                <c:url var="issueUrl" value="/delivery-notes/confirm">
                                                    <c:param name="pickList" value="${pickList.id}"/>
                                                    <c:param name="order" value="${order.orderId}"/>
                                                </c:url>
                                                <a class="inv-button inv-button--primary inv-button--small" href="${fn:escapeXml(issueUrl)}"
                                                   id="issue-${order.orderId}">Xác nhận xuất kho</a>
                                            </c:when>
                                            <c:when test="${pickList.done}"><span class="inv-pill pick-pill pick-pill--open">Chờ xác nhận xuất kho</span></c:when>
                                            <c:otherwise><span class="pick-orders__muted">Chưa soạn xong</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </section>

            <form class="inv-card pick-lines" id="pick-form" action="<c:url value='/pick-lists/pick'/>" method="post" novalidate>
                <input type="hidden" name="id" value="${pickList.id}">
                <div class="pick-section__head pick-lines__head">
                    <div>
                        <h2 class="pick-section__title">Danh sách hàng</h2>
                        <p class="pick-section__count">Sắp theo đường đi trong kho; lô hạn sử dụng gần nhất được chọn trước.</p>
                    </div>
                </div>
                <div class="pick-lines__columns" aria-hidden="true">
                    <span>Đã soạn</span>
                    <span>Hàng và vị trí</span>
                    <span class="pick-lines__columns-qty">SL cần</span>
                    <span class="pick-lines__columns-qty">SL lấy</span>
                </div>
                <ol class="pick-lines__list" id="pick-lines">
                    <c:forEach var="line" items="${pickList.lines}" varStatus="loop">
                        <c:set var="input" value="${empty inputs ? null : inputs[line.id]}"/>
                        <c:set var="lineError" value="${empty errors ? null : errors[line.id]}"/>
                        <c:set var="picked" value="${empty inputs ? line.picked : input.picked}"/>
                        <c:set var="qtyValue" value="${empty inputs ? line.qtyPickedText : input.qty}"/>
                        <c:set var="reasonValue" value="${empty inputs ? line.shortReason : input.reason}"/>
                        <c:set var="required" value="${line.qtyRequired.stripTrailingZeros().toPlainString()}"/>
                        <li class="pick-line${picked ? ' is-picked' : ''}${not empty lineError ? ' is-invalid' : ''}" id="line-${line.id}"
                            data-required="${required}">
                            <div class="pick-line__check">
                                <c:choose>
                                    <c:when test="${canPick}">
                                        <input class="pick-line__checkbox" type="checkbox" id="picked-${line.id}" name="picked_${line.id}" value="1"
                                               ${picked ? 'checked' : ''} data-pick-check>
                                        <label class="pick-line__check-button" for="picked-${line.id}">
                                            <span class="inv-icon pick-icon--check" aria-hidden="true"></span>
                                            <span class="visually-hidden">Đã soạn dòng ${loop.index + 1}</span>
                                        </label>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pick-line__check-button${line.picked ? ' is-on' : ''}" aria-label="${line.picked ? 'Đã soạn' : 'Chưa soạn'}">
                                            <span class="inv-icon pick-icon--check" aria-hidden="true"></span>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="pick-line__info">
                                <p class="pick-line__name"><c:out value="${line.productName}"/></p>
                                <p class="pick-line__sub"><c:out value="${line.sku}"/> • Đơn <c:out value="${line.orderNo}"/>
                                    <c:if test="${not empty line.lotText}"> • <c:out value="${line.lotText}"/></c:if></p>
                                <span class="pick-line__location">
                                    <span class="inv-icon inv-icon--map-pin" aria-hidden="true"></span>
                                    <c:choose>
                                        <c:when test="${not empty line.locationCode}">
                                            <c:out value="${line.locationCode}"/><c:if test="${not empty line.locationName}"> · <c:out value="${line.locationName}"/></c:if>
                                        </c:when>
                                        <c:otherwise>Chưa gán vị trí</c:otherwise>
                                    </c:choose>
                                </span>
                            </div>
                            <div class="pick-line__qty">
                                <span class="pick-line__qty-label">Cần</span>
                                <span class="pick-line__required">${required} <span class="pick-line__unit"><c:out value="${line.unitName}"/></span></span>
                            </div>
                            <div class="pick-line__qty">
                                <label class="pick-line__qty-label" for="qty-${line.id}">Lấy</label>
                                <c:choose>
                                    <c:when test="${canPick}">
                                        <input class="inv-control pick-line__input" type="text" inputmode="decimal" id="qty-${line.id}"
                                               name="qty_${line.id}" value="<c:out value='${qtyValue}'/>" maxlength="16" placeholder="—"
                                               autocomplete="off" data-pick-qty>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pick-line__picked">${line.picked ? line.qtyPickedText : '—'}</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <c:choose>
                                <c:when test="${canPick}">
                                    <div class="pick-line__reason${not empty reasonValue or line.shortage or not empty lineError ? '' : ' is-hidden'}" data-pick-reason>
                                        <label class="pick-line__reason-label" for="reason-${line.id}">Lý do soạn thiếu</label>
                                        <select class="inv-control" id="reason-${line.id}" name="reason_${line.id}">
                                            <option value="">Chọn lý do</option>
                                            <c:forEach var="reason" items="${shortReasons}">
                                                <option value="<c:out value='${reason}'/>"${reason == reasonValue ? ' selected' : ''}><c:out value="${reason}"/></option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </c:when>
                                <c:when test="${line.shortage}">
                                    <p class="pick-line__reason-text">Thiếu ${line.qtyRequired.subtract(line.qtyPicked).stripTrailingZeros().toPlainString()}: <c:out value="${line.shortReason}"/></p>
                                </c:when>
                            </c:choose>
                            <c:if test="${not empty lineError}">
                                <p class="pick-line__error" role="alert"><c:out value="${lineError}"/></p>
                            </c:if>
                        </li>
                    </c:forEach>
                </ol>

                <c:if test="${canPick or canCancel}">
                    <div class="pick-actions">
                        <c:if test="${canCancel}">
                            <button class="inv-button inv-button--outline pick-actions__danger" type="submit" form="pick-cancel-form" id="pick-cancel"
                                    data-confirm="Huỷ phiếu soạn ${pickList.pickNo}? Các đơn sẽ quay về chờ soạn.">Huỷ phiếu</button>
                        </c:if>
                        <span class="pick-actions__spacer"></span>
                        <c:if test="${canPick}">
                        <button class="inv-button inv-button--outline" type="submit" name="action" value="save" id="pick-save">
                            <span class="inv-icon inv-icon--save" aria-hidden="true"></span>Lưu tiến độ
                        </button>
                        <button class="inv-button inv-button--primary pick-actions__complete" type="submit" name="action" value="complete" id="pick-complete">
                            <span class="inv-icon pick-icon--check" aria-hidden="true"></span>Hoàn tất
                        </button>
                        </c:if>
                    </div>
                </c:if>
            </form>
            <c:if test="${canCancel}">
                <form id="pick-cancel-form" action="<c:url value='/pick-lists/cancel'/>" method="post" hidden>
                    <input type="hidden" name="id" value="${pickList.id}">
                </form>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/pick-lists.js'/>"></script>
</body>
</html>
