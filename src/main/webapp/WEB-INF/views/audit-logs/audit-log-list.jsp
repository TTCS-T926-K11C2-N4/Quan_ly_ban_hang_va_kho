<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="audit-logs"/>
<c:set var="breadcrumbSection" value="Quản trị hệ thống"/>
<c:set var="breadcrumbPage" value="Nhật ký thao tác"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhật ký thao tác | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <%-- Dùng lại bộ lọc, bảng, phân trang của nhóm màn hình Quản lý tài khoản --%>
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/audit-logs.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Nhật ký thao tác</h1>
                <p class="page-header__subtitle">Ai đã thay đổi tồn kho, giá, hạn mức công nợ, hoá đơn và tài khoản, vào lúc nào, giá trị trước và sau.</p>
            </header>

            <form class="account-filters audit-filters" id="audit-filter-form" action="<c:url value='/audit-logs'/>" method="get" role="search">
                <div class="account-filters__field audit-filters__field--actor">
                    <label class="account-filters__label" for="actor-filter">Người thực hiện</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="actor-filter" name="actorUserId">
                            <option value="">Tất cả</option>
                            <c:forEach var="actor" items="${actors}">
                                <option value="${actor.id}"${filter.actorUserId == actor.id ? ' selected' : ''}><c:out value="${actor.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field audit-filters__field--type">
                    <label class="account-filters__label" for="entity-type-filter">Loại đối tượng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="entity-type-filter" name="entityType">
                            <option value="">Tất cả</option>
                            <c:forEach var="type" items="${entityTypes}">
                                <option value="${type.code}"${filter.entityType == type ? ' selected' : ''}>${type.label}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field audit-filters__field--date${not empty errors.fromDate ? ' form-group--invalid' : ''}">
                    <label class="account-filters__label" for="from-date">Từ ngày</label>
                    <input class="account-filters__control" type="date" id="from-date" name="fromDate"
                           value="${filter.fromDate}" aria-describedby="date-error">
                </div>

                <div class="account-filters__field audit-filters__field--date${not empty errors.toDate ? ' form-group--invalid' : ''}">
                    <label class="account-filters__label" for="to-date">Đến ngày</label>
                    <input class="account-filters__control" type="date" id="to-date" name="toDate"
                           value="${filter.toDate}" aria-describedby="date-error">
                </div>

                <a class="button button--secondary audit-filters__clear" id="clear-audit-filter" href="<c:url value='/audit-logs'/>">Xoá bộ lọc</a>
            </form>
            <c:if test="${not empty errors}">
                <p class="audit-filters__error" id="date-error" role="alert">
                    <c:out value="${not empty errors.fromDate ? errors.fromDate : errors.toDate}"/> Đang hiện nhật ký không lọc theo ngày.
                </p>
            </c:if>

            <section class="account-table-card" aria-label="Danh sách nhật ký thao tác">
                <div class="account-table-scroll">
                    <table class="account-table audit-table">
                        <thead>
                            <tr>
                                <th scope="col">Thời điểm</th>
                                <th scope="col">Người thực hiện</th>
                                <th scope="col">Thao tác</th>
                                <th scope="col">Đối tượng</th>
                                <th scope="col">Giá trị trước</th>
                                <th scope="col">Giá trị sau</th>
                                <th scope="col">Lý do</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="log" items="${logPage.items}">
                                <tr>
                                    <td class="audit-table__time">
                                        ${log.occurredAtText}
                                        <c:if test="${not empty log.ipAddress}"><span class="audit-table__sub">IP <c:out value="${log.ipAddress}"/></span></c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty log.actorName}">
                                                <span class="account-table__name"><c:out value="${log.actorName}"/></span>
                                                <span class="audit-table__sub">@<c:out value="${log.actorUsername}"/></span>
                                            </c:when>
                                            <%-- Thao tác không cần đăng nhập, vd tự đăng ký tài khoản --%>
                                            <c:otherwise><span class="audit-table__sub">Không đăng nhập</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><c:out value="${log.actionLabel}"/></td>
                                    <td>
                                        <c:out value="${log.entityTypeLabel}"/>
                                        <c:if test="${not empty log.entityId}"><span class="audit-table__sub">#${log.entityId}</span></c:if>
                                    </td>
                                    <td><c:choose><c:when test="${not empty log.oldValues}"><code class="audit-table__json"><c:out value="${log.oldValues}"/></code></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                                    <td><c:choose><c:when test="${not empty log.newValues}"><code class="audit-table__json"><c:out value="${log.newValues}"/></code></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                                    <td><c:out value="${empty log.reason ? '—' : log.reason}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty logPage.items}">
                                <tr>
                                    <td class="audit-table__empty" colspan="7">Không có nhật ký phù hợp.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <c:url var="baseUrl" value="/audit-logs">
                    <c:param name="actorUserId" value="${filter.actorUserId}"/>
                    <c:param name="entityType" value="${filter.entityType.code}"/>
                    <c:param name="fromDate" value="${filter.fromDate}"/>
                    <c:param name="toDate" value="${filter.toDate}"/>
                </c:url>
                <div class="account-pagination">
                    <p class="account-pagination__info">${logPage.totalItems} bản ghi · ${logPage.pageSize} dòng/trang</p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${logPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:out value='${baseUrl}'/>&amp;page=${logPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${logPage.startPage}" end="${logPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == logPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:out value='${baseUrl}'/>&amp;page=${pageNumber}">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${logPage.page < logPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:out value='${baseUrl}'/>&amp;page=${logPage.page + 1}">›</a>
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
    <script src="<c:url value='/assets/js/audit-logs.js'/>"></script>
</body>
</html>
