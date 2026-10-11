<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Quản lý đại lý"/>
<c:set var="breadcrumbPage" value="Chi tiết đại lý"/>
<c:set var="customerTab" value="profile"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hồ sơ đại lý | Hệ thống quản lý bán hàng &amp; kho</title>
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
            <header class="page-header customer-list-header">
                <div>
                    <h1 class="page-header__title">Chi tiết đại lý</h1>
                    <p class="page-header__subtitle">Xem thông tin hồ sơ đại lý và trạng thái giao dịch.</p>
                </div>
                <c:if test="${canManage}">
                    <div class="profile-actions">
                        <a class="sales-button" id="edit-customer" href="<c:url value='/customers/edit'><c:param name='id' value='${profile.id}'/></c:url>">Chỉnh sửa</a>
                        <form method="post" action="<c:url value='/customers/profile/action'/>"
                              data-confirm="${profile.active ? 'Ngừng giao dịch với đại lý này? Hồ sơ vẫn được giữ lại, đại lý không tạo được đơn mới.' : 'Cho đại lý giao dịch lại?'}">
                            <input type="hidden" name="id" value="${profile.id}">
                            <button class="sales-button ${profile.active ? 'profile-actions__stop' : 'sales-button--soft'}" type="submit" name="action"
                                    value="${profile.active ? 'deactivate' : 'activate'}" id="toggle-customer-status">${profile.active ? 'Ngừng giao dịch' : 'Giao dịch lại'}</button>
                        </form>
                        <c:if test="${not profile.hasTransactions}">
                            <form method="post" action="<c:url value='/customers/profile/action'/>"
                                  data-confirm="Xoá hẳn đại lý ${fn:escapeXml(profile.code)}? Đại lý chưa phát sinh giao dịch nên xoá được.">
                                <input type="hidden" name="id" value="${profile.id}">
                                <button class="sales-button profile-actions__delete" type="submit" name="action" value="remove" id="delete-customer">Xoá</button>
                            </form>
                        </c:if>
                    </div>
                </c:if>
            </header>

            <%@ include file="/WEB-INF/views/customers/customer-tabs.jspf" %>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="profile-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="sales-card profile-head" aria-labelledby="profile-name">
                <div>
                    <p class="profile-head__code">Mã đại lý · <c:out value="${profile.code}"/></p>
                    <h2 class="profile-head__name" id="profile-name"><c:out value="${profile.name}"/></h2>
                    <p class="profile-head__note">${profile.hasTransactions ? 'Đại lý đã có giao dịch trong hệ thống' : 'Đại lý chưa phát sinh giao dịch'}</p>
                </div>
                <span class="customer-status customer-status--${fn:toLowerCase(profile.displayStatus.code)}" id="profile-status">${profile.displayStatus.label}</span>
            </section>

            <div class="profile-grid">
                <section class="sales-card" aria-labelledby="profile-info-title">
                    <h3 class="profile-card__title" id="profile-info-title">Thông tin hồ sơ</h3>
                    <dl class="profile-info">
                        <div><dt>Mã đại lý</dt><dd><c:out value="${profile.code}"/></dd></div>
                        <div><dt>Tên đại lý</dt><dd><c:out value="${profile.name}"/></dd></div>
                        <div><dt>Mã số thuế</dt><dd><c:out value="${empty profile.taxCode ? '—' : profile.taxCode}"/></dd></div>
                        <div><dt>Số điện thoại</dt><dd>
                            <c:choose>
                                <c:when test="${empty profile.phone}">—</c:when>
                                <c:otherwise><a class="profile-info__link" href="tel:${fn:escapeXml(fn:replace(profile.phone, ' ', ''))}"><c:out value="${profile.phone}"/></a></c:otherwise>
                            </c:choose>
                        </dd></div>
                        <div><dt>Email</dt><dd><c:out value="${empty profile.email ? '—' : profile.email}"/></dd></div>
                        <div class="profile-info__wide"><dt>Địa chỉ</dt><dd><c:out value="${empty profile.address ? '—' : profile.address}"/></dd></div>
                    </dl>
                </section>

                <section class="sales-card" aria-labelledby="profile-manage-title">
                    <h3 class="profile-card__title" id="profile-manage-title">Thông tin quản lý</h3>
                    <dl class="profile-info">
                        <div class="profile-info__wide"><dt>Nhóm khách hàng</dt><dd><c:out value="${profile.customerGroupName}"/>
                            <span class="profile-info__hint">Áp dụng bảng giá của nhóm này khi tạo đơn hàng</span></dd></div>
                        <div><dt>Khu vực</dt><dd><c:out value="${profile.regionName}"/></dd></div>
                        <div><dt>Người phụ trách</dt><dd><c:out value="${empty profile.salesRepName ? 'Chưa phân công' : profile.salesRepName}"/></dd></div>
                        <div><dt>Kho phục vụ mặc định</dt><dd><c:out value="${empty profile.defaultWarehouseName ? 'Chưa chọn (chưa tạo được đơn)' : profile.defaultWarehouseName}"/></dd></div>
                        <div><dt>Trạng thái hồ sơ</dt><dd>${profile.active ? 'Đang hoạt động' : 'Ngừng giao dịch'}</dd></div>
                        <div><dt>Khoá giao dịch</dt><dd>
                            <c:choose>
                                <c:when test="${profile.blocked}"><a class="profile-info__link" href="<c:url value='/customers/block'><c:param name='id' value='${profile.id}'/></c:url>">Đang khoá giao dịch</a></c:when>
                                <c:otherwise>Không</c:otherwise>
                            </c:choose>
                        </dd></div>
                        <div><dt>Tình trạng giao dịch</dt><dd>${profile.hasTransactions ? 'Đã phát sinh giao dịch' : 'Chưa phát sinh giao dịch'}</dd></div>
                    </dl>
                </section>
            </div>

            <c:choose>
                <c:when test="${profile.hasTransactions}">
                    <p class="sales-note sales-note--warning profile-notice" id="profile-notice">
                        <span class="sales-note__icon" aria-hidden="true"><img src="<c:url value='/assets/img/icons/warning.svg'/>" alt="" width="15" height="15"></span>
                        <span><span class="sales-note__title">Đại lý đã có giao dịch</span><br>
                            Đại lý này đã phát sinh giao dịch nên không xoá được. Khi cần dừng hoạt động, dùng "Ngừng giao dịch" để giữ lại hồ sơ và lịch sử.</span>
                    </p>
                </c:when>
                <c:otherwise>
                    <p class="sales-note profile-notice" id="profile-notice">
                        <span><span class="sales-note__title">Đại lý chưa phát sinh giao dịch</span><br>
                            Hồ sơ nhập nhầm có thể xoá. Sau khi đại lý có đơn hàng, hoá đơn hoặc công nợ thì chỉ còn ngừng giao dịch được.</span>
                    </p>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/customers.js'/>"></script>
</body>
</html>
