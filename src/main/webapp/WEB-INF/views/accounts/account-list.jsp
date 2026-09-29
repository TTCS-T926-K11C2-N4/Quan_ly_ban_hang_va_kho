<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
<c:set var="breadcrumbPage" value="Quản lý tài khoản"/>
<%-- Người chỉ có quyền xem (vd Quản lý kinh doanh) không thấy nút tạo, sửa, khóa --%>
<c:set var="canManage" value="${currentUser.can('USER_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách tài khoản người dùng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Danh sách tài khoản người dùng</h1>
                <p class="page-header__subtitle">Quản lý tài khoản, vai trò và trạng thái truy cập trong toàn hệ thống.</p>
            </header>

            <c:if test="${not empty createdUsername}">
                <div class="account-flash" id="account-created-message" role="status">
                    <p>Đã tạo tài khoản <strong><c:out value="${createdUsername}"/></strong>.</p>
                    <p>
                        Mật khẩu tạm: <code class="account-flash__password"><c:out value="${temporaryPassword}"/></code>
                        — chỉ hiển thị một lần, hãy gửi cho người dùng. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.
                    </p>
                </div>
            </c:if>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="account-updated-message" role="status">
                    <p><c:out value="${flashMessage}"/></p>
                </div>
            </c:if>

            <form class="account-filters" id="account-filter-form" action="<c:url value='/accounts'/>" method="get" role="search">
                <div class="account-filters__field account-filters__field--keyword">
                    <label class="account-filters__label" for="account-keyword">Tìm kiếm</label>
                    <input class="account-filters__control" type="search" id="account-keyword" name="keyword"
                           value="<c:out value='${keyword}'/>" placeholder="Tìm theo tên, tài khoản hoặc số điện thoại"
                           maxlength="100">
                </div>

                <div class="account-filters__field account-filters__field--role">
                    <label class="account-filters__label" for="role-filter">Vai trò</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="role-filter" name="roleFilter">
                            <option value="">Tất cả vai trò</option>
                            <c:forEach var="role" items="${roles}">
                                <option value="<c:out value='${role.code}'/>"${role.code == roleFilter ? ' selected' : ''}><c:out value="${role.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field account-filters__field--status">
                    <label class="account-filters__label" for="status-filter">Trạng thái</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="status-filter" name="statusFilter">
                            <option value="">Tất cả</option>
                            <c:forEach var="status" items="${statuses}">
                                <option value="${status.code}"${status.code == statusFilter ? ' selected' : ''}>${status.label}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <c:if test="${canManage}">
                    <a class="account-filters__create" id="create-account-link" href="<c:url value='/accounts/new'/>">
                        <span aria-hidden="true">+</span> Tạo tài khoản
                    </a>
                </c:if>
            </form>

            <section class="account-table-card" aria-label="Danh sách tài khoản">
                <div class="account-table-scroll">
                    <table class="account-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Họ tên</th>
                                <th scope="col">Tài khoản</th>
                                <th scope="col">Số điện thoại</th>
                                <th scope="col">Vai trò</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="account" items="${accountPage.items}" varStatus="loop">
                                <tr>
                                    <td class="account-table__index">${accountPage.firstRowNumber + loop.index}</td>
                                    <td class="account-table__name"><c:out value="${account.fullName}"/></td>
                                    <td><c:out value="${account.username}"/></td>
                                    <td><c:out value="${account.phone}"/></td>
                                    <td>
                                        <div class="account-table__roles">
                                            <c:forEach var="role" items="${account.roles}">
                                                <span class="role-badge role-badge--${fn:toLowerCase(role.code)}"><c:out value="${role.name}"/></span>
                                            </c:forEach>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="status-badge status-badge--${fn:toLowerCase(account.status.code)}">${account.status.label}</span>
                                    </td>
                                    <td>
                                        <div class="row-actions">
                                            <a class="row-actions__link" href="<c:url value='/accounts/view'><c:param name='id' value='${account.id}'/></c:url>">Xem</a>
                                            <c:if test="${canManage}">
                                            <a class="row-actions__link" href="<c:url value='/accounts/edit'><c:param name='id' value='${account.id}'/></c:url>">Sửa</a>
                                            <details class="row-menu">
                                                <summary class="row-menu__toggle" aria-label="Thao tác khác cho tài khoản ${fn:escapeXml(account.username)}">⋮</summary>
                                                <div class="row-menu__list">
                                                    <c:choose>
                                                        <c:when test="${account.status.locked}">
                                                            <a class="row-menu__item" href="<c:url value='/accounts/unlock'><c:param name='id' value='${account.id}'/></c:url>">Mở khóa</a>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a class="row-menu__item row-menu__item--danger" href="<c:url value='/accounts/lock'><c:param name='id' value='${account.id}'/></c:url>">Khóa tài khoản</a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </details>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">Hiển thị ${accountPage.pageSize} dòng/trang</p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${accountPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/accounts'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='roleFilter' value='${roleFilter}'/>
                                            <c:param name='statusFilter' value='${statusFilter}'/><c:param name='page' value='${accountPage.page - 1}'/>
                                        </c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${accountPage.startPage}" end="${accountPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == accountPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/accounts'>
                                                <c:param name='keyword' value='${keyword}'/><c:param name='roleFilter' value='${roleFilter}'/>
                                                <c:param name='statusFilter' value='${statusFilter}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${accountPage.page < accountPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/accounts'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='roleFilter' value='${roleFilter}'/>
                                            <c:param name='statusFilter' value='${statusFilter}'/><c:param name='page' value='${accountPage.page + 1}'/>
                                        </c:url>">›</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/accounts.js'/>"></script>
</body>
</html>
