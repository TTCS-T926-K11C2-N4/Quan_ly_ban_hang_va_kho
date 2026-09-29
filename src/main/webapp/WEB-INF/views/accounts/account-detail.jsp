<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Quản lý tài khoản"/>
<c:url var="viewUrl" value="/accounts/view"><c:param name="id" value="${account.id}"/></c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết tài khoản | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <%-- Khi hộp thoại đang mở, nội dung phía sau không nhận focus/click --%>
    <div class="app-shell"${lockMode || unlockMode ? ' inert' : ''}>
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header page-header--with-action">
                <div>
                    <h1 class="page-header__title">Chi tiết tài khoản</h1>
                    <p class="page-header__subtitle">Xem thông tin, vai trò và trạng thái hoạt động của tài khoản.</p>
                </div>
                <%-- Người chỉ có quyền xem (vd Quản lý kinh doanh) không thấy nút; không tự khóa chính mình --%>
                <c:if test="${currentUser.can('USER_MANAGE') and account.id != currentUser.id}">
                <c:choose>
                    <c:when test="${account.status.locked}">
                        <a class="button button--success" id="unlock-account-link"
                           href="<c:url value='/accounts/unlock'><c:param name='id' value='${account.id}'/></c:url>">Mở khóa tài khoản</a>
                    </c:when>
                    <c:otherwise>
                        <a class="button button--danger-outline" id="lock-account-link"
                           href="<c:url value='/accounts/lock'><c:param name='id' value='${account.id}'/></c:url>">Khóa tài khoản</a>
                    </c:otherwise>
                </c:choose>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="account-detail-message" role="status">
                    <p><c:out value="${flashMessage}"/></p>
                </div>
            </c:if>

            <section class="account-detail" aria-labelledby="account-name">
                <div class="account-profile">
                    <span class="account-profile__avatar" aria-hidden="true"><c:out value="${account.initials}"/></span>
                    <div>
                        <h2 class="account-profile__name" id="account-name"><c:out value="${account.fullName}"/></h2>
                        <p class="account-profile__username">@<c:out value="${account.username}"/></p>
                        <span class="status-badge status-badge--${fn:toLowerCase(account.status.code)}">${account.status.label}</span>
                    </div>
                </div>

                <h3 class="account-detail__title">Thông tin tài khoản</h3>
                <dl class="detail-grid">
                    <div>
                        <dt>Email</dt>
                        <dd><c:out value="${account.email}"/></dd>
                    </div>
                    <div>
                        <dt>Số điện thoại</dt>
                        <dd><c:out value="${empty account.phone ? '—' : account.phone}"/></dd>
                    </div>
                    <div>
                        <dt>Ngày tạo</dt>
                        <dd>${account.createdAtText}</dd>
                    </div>
                    <div>
                        <dt>Đăng nhập gần nhất</dt>
                        <dd>${empty account.lastLoginAtText ? 'Chưa đăng nhập' : account.lastLoginAtText}</dd>
                    </div>
                </dl>

                <h3 class="account-detail__title account-detail__title--spaced">Vai trò và phạm vi phụ trách</h3>
                <div class="account-detail__roles">
                    <c:forEach var="role" items="${account.roles}">
                        <span class="role-badge role-badge--${fn:toLowerCase(role.code)}"><c:out value="${role.name}"/></span>
                    </c:forEach>
                    <c:if test="${empty account.roles}">
                        <span class="account-detail__muted">Chưa có vai trò</span>
                    </c:if>
                </div>
                <dl class="detail-grid detail-grid--assignments">
                    <div>
                        <dt>Kho phụ trách</dt>
                        <dd><c:out value="${empty account.warehouseNamesText ? '—' : account.warehouseNamesText}"/></dd>
                    </div>
                    <div>
                        <dt>Địa bàn phụ trách</dt>
                        <dd><c:out value="${empty account.regionNamesText ? '—' : account.regionNamesText}"/></dd>
                    </div>
                </dl>

                <h3 class="account-detail__title account-detail__title--spaced">Nhật ký gần nhất</h3>
                <p class="account-detail__muted account-detail__activity">
                    <c:choose>
                        <c:when test="${not empty account.latestActivity}">
                            <c:set var="activity" value="${account.latestActivity}"/>
                            ${activity.label}<c:if test="${not empty activity.reason}"> · Lý do: <c:out value="${activity.reason}"/></c:if> · ${activity.occurredAtText}
                        </c:when>
                        <c:otherwise>Chưa có nhật ký</c:otherwise>
                    </c:choose>
                </p>
            </section>
        </main>
    </div>

    <c:if test="${lockMode}">
        <div class="modal-backdrop">
            <div class="modal" role="dialog" aria-modal="true" aria-labelledby="lock-dialog-title" aria-describedby="lock-dialog-desc">
                <form id="lock-account-form" action="<c:url value='/accounts/lock'/>" method="post" novalidate
                      data-cancel-url="${viewUrl}" data-min-reason-length="${lockReasonMinLength}">
                    <input type="hidden" name="id" value="${account.id}">
                    <div class="modal__body">
                        <div class="modal__header">
                            <span class="modal__icon" aria-hidden="true">!</span>
                            <div>
                                <h2 class="modal__title" id="lock-dialog-title">Khóa tài khoản</h2>
                                <p class="modal__desc" id="lock-dialog-desc">Tài khoản sẽ không thể đăng nhập cho đến khi được mở khóa.</p>
                            </div>
                        </div>

                        <c:if test="${account.handoverRequired}">
                            <div class="handover-warning" id="handover-warning">
                                <p class="handover-warning__title">Cần bàn giao địa bàn phụ trách</p>
                                <p class="handover-warning__text">
                                    <%-- Viết liền một dòng để không sinh khoảng trắng trước dấu chấm --%>
                                    <c:out value="${account.fullName}"/> đang phụ trách <c:if test="${not empty account.regionNamesText}">địa bàn <c:out value="${account.regionNamesText}"/></c:if><c:if test="${not empty account.regionNamesText && account.customerCount > 0}"> và </c:if><c:if test="${account.customerCount > 0}">${account.customerCount} đại lý</c:if>.<br>
                                    Hãy chọn người nhận bàn giao trước khi khóa.
                                </p>
                            </div>

                            <div class="form-group modal__field${not empty errors.handoverUserId ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="handover-user-id">Bàn giao cho *</label>
                                <div class="select">
                                    <select class="form-group__control select__control" id="handover-user-id" name="handoverUserId"
                                            aria-describedby="handover-error" aria-invalid="${not empty errors.handoverUserId}" autofocus>
                                        <option value="">Chọn người nhận bàn giao</option>
                                        <c:forEach var="candidate" items="${handoverCandidates}">
                                            <option value="${candidate.id}"${handoverUserId == candidate.id ? ' selected' : ''}><c:out value="${candidate.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <p class="form-group__error" id="handover-error"${empty errors.handoverUserId ? ' hidden' : ''}><c:out value="${errors.handoverUserId}"/></p>
                            </div>
                        </c:if>

                        <div class="form-group modal__field${not empty errors.lockReason ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="lock-reason">Lý do khóa *</label>
                            <textarea class="form-group__control form-group__control--textarea" id="lock-reason" name="lockReason"
                                      rows="4" maxlength="1000" required${account.handoverRequired ? '' : ' autofocus'}
                                      aria-describedby="lock-reason-hint lock-reason-error"
                                      aria-invalid="${not empty errors.lockReason}"><c:out value="${lockReason}"/></textarea>
                            <p class="form-group__hint" id="lock-reason-hint">Bắt buộc nhập tối thiểu ${lockReasonMinLength} ký tự.</p>
                            <p class="form-group__error" id="lock-reason-error"${empty errors.lockReason ? ' hidden' : ''}><c:out value="${errors.lockReason}"/></p>
                        </div>
                    </div>

                    <div class="modal__footer">
                        <a class="button button--secondary" id="cancel-lock-account" href="${viewUrl}">Hủy</a>
                        <button class="button button--danger" type="submit" id="confirm-lock-account">Khóa tài khoản</button>
                    </div>
                </form>
            </div>
        </div>
    </c:if>

    <c:if test="${unlockMode}">
        <div class="modal-backdrop">
            <div class="modal" role="dialog" aria-modal="true" aria-labelledby="unlock-dialog-title" aria-describedby="unlock-dialog-desc">
                <form id="unlock-account-form" action="<c:url value='/accounts/unlock'/>" method="post" data-cancel-url="${viewUrl}">
                    <input type="hidden" name="id" value="${account.id}">
                    <div class="modal__body">
                        <div class="modal__header">
                            <span class="modal__icon modal__icon--success" aria-hidden="true">✓</span>
                            <div>
                                <h2 class="modal__title" id="unlock-dialog-title">Mở khóa tài khoản?</h2>
                                <p class="modal__desc" id="unlock-dialog-desc">
                                    <c:out value="${account.fullName}"/> sẽ có thể đăng nhập và tiếp tục sử dụng
                                    những chức năng thuộc vai trò được phân quyền.
                                </p>
                            </div>
                        </div>

                        <div class="status-preview">
                            <p class="status-preview__title">Trạng thái sau khi mở khóa</p>
                            <p class="status-preview__text">Hoạt động · Giữ nguyên vai trò và kho phụ trách</p>
                            <c:if test="${handoverOnLastLock}">
                                <p class="status-preview__note">
                                    Địa bàn/đại lý đã bàn giao khi khóa không tự trả lại — gán lại ở màn Sửa nếu cần.
                                </p>
                            </c:if>
                        </div>
                    </div>

                    <div class="modal__footer">
                        <a class="button button--secondary" id="cancel-unlock-account" href="${viewUrl}">Hủy</a>
                        <button class="button button--success" type="submit" id="confirm-unlock-account" autofocus>Xác nhận mở khóa</button>
                    </div>
                </form>
            </div>
        </div>
    </c:if>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <c:if test="${lockMode || unlockMode}">
        <script src="<c:url value='/assets/js/account-dialog.js'/>"></script>
    </c:if>
</body>
</html>
