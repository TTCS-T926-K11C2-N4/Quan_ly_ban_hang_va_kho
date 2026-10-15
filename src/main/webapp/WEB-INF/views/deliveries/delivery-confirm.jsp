<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="deliveries"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Xác nhận giao hàng"/>
<c:set var="breadcrumbParentUrl" value="/deliveries?date=${date}"/>
<c:set var="breadcrumbPage" value="${success ? 'Giao thành công' : 'Giao không thành công'}"/>
<c:url var="backUrl" value="/deliveries/point">
    <c:param name="address" value="${point.addressId}"/>
    <c:param name="date" value="${date}"/>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${success ? 'Giao thành công' : 'Giao không thành công'} | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/deliveries.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page delivery-page">
            <header class="inv-header">
                <div>
                    <a class="delivery-back" id="confirm-back" href="${fn:escapeXml(backUrl)}">
                        <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Chi tiết điểm giao
                    </a>
                    <h1 class="inv-header__title">${success ? 'Giao thành công' : 'Giao không thành công'}</h1>
                    <p class="inv-header__subtitle"><c:out value="${point.customerName}"/> • Ngày giao ${dateText}</p>
                </div>
            </header>

            <c:if test="${not empty errors}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="confirm-error" role="alert">Chưa xác nhận được: kiểm tra các ô được đánh dấu đỏ.</div>
            </c:if>

            <form class="delivery-confirm" id="delivery-confirm-form" action="<c:url value='/deliveries/confirm'/>" method="post"
                  enctype="multipart/form-data" novalidate data-mode="${success ? 'success' : 'fail'}" data-max-proofs="${maxProofs}">
                <input type="hidden" name="result" value="${success ? 'success' : 'fail'}">
                <input type="hidden" name="address" value="${point.addressId}">
                <input type="hidden" name="date" value="${date}">
                <c:forEach var="stop" items="${stops}">
                    <input type="hidden" name="note" value="${stop.noteId}">
                </c:forEach>

                <section class="inv-card delivery-info" aria-label="Điểm giao">
                    <p class="delivery-label">Điểm giao</p>
                    <h2 class="delivery-info__name"><c:out value="${point.customerName}"/>
                        <c:if test="${not empty point.label}"><span class="delivery-point__label"> · <c:out value="${point.label}"/></span></c:if></h2>
                    <p class="delivery-point__address"><span class="inv-icon inv-icon--map-pin" aria-hidden="true"></span><c:out value="${point.address}"/></p>
                    <div class="delivery-scope">
                        <span class="inv-icon inv-icon--package" aria-hidden="true"></span>
                        <div>
                            <p class="delivery-label">Phạm vi xác nhận</p>
                            <p class="delivery-scope__text" id="confirm-scope">${fn:length(stops)} đơn:
                                <c:forEach var="stop" items="${stops}" varStatus="loop"><c:out value="${stop.orderNo}"/> (<c:out value="${stop.deliveryNo}"/>)${loop.last ? '' : ', '}</c:forEach></p>
                        </div>
                    </div>
                </section>

                <p class="delivery-notice">
                    <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                    <c:choose>
                        <c:when test="${success}">Chụp rõ biên bản giao hàng có chữ ký của người nhận. Sau khi xác nhận, phiếu xuất chuyển "Đã giao" và đủ điều kiện lập hoá đơn.</c:when>
                        <c:otherwise>Hàng của các đơn này sẽ quay về kho (cộng lại tồn theo lô) và được giữ lại cho đơn để soạn giao lại.</c:otherwise>
                    </c:choose>
                </p>

                <c:choose>
                    <c:when test="${success}">
                        <section class="inv-card delivery-proofs${not empty errors['proofs'] ? ' is-invalid' : ''}" aria-labelledby="proof-title">
                            <div class="delivery-proofs__head">
                                <h2 class="delivery-card-title" id="proof-title">Ảnh chứng từ</h2>
                                <span class="delivery-proofs__count">Số ảnh: <strong data-proof-count>0</strong>/${maxProofs}</span>
                            </div>
                            <div class="delivery-proofs__empty" data-proof-empty>
                                <span class="delivery-proofs__camera"><span class="inv-icon delivery-icon--camera" aria-hidden="true"></span></span>
                                <p class="delivery-proofs__status">Chưa có chứng từ</p>
                                <p class="delivery-proofs__hint">Chụp hoặc chọn ảnh biên bản giao hàng có chữ ký người nhận.</p>
                            </div>
                            <ul class="delivery-proofs__grid" data-proof-list hidden></ul>
                            <div class="delivery-proofs__actions">
                                <label class="inv-button inv-button--outline delivery-proofs__add" for="proof-input">
                                    <span class="inv-icon delivery-icon--image-plus" aria-hidden="true"></span>Thêm ảnh
                                </label>
                                <input class="delivery-proofs__input" type="file" id="proof-input" name="proofs" accept="image/jpeg,image/png"
                                       multiple data-proof-input>
                                <p class="delivery-proofs__format">Định dạng JPG, PNG · tối đa 5MB mỗi ảnh · tối đa ${maxProofs} ảnh</p>
                            </div>
                            <c:if test="${not empty errors['proofs']}"><p class="delivery-error"><c:out value="${errors['proofs']}"/></p></c:if>
                            <c:if test="${not empty errors and empty errors['proofs']}"><p class="delivery-proofs__hint">Trình duyệt không giữ ảnh sau khi gửi lỗi, vui lòng chọn lại ảnh.</p></c:if>
                        </section>

                        <section class="inv-card delivery-fields" aria-label="Thông tin nhận hàng">
                            <div class="inv-field${not empty errors['recipientName'] ? ' inv-field--invalid' : ''}">
                                <label class="delivery-field-label" for="recipient-name">Người nhận <span class="inv-field__required">*</span></label>
                                <input class="inv-control" type="text" id="recipient-name" name="recipientName" maxlength="150"
                                       value="<c:out value='${successForm.recipientName}'/>" placeholder="Họ tên người ký nhận" data-required-field>
                                <c:if test="${not empty errors['recipientName']}"><p class="delivery-error"><c:out value="${errors['recipientName']}"/></p></c:if>
                            </div>
                            <div class="inv-field${not empty errors['deliveredAt'] ? ' inv-field--invalid' : ''}">
                                <label class="delivery-field-label" for="delivered-at">Thời điểm giao <span class="inv-field__required">*</span></label>
                                <input class="inv-control" type="datetime-local" id="delivered-at" name="deliveredAt" max="${nowLocal}"
                                       value="<c:out value='${successForm.deliveredAt}'/>" data-required-field>
                                <c:if test="${not empty errors['deliveredAt']}"><p class="delivery-error"><c:out value="${errors['deliveredAt']}"/></p></c:if>
                            </div>
                            <div class="inv-field delivery-fields__wide${not empty errors['remark'] ? ' inv-field--invalid' : ''}">
                                <label class="delivery-field-label" for="delivery-remark">Ghi chú giao hàng</label>
                                <textarea class="inv-control inv-control--textarea" id="delivery-remark" name="remark" rows="3" maxlength="1000"
                                          placeholder="Ví dụ: Giao tại kho sau, khách đã kiểm đủ số lượng."><c:out value="${successForm.remark}"/></textarea>
                                <p class="delivery-hint">Gợi ý: ghi rõ nơi giao, tình trạng hàng khi bàn giao nếu có.</p>
                                <c:if test="${not empty errors['remark']}"><p class="delivery-error"><c:out value="${errors['remark']}"/></p></c:if>
                            </div>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <section class="inv-card delivery-reasons${not empty errors['reason'] ? ' is-invalid' : ''}" aria-labelledby="reason-title">
                            <h2 class="delivery-card-title" id="reason-title">Lý do giao không thành công</h2>
                            <p class="delivery-hint">Chọn một lý do; chọn "<c:out value="${otherReason}"/>" thì ghi rõ ở ghi chú bổ sung.</p>
                            <fieldset class="delivery-reasons__list">
                                <legend class="visually-hidden">Chọn lý do</legend>
                                <c:forEach var="reason" items="${failureReasons}" varStatus="loop">
                                    <label class="delivery-reason" for="reason-${loop.index}">
                                        <input type="radio" id="reason-${loop.index}" name="reason" value="<c:out value='${reason}'/>"
                                               ${reason == failureForm.reason ? 'checked' : ''} data-reason${reason == otherReason ? ' data-other-reason' : ''}>
                                        <span class="delivery-reason__mark" aria-hidden="true"></span>
                                        <span class="delivery-reason__text"><c:out value="${reason}"/></span>
                                        <span class="delivery-reason__selected">Đã chọn</span>
                                    </label>
                                </c:forEach>
                            </fieldset>
                            <c:if test="${not empty errors['reason']}"><p class="delivery-error"><c:out value="${errors['reason']}"/></p></c:if>
                        </section>
                        <section class="inv-card delivery-fields" aria-label="Ghi chú bổ sung">
                            <div class="inv-field delivery-fields__wide${not empty errors['remark'] ? ' inv-field--invalid' : ''}">
                                <label class="delivery-field-label" for="delivery-remark">Ghi chú bổ sung</label>
                                <textarea class="inv-control inv-control--textarea" id="delivery-remark" name="remark" rows="3" maxlength="1000"
                                          placeholder="Ví dụ: Đã gọi 3 lần không nghe máy, hẹn giao lại sáng mai."><c:out value="${failureForm.remark}"/></textarea>
                                <c:if test="${not empty errors['remark']}"><p class="delivery-error"><c:out value="${errors['remark']}"/></p></c:if>
                            </div>
                        </section>
                    </c:otherwise>
                </c:choose>

                <div class="inv-card delivery-actionbar">
                    <p class="delivery-actionbar__scope">
                        <span class="delivery-label">Điều kiện xác nhận</span>
                        <span id="confirm-condition" data-condition>
                            <c:choose>
                                <c:when test="${success}">Có ít nhất 1 ảnh biên bản có chữ ký và tên người nhận.</c:when>
                                <c:otherwise>Đã chọn lý do giao không thành công.</c:otherwise>
                            </c:choose>
                        </span>
                    </p>
                    <div class="delivery-actionbar__buttons">
                        <a class="inv-button inv-button--outline" href="${fn:escapeXml(backUrl)}" id="confirm-cancel">Huỷ</a>
                        <button class="inv-button delivery-button ${success ? 'delivery-button--success' : 'delivery-button--fail'}" type="submit" id="confirm-submit" data-confirm-submit>
                            <span class="inv-icon delivery-icon--lock" aria-hidden="true" data-lock-icon></span>
                            <span class="inv-icon ${success ? 'inv-icon--circle-check' : 'inv-icon--circle-x'}" aria-hidden="true" data-ready-icon hidden></span>
                            ${success ? 'Xác nhận giao thành công' : 'Xác nhận giao không thành công'}
                        </button>
                    </div>
                </div>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/deliveries.js'/>"></script>
</body>
</html>
