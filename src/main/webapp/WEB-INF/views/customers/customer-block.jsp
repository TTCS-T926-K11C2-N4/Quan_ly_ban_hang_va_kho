<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Đại lý"/>
<c:set var="breadcrumbPage" value="Khoá/mở giao dịch"/>
<c:set var="customerTab" value="block"/>
<c:set var="blocking" value="${not customer.blocked}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Khoá/mở giao dịch đại lý | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page">
            <header class="page-header">
                <h1 class="page-header__title">Chi tiết đại lý</h1>
                <p class="page-header__subtitle">Khoá giao dịch và ghi nhận lý do khoá đại lý.</p>
            </header>

            <%@ include file="/WEB-INF/views/customers/customer-tabs.jspf" %>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="customer-block-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="sales-card" aria-labelledby="customer-name">
                <div class="customer-head">
                    <div>
                        <h2 class="customer-head__name" id="customer-name"><c:out value="${customer.name}"/></h2>
                        <p class="customer-head__code">Mã đại lý: <c:out value="${customer.code}"/></p>
                    </div>
                    <span class="status-pill${customer.blocked ? ' status-pill--blocked' : customer.active ? '' : ' status-pill--inactive'}" id="customer-status"><c:out value="${customer.statusLabel}"/></span>
                </div>

                <dl class="customer-info">
                    <div>
                        <dt class="customer-info__label">Người liên hệ</dt>
                        <dd class="customer-info__value"><c:out value="${empty customer.contactName ? '—' : customer.contactName}"/></dd>
                    </div>
                    <div>
                        <dt class="customer-info__label">Số điện thoại</dt>
                        <dd class="customer-info__value"><c:out value="${empty customer.phone ? '—' : customer.phone}"/></dd>
                    </div>
                    <div>
                        <dt class="customer-info__label">Địa chỉ</dt>
                        <dd class="customer-info__value"><c:out value="${empty customer.address ? '—' : customer.address}"/></dd>
                    </div>
                </dl>

                <section class="sales-section" aria-labelledby="block-title">
                    <div class="section-head">
                        <div>
                            <h3 class="section-head__title" id="block-title">Khoá/mở giao dịch đại lý</h3>
                            <p class="section-head__desc">${blocking ? 'Xác nhận khoá giao dịch của đại lý.' : 'Đại lý đang bị khoá: không tạo được đơn mới cho tới khi mở lại giao dịch.'}</p>
                        </div>
                    </div>

                    <c:if test="${customer.blocked}">
                        <div class="block-current" id="block-current">
                            <p class="block-current__title">Lý do khoá hiện tại</p>
                            <p class="block-current__reason"><c:out value="${customer.blockReason}"/></p>
                            <c:if test="${not empty latestBlock and latestBlock.block}">
                                <p class="block-current__meta">Khoá lúc <c:out value="${latestBlock.createdAtText}"/><c:if test="${not empty latestBlock.actorName}"> bởi <c:out value="${latestBlock.actorName}"/></c:if></p>
                            </c:if>
                        </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${currentUser.can('CREDIT_MANAGE')}">
                            <form class="sales-panel sales-panel--lock" id="block-form" method="post" action="<c:url value='/customers/block/save'/>" novalidate>
                                <input type="hidden" name="id" value="${customer.id}">
                                <input type="hidden" name="action" value="${blocking ? 'BLOCK' : 'UNBLOCK'}">
                                <h4 class="sales-panel__title">${blocking ? 'Khoá giao dịch đại lý' : 'Mở lại giao dịch đại lý'}</h4>

                                <c:if test="${not empty formError}">
                                    <p class="sales-flash sales-flash--error" role="alert"><c:out value="${formError}"/></p>
                                </c:if>

                                <div class="sales-field${not empty reasonError ? ' sales-field--invalid' : ''}">
                                    <label class="sales-field__label" for="block-reason">${blocking ? 'Lý do khoá' : 'Lý do mở khoá'}<span class="sales-field__required">*</span></label>
                                    <textarea class="sales-field__control sales-field__control--textarea" id="block-reason" name="reason"
                                              maxlength="500" required aria-describedby="block-reason-hint" aria-invalid="${not empty reasonError}"
                                              placeholder="${blocking ? 'Nhập lý do khoá giao dịch của đại lý...' : 'Nhập lý do mở lại giao dịch, ví dụ: đại lý đã thanh toán đủ nợ quá hạn...'}"><c:out value="${reason}"/></textarea>
                                    <p class="${not empty reasonError ? 'sales-field__error' : 'sales-field__hint'}" id="block-reason-hint"><c:out value="${not empty reasonError ? reasonError : (blocking ? 'Bắt buộc nhập lý do trước khi xác nhận khoá.' : 'Bắt buộc nhập lý do trước khi mở lại giao dịch.')}"/></p>
                                </div>

                                <div class="sales-actions">
                                    <a class="sales-button" href="<c:url value='/customers/block'><c:param name='id' value='${customer.id}'/></c:url>">Hủy</a>
                                    <button class="sales-button sales-button--primary" type="submit" id="confirm-block">${blocking ? 'Xác nhận khoá' : 'Xác nhận mở khoá'}</button>
                                </div>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="sales-empty">Chỉ Kế toán công nợ và Quản lý kinh doanh được khoá hoặc mở giao dịch đại lý.</p>
                        </c:otherwise>
                    </c:choose>

                    <p class="sales-note sales-note--warning" id="open-order-note">
                        <span class="sales-note__icon" aria-hidden="true">
                            <img src="<c:url value='/assets/img/icons/warning.svg'/>" alt="" width="15" height="15">
                        </span>
                        <span>
                            <span class="sales-note__title">Lưu ý về đơn đang dở</span><br>
                            ${blocking ? 'Sau khi khoá, đơn đang dở vẫn được xử lý nhưng hệ thống sẽ hiển thị cảnh báo.' : 'Trong thời gian khoá, đơn đang dở vẫn được xử lý nhưng có cảnh báo.'}
                            <c:if test="${openOrderCount > 0}"> Đại lý đang có <strong>${openOrderCount}</strong> đơn chưa xuất kho.</c:if>
                        </span>
                    </p>
                </section>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
