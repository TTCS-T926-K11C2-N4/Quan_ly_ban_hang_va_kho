<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Đại lý"/>
<c:set var="breadcrumbPage" value="Hạn mức công nợ"/>
<c:set var="customerTab" value="credit-limit"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hạn mức công nợ | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <p class="page-header__subtitle">Quản lý thông tin và hạn mức công nợ của đại lý.</p>
            </header>

            <c:choose>
                <c:when test="${empty customer}">
                    <%-- Chưa có danh sách đại lý (S3-03): chọn đại lý trong phạm vi được xem --%>
                    <section class="sales-card" aria-labelledby="customer-picker-title">
                        <div>
                            <h2 class="sales-card__title" id="customer-picker-title">Chọn đại lý</h2>
                            <p class="sales-card__subtitle">Chọn đại lý để xem và cập nhật hạn mức công nợ.</p>
                        </div>
                        <c:choose>
                            <c:when test="${empty customers}">
                                <p class="sales-empty">Chưa có đại lý nào trong phạm vi bạn phụ trách.</p>
                            </c:when>
                            <c:otherwise>
                                <form class="customer-picker" method="get" action="<c:url value='/customers/credit-limit'/>">
                                    <div class="sales-field">
                                        <label class="sales-field__label" for="customer-id">Đại lý</label>
                                        <select class="sales-field__control" id="customer-id" name="id" required>
                                            <c:forEach var="option" items="${customers}">
                                                <option value="${option.id}"><c:out value="${option.name}"/> (<c:out value="${option.code}"/>)</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <button class="sales-button sales-button--primary" type="submit">Xem hạn mức</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </c:when>
                <c:otherwise>
                    <%@ include file="/WEB-INF/views/customers/customer-tabs.jspf" %>

                    <c:if test="${not empty flashMessage}">
                        <div class="sales-flash" id="credit-limit-message" role="status"><c:out value="${flashMessage}"/></div>
                    </c:if>

                    <section class="sales-card" aria-labelledby="customer-name">
                        <div class="customer-head">
                            <div>
                                <h2 class="customer-head__name" id="customer-name"><c:out value="${customer.name}"/></h2>
                                <p class="customer-head__code">Mã đại lý: <c:out value="${customer.code}"/></p>
                            </div>
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
                                <dt class="customer-info__label">Trạng thái</dt>
                                <dd class="customer-info__value"><c:out value="${customer.statusLabel}"/></dd>
                            </div>
                        </dl>

                        <section class="sales-section" aria-labelledby="credit-title">
                            <div class="section-head">
                                <div>
                                    <h3 class="section-head__title" id="credit-title">Hạn mức công nợ</h3>
                                    <p class="section-head__desc">Giới hạn số tiền và số ngày đại lý được phép nợ.</p>
                                </div>
                                <c:if test="${currentUser.can('CREDIT_MANAGE') and empty form}">
                                    <a class="sales-button sales-button--soft sales-button--small" id="edit-credit-limit"
                                       href="<c:url value='/customers/credit-limit'><c:param name='id' value='${customer.id}'/><c:param name='edit' value='1'/></c:url>#credit-form">
                                        <img class="sales-button__icon" src="<c:url value='/assets/img/icons/pencil.svg'/>" alt="" width="15" height="15">
                                        Chỉnh sửa
                                    </a>
                                </c:if>
                            </div>

                            <div class="credit-limits">
                                <div class="credit-limit">
                                    <p class="credit-limit__label">Hạn mức tiền tối đa</p>
                                    <p class="credit-limit__value" id="credit-limit-value"><fmt:formatNumber value="${customer.creditLimit}" maxFractionDigits="0"/><span class="credit-limit__unit">₫</span></p>
                                    <p class="credit-limit__hint">Số tiền tối đa đại lý được phép nợ</p>
                                </div>
                                <div class="credit-limit">
                                    <p class="credit-limit__label">Số ngày nợ tối đa</p>
                                    <p class="credit-limit__value" id="debt-days-value">${customer.maxDebtDays}<span class="credit-limit__unit">ngày</span></p>
                                    <p class="credit-limit__hint">Thời gian nợ tối đa kể từ ngày phát sinh</p>
                                </div>
                            </div>

                            <p class="sales-note">
                                <span class="sales-note__icon" aria-hidden="true">
                                    <img src="<c:url value='/assets/img/icons/permission.svg'/>" alt="" width="15" height="15">
                                </span>
                                <span><span class="sales-note__title">Phân quyền chỉnh sửa:</span> Kế toán công nợ và Quản lý kinh doanh được phép chỉnh sửa hạn mức công nợ.</span>
                            </p>

                            <c:if test="${not empty form}">
                                <form class="sales-panel" id="credit-form" method="post" action="<c:url value='/customers/credit-limit/edit'/>" novalidate>
                                    <input type="hidden" name="id" value="${customer.id}">
                                    <input type="hidden" name="version" value="<c:out value='${form.version}'/>">
                                    <h4 class="sales-panel__title">Sửa hạn mức công nợ</h4>

                                    <c:if test="${not empty errors.form}">
                                        <p class="sales-flash sales-flash--error" role="alert"><c:out value="${errors.form}"/></p>
                                    </c:if>

                                    <div class="sales-field__row">
                                        <div class="sales-field${not empty errors.creditLimit ? ' sales-field--invalid' : ''}">
                                            <label class="sales-field__label" for="credit-limit">Hạn mức tiền tối đa<span class="sales-field__required">*</span></label>
                                            <div class="sales-field__suffix-wrap">
                                                <input class="sales-field__control" type="text" id="credit-limit" name="creditLimit"
                                                       inputmode="numeric" maxlength="22" required autocomplete="off" data-money
                                                       value="<c:out value='${form.creditLimit}'/>"
                                                       aria-describedby="credit-limit-hint" aria-invalid="${not empty errors.creditLimit}">
                                                <span class="sales-field__suffix" aria-hidden="true">₫</span>
                                            </div>
                                            <p class="${not empty errors.creditLimit ? 'sales-field__error' : 'sales-field__hint'}" id="credit-limit-hint"><c:out value="${not empty errors.creditLimit ? errors.creditLimit : 'Bắt buộc nhập.'}"/></p>
                                        </div>
                                        <div class="sales-field${not empty errors.maxDebtDays ? ' sales-field--invalid' : ''}">
                                            <label class="sales-field__label" for="max-debt-days">Số ngày nợ tối đa<span class="sales-field__required">*</span></label>
                                            <div class="sales-field__suffix-wrap">
                                                <input class="sales-field__control" type="number" id="max-debt-days" name="maxDebtDays"
                                                       min="0" max="365" step="1" required
                                                       value="<c:out value='${form.maxDebtDays}'/>"
                                                       aria-describedby="max-debt-days-hint" aria-invalid="${not empty errors.maxDebtDays}">
                                                <span class="sales-field__suffix" aria-hidden="true">ngày</span>
                                            </div>
                                            <p class="${not empty errors.maxDebtDays ? 'sales-field__error' : 'sales-field__hint'}" id="max-debt-days-hint"><c:out value="${not empty errors.maxDebtDays ? errors.maxDebtDays : 'Bắt buộc nhập.'}"/></p>
                                        </div>
                                    </div>

                                    <%-- AC2 của S3-05: thay đổi hạn mức bắt buộc có lý do (thiết kế chưa có ô này) --%>
                                    <div class="sales-field${not empty errors.reason ? ' sales-field--invalid' : ''}">
                                        <label class="sales-field__label" for="credit-reason">Lý do thay đổi<span class="sales-field__required">*</span></label>
                                        <textarea class="sales-field__control sales-field__control--textarea" id="credit-reason" name="reason"
                                                  maxlength="500" required placeholder="Ví dụ: Đại lý thanh toán đúng hạn 6 tháng liên tiếp..."
                                                  aria-describedby="credit-reason-hint" aria-invalid="${not empty errors.reason}"><c:out value="${form.reason}"/></textarea>
                                        <p class="${not empty errors.reason ? 'sales-field__error' : 'sales-field__hint'}" id="credit-reason-hint"><c:out value="${not empty errors.reason ? errors.reason : 'Bắt buộc nhập. Lý do được ghi vào lịch sử hạn mức và nhật ký thao tác.'}"/></p>
                                    </div>

                                    <div class="sales-actions">
                                        <a class="sales-button" href="<c:url value='/customers/credit-limit'><c:param name='id' value='${customer.id}'/></c:url>">Hủy</a>
                                        <button class="sales-button sales-button--primary" type="submit" id="save-credit-limit">Lưu thay đổi</button>
                                    </div>
                                </form>
                            </c:if>
                        </section>
                    </section>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/customers.js'/>"></script>
</body>
</html>
