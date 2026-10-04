<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="audit-logs"/>
<c:set var="breadcrumbSection" value="Tổng quan"/>
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
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/audit.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title audit-header__title">Nhật ký thao tác</h1>
                <p class="page-header__subtitle">Theo dõi lịch sử thao tác trên hệ thống: ai đã thêm, sửa, xoá dữ liệu gì, lúc nào, giá trị trước và sau.</p>
            </header>

            <form class="account-filters audit-filters" id="audit-filter-form" action="<c:url value='/audit-logs'/>" method="get" role="search">
                <fieldset class="account-filters__field audit-filters__period">
                    <legend class="account-filters__label">Thời gian</legend>
                    <div class="audit-period">
                        <input class="account-filters__control audit-period__date" type="date" id="audit-from" name="from"
                               value="${fromDate}" aria-label="Từ ngày">
                        <span class="audit-period__arrow" aria-hidden="true">→</span>
                        <input class="account-filters__control audit-period__date" type="date" id="audit-to" name="to"
                               value="${toDate}" aria-label="Đến ngày">
                    </div>
                </fieldset>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="audit-actor-filter">Người dùng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="audit-actor-filter" name="actorId">
                            <option value="">Tất cả</option>
                            <c:forEach var="actor" items="${actors}">
                                <option value="${actor.id}"${actor.id == actorFilter ? ' selected' : ''}><c:out value="${actor.name}"/> (<c:out value="${actor.code}"/>)</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="audit-group-filter">Hành động</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="audit-group-filter" name="actionGroup">
                            <option value="">Tất cả</option>
                            <c:forEach var="group" items="${groups}">
                                <option value="${group.key}"${group.key == groupFilter ? ' selected' : ''}><c:out value="${group.value}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="account-filters__field">
                    <label class="account-filters__label" for="audit-entity-filter">Đối tượng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="audit-entity-filter" name="entityType">
                            <option value="">Tất cả</option>
                            <c:forEach var="entity" items="${entities}">
                                <option value="${entity.key}"${entity.key == entityFilter ? ' selected' : ''}><c:out value="${entity.value}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <div class="audit-filters__actions">
                    <a class="audit-filters__reset" id="reset-audit-filters" href="<c:url value='/audit-logs'/>">
                        <span aria-hidden="true">⟳</span> Làm mới
                    </a>
                    <a class="account-filters__create audit-filters__export" id="export-audit-logs"
                       title="Xuất các dòng đang lọc (tối đa ${exportMaxRows} dòng, mới nhất trước)"
                       href="<c:url value='/audit-logs/export'><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='actorId' value='${actorFilter}'/><c:param name='actionGroup' value='${groupFilter}'/><c:param name='entityType' value='${entityFilter}'/></c:url>">
                        <span aria-hidden="true">⇩</span> Xuất Excel
                    </a>
                </div>
            </form>

            <section class="account-table-card" aria-label="Nhật ký thao tác">
                <div class="account-table-scroll">
                    <table class="account-table audit-table" id="audit-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Thời gian</th>
                                <th scope="col">Người dùng</th>
                                <th scope="col">Hành động</th>
                                <th scope="col">Đối tượng</th>
                                <th scope="col">Nội dung chi tiết</th>
                                <th scope="col">IP truy cập</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="log" items="${logPage.items}" varStatus="loop">
                                <c:set var="actorText" value="${empty log.actorName ? 'Khách (tự đăng ký)' : log.actorName}"/>
                                <tr>
                                    <td class="account-table__index">${logPage.firstRowNumber + loop.index}</td>
                                    <td class="audit-time"><c:out value="${log.occurredAtText}"/></td>
                                    <td class="account-table__name"><c:out value="${actorText}"/></td>
                                    <td><span class="status-badge audit-badge--${fn:toLowerCase(log.actionGroup)}"><c:out value="${log.actionLabel}"/></span></td>
                                    <td><c:out value="${log.entityLabel}"/></td>
                                    <td class="audit-summary"><c:out value="${log.summary}"/></td>
                                    <td class="audit-ip"><c:out value="${empty log.ipAddress ? '—' : log.ipAddress}"/></td>
                                    <td>
                                        <button class="row-menu__toggle audit-detail-button" type="button"
                                                aria-label="Xem chi tiết: ${fn:escapeXml(log.summary)}" title="Xem chi tiết"
                                                data-summary="${fn:escapeXml(log.summary)}" data-time="${fn:escapeXml(log.occurredAtText)}"
                                                data-actor="${fn:escapeXml(actorText)}" data-ip="${fn:escapeXml(log.ipAddress)}"
                                                data-reason="${fn:escapeXml(log.reason)}"
                                                data-old="${fn:escapeXml(log.oldValues)}" data-new="${fn:escapeXml(log.newValues)}">•••</button>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty logPage.items}">
                                <tr>
                                    <td class="category-table__empty" colspan="8">Không có thao tác nào phù hợp với bộ lọc.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">
                        <c:choose>
                            <c:when test="${logPage.totalItems == 0}">Hiển thị 0 kết quả</c:when>
                            <c:otherwise>
                                Hiển thị ${logPage.firstRowNumber}-${logPage.firstRowNumber + fn:length(logPage.items) - 1}
                                trong ${logPage.totalItems} kết quả
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${logPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/audit-logs'><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='actorId' value='${actorFilter}'/><c:param name='actionGroup' value='${groupFilter}'/><c:param name='entityType' value='${entityFilter}'/><c:param name='page' value='${logPage.page - 1}'/></c:url>">‹</a>
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
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/audit-logs'><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='actorId' value='${actorFilter}'/><c:param name='actionGroup' value='${groupFilter}'/><c:param name='entityType' value='${entityFilter}'/><c:param name='page' value='${pageNumber}'/></c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${logPage.page < logPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/audit-logs'><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='actorId' value='${actorFilter}'/><c:param name='actionGroup' value='${groupFilter}'/><c:param name='entityType' value='${entityFilter}'/><c:param name='page' value='${logPage.page + 1}'/></c:url>">›</a>
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

    <%-- Hộp chi tiết: audit-logs.js điền nội dung từ data-* của nút ••• ở dòng được bấm --%>
    <dialog class="audit-dialog" id="audit-detail-dialog" aria-labelledby="audit-detail-title">
        <%-- Lớp trong phủ kín hộp để bấm vào vùng nền mờ (chính thẻ dialog) mới đóng --%>
        <div class="audit-dialog__inner">
        <div class="audit-dialog__header">
            <div>
                <h2 class="audit-dialog__title" id="audit-detail-title">Chi tiết thao tác</h2>
                <p class="audit-dialog__summary" id="audit-detail-summary"></p>
            </div>
            <button class="audit-dialog__close" type="button" id="audit-detail-close" aria-label="Đóng">×</button>
        </div>
        <dl class="audit-dialog__meta">
            <div><dt>Người thực hiện</dt><dd id="audit-detail-actor"></dd></div>
            <div><dt>Thời gian</dt><dd id="audit-detail-time"></dd></div>
            <div><dt>IP truy cập</dt><dd id="audit-detail-ip"></dd></div>
            <div id="audit-detail-reason-row"><dt>Lý do</dt><dd id="audit-detail-reason"></dd></div>
        </dl>
        <div class="audit-dialog__changes">
            <table class="audit-changes" id="audit-detail-changes">
                <thead>
                    <tr>
                        <th scope="col">Trường dữ liệu</th>
                        <th scope="col">Giá trị trước</th>
                        <th scope="col">Giá trị sau</th>
                    </tr>
                </thead>
                <tbody></tbody>
            </table>
            <p class="audit-changes__empty" id="audit-detail-empty" hidden>Thao tác này không lưu giá trị trước và sau.</p>
        </div>
        </div>
    </dialog>
    <%-- Tên ứng với id/mã trong giá trị trước/sau (vd categoryId 20 -> Đồ uống); để trong thuộc tính đã escape --%>
    <div id="audit-name-lookups" data-json="${fn:escapeXml(nameLookupsJson)}" hidden></div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/audit-logs.js'/>"></script>
</body>
</html>
