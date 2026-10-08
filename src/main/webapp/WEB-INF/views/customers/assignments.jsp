<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="assignments"/>
<c:set var="breadcrumbSection" value="Đại lý"/>
<c:set var="breadcrumbPage" value="Phân công kinh doanh"/>
<c:set var="salesRepParam" value="${filter.unassigned ? 'none' : filter.salesRepId}"/>
<%-- Link giữ điều kiện lọc hiện tại cho phân trang, mở/đóng khung lịch sử --%>
<c:url var="listUrl" value="/customers/assignments">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="region" value="${filter.regionId}"/>
    <c:param name="group" value="${filter.customerGroupId}"/>
    <c:param name="salesRep" value="${salesRepParam}"/>
</c:url>
<c:set var="listHref" value="${fn:escapeXml(listUrl)}"/>
<c:set var="selectedCount" value="${empty selectedIds ? 0 : fn:length(selectedIds)}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phân công nhân viên kinh doanh | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header assignment-header">
                <div>
                    <h1 class="page-header__title">Phân công nhân viên kinh doanh</h1>
                    <p class="page-header__subtitle">Mỗi đại lý có một nhân viên kinh doanh phụ trách chính. Nhân viên chỉ nhìn thấy đại lý mình phụ trách.</p>
                </div>
                <c:if test="${canAssign and not transferOpen}">
                    <a class="customer-filters__submit assignment-header__transfer" id="open-transfer" href="${listHref}&amp;transfer=1#transfer-form">Chuyển giao hàng loạt</a>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="assignment-message" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>

            <c:if test="${canAssign and transferOpen}">
                <form class="assignment-transfer" id="transfer-form" method="post" action="<c:url value='/customers/assignments/transfer'/>" novalidate>
                    <c:forEach var="name" items="${['keyword', 'region', 'group', 'salesRep', 'page']}">
                        <c:if test="${not empty param[name]}"><input type="hidden" name="${name}" value="<c:out value='${param[name]}'/>"></c:if>
                    </c:forEach>
                    <div>
                        <h2 class="assignment-transfer__title">Chuyển giao hàng loạt</h2>
                        <p class="assignment-transfer__desc">Dùng khi nhân viên nghỉ hoặc đổi tuyến: chuyển các đại lý và địa bàn của nhân viên bàn giao sang nhân viên nhận. Mỗi đại lý được ghi lịch sử chuyển giao.</p>
                    </div>
                    <div class="assignment-transfer__grid">
                        <div class="customer-filters__field${not empty transferErrors.fromSalesRepId ? ' assignment-field--invalid' : ''}">
                            <label class="customer-filters__label" for="transfer-from">Nhân viên bàn giao *</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="transfer-from" name="fromSalesRepId" required>
                                    <option value="">Chọn nhân viên</option>
                                    <c:forEach var="rep" items="${salesReps}">
                                        <option value="${rep.id}"${rep.id == transferFromId ? ' selected' : ''}><c:out value="${rep.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <c:if test="${not empty transferErrors.fromSalesRepId}"><p class="assignment-field__error"><c:out value="${transferErrors.fromSalesRepId}"/></p></c:if>
                        </div>
                        <div class="customer-filters__field${not empty transferErrors.toSalesRepId ? ' assignment-field--invalid' : ''}">
                            <label class="customer-filters__label" for="transfer-to">Nhân viên nhận *</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="transfer-to" name="toSalesRepId" required>
                                    <option value="">Chọn nhân viên</option>
                                    <c:forEach var="rep" items="${activeReps}">
                                        <option value="${rep.id}"${rep.id == transferToId ? ' selected' : ''}><c:out value="${rep.name}"/><c:if test="${not empty rep.code}"> — <c:out value="${rep.code}"/></c:if></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <c:if test="${not empty transferErrors.toSalesRepId}"><p class="assignment-field__error"><c:out value="${transferErrors.toSalesRepId}"/></p></c:if>
                        </div>
                        <div class="customer-filters__field">
                            <label class="customer-filters__label" for="transfer-region">Khu vực</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="transfer-region" name="transferRegionId">
                                    <option value="">Tất cả khu vực</option>
                                    <c:forEach var="region" items="${regions}">
                                        <option value="${region.id}"${region.id == transferRegionId ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="customer-filters__field assignment-transfer__reason${not empty transferErrors.reason ? ' assignment-field--invalid' : ''}">
                            <label class="customer-filters__label" for="transfer-reason">Lý do chuyển giao *</label>
                            <input class="account-filters__control" type="text" id="transfer-reason" name="reason" maxlength="500" required
                                   value="<c:out value='${transferReason}'/>" placeholder="Ví dụ: nhân viên nghỉ việc từ 15/10, bàn giao toàn bộ tuyến">
                            <c:if test="${not empty transferErrors.reason}"><p class="assignment-field__error"><c:out value="${transferErrors.reason}"/></p></c:if>
                        </div>
                    </div>

                    <c:if test="${not empty transferPreview}">
                        <div class="assignment-preview" id="transfer-preview" role="status">
                            <p class="assignment-preview__title">Sẽ chuyển <strong>${fn:length(transferPreview.customers())}</strong> đại lý
                                và <strong>${fn:length(transferPreview.regions())}</strong> địa bàn</p>
                            <c:if test="${not empty transferPreview.regions()}">
                                <p class="assignment-preview__line">Địa bàn: <c:forEach var="regionName" items="${transferPreview.regions()}" varStatus="loop"><c:out value="${regionName}"/><c:if test="${not loop.last}">, </c:if></c:forEach></p>
                            </c:if>
                            <c:if test="${not empty transferPreview.customers()}">
                                <ul class="assignment-preview__list">
                                    <c:forEach var="customer" items="${transferPreview.customers()}" end="9">
                                        <li><c:out value="${customer.code}"/> · <c:out value="${customer.name}"/></li>
                                    </c:forEach>
                                    <c:if test="${fn:length(transferPreview.customers()) > 10}"><li>… và ${fn:length(transferPreview.customers()) - 10} đại lý khác</li></c:if>
                                </ul>
                            </c:if>
                        </div>
                    </c:if>

                    <div class="assignment-transfer__actions">
                        <a class="sales-button" href="${listHref}">Hủy</a>
                        <button class="sales-button${empty transferPreview ? ' sales-button--primary' : ''}" type="submit" name="action" value="preview" id="transfer-preview-button">Xem trước</button>
                        <c:if test="${not empty transferPreview}">
                            <button class="sales-button sales-button--primary" type="submit" name="action" value="confirm" id="transfer-confirm">Xác nhận chuyển giao</button>
                        </c:if>
                    </div>
                </form>
            </c:if>

            <div class="assignment-layout${not empty historyCustomer ? ' assignment-layout--with-history' : ''}">
                <div class="assignment-main">
                    <form class="customer-filters customer-filters--open" id="assignment-filter-form" action="<c:url value='/customers/assignments'/>" method="get" role="search">
                        <div class="customer-filters__search">
                            <div class="customer-filters__field customer-filters__field--keyword">
                                <label class="customer-filters__label" for="assignment-keyword">Tìm kiếm</label>
                                <div class="customer-filters__search-box">
                                    <img class="customer-filters__search-icon" src="<c:url value='/assets/img/icons/search.svg'/>" alt="" width="18" height="18">
                                    <input class="account-filters__control customer-filters__keyword" type="search" id="assignment-keyword" name="keyword"
                                           value="<c:out value='${filter.keyword}'/>" maxlength="100" placeholder="Mã đại lý, tên đại lý, số điện thoại..." autocomplete="off">
                                </div>
                            </div>
                            <div class="customer-filters__actions">
                                <button class="customer-filters__submit" type="submit">Tìm kiếm</button>
                            </div>
                        </div>
                        <div class="customer-filters__fields">
                            <div class="customer-filters__field">
                                <label class="customer-filters__label" for="assignment-region">Khu vực</label>
                                <div class="select">
                                    <select class="account-filters__control select__control" id="assignment-region" name="region">
                                        <option value="">Tất cả</option>
                                        <c:forEach var="region" items="${regions}">
                                            <option value="${region.id}"${region.id == filter.regionId ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                            <div class="customer-filters__field">
                                <label class="customer-filters__label" for="assignment-group">Nhóm khách hàng</label>
                                <div class="select">
                                    <select class="account-filters__control select__control" id="assignment-group" name="group">
                                        <option value="">Tất cả</option>
                                        <c:forEach var="group" items="${groups}">
                                            <option value="${group.id}"${group.id == filter.customerGroupId ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                            <c:if test="${canViewAll}">
                                <div class="customer-filters__field">
                                    <label class="customer-filters__label" for="assignment-sales-rep">Nhân viên phụ trách</label>
                                    <div class="select">
                                        <select class="account-filters__control select__control" id="assignment-sales-rep" name="salesRep">
                                            <option value="">Tất cả</option>
                                            <option value="none"${filter.unassigned ? ' selected' : ''}>Chưa phân công</option>
                                            <c:forEach var="rep" items="${salesReps}">
                                                <option value="${rep.id}"${rep.id == filter.salesRepId ? ' selected' : ''}><c:out value="${rep.name}"/></option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>
                            </c:if>
                            <c:if test="${filter.filtered}">
                                <a class="customer-filters__clear" href="<c:url value='/customers/assignments'/>">Xoá bộ lọc</a>
                            </c:if>
                        </div>
                    </form>

                    <c:if test="${canAssign}">
                        <form class="assignment-bar${not empty assignErrors ? ' assignment-bar--invalid' : ''}" id="assign-form" method="post"
                              action="<c:url value='/customers/assignments/assign'/>" novalidate>
                            <c:forEach var="name" items="${['keyword', 'region', 'group', 'salesRep', 'page']}">
                                <c:if test="${not empty param[name]}"><input type="hidden" name="${name}" value="<c:out value='${param[name]}'/>"></c:if>
                            </c:forEach>
                            <p class="assignment-bar__count">Đã chọn <strong id="assign-selected-count">${selectedCount}</strong> đại lý</p>
                            <div class="select assignment-bar__rep">
                                <label class="visually-hidden" for="assign-to">Giao cho nhân viên</label>
                                <select class="account-filters__control select__control" id="assign-to" name="toSalesRepId" required>
                                    <option value="">Giao cho nhân viên...</option>
                                    <c:forEach var="rep" items="${activeReps}">
                                        <option value="${rep.id}"${rep.id == assignToId ? ' selected' : ''}><c:out value="${rep.name}"/><c:if test="${not empty rep.code}"> — <c:out value="${rep.code}"/></c:if></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <label class="visually-hidden" for="assign-reason">Lý do</label>
                            <input class="account-filters__control assignment-bar__reason" type="text" id="assign-reason" name="reason" maxlength="500"
                                   value="<c:out value='${assignReason}'/>" placeholder="Lý do (không bắt buộc)">
                            <button class="customer-filters__submit" type="submit" id="assign-submit"${selectedCount == 0 ? ' disabled' : ''}>Phân công</button>
                            <c:if test="${not empty assignErrors}">
                                <p class="assignment-field__error assignment-bar__error" role="alert">
                                    <c:forEach var="error" items="${assignErrors}" varStatus="loop"><c:out value="${error.value}"/><c:if test="${not loop.last}"> </c:if></c:forEach>
                                </p>
                            </c:if>
                        </form>
                    </c:if>

                    <section class="account-table-card" aria-label="Danh sách đại lý và nhân viên phụ trách">
                        <div class="account-table-scroll">
                            <table class="account-table customer-table assignment-table" id="assignment-table">
                                <thead>
                                    <tr>
                                        <c:if test="${canAssign}">
                                            <th scope="col" class="assignment-table__check">
                                                <input type="checkbox" id="assign-select-all" aria-label="Chọn tất cả đại lý trên trang">
                                            </th>
                                        </c:if>
                                        <th scope="col">Mã đại lý</th>
                                        <th scope="col">Tên đại lý</th>
                                        <th scope="col">Khu vực</th>
                                        <th scope="col">Nhóm khách hàng</th>
                                        <th scope="col">Nhân viên phụ trách</th>
                                        <th scope="col">Ngày phân công</th>
                                        <th scope="col">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="customer" items="${customerPage.items}">
                                        <tr class="${customer.id == historyCustomer.id ? 'assignment-table__row--active' : ''}">
                                            <c:if test="${canAssign}">
                                                <td class="assignment-table__check" data-label="Chọn">
                                                    <input type="checkbox" name="customerIds" value="${customer.id}" form="assign-form" data-assign-checkbox
                                                           aria-label="Chọn đại lý ${fn:escapeXml(customer.code)}"${selectedIds.contains(customer.id) ? ' checked' : ''}>
                                                </td>
                                            </c:if>
                                            <td class="customer-table__code" data-label="Mã đại lý"><c:out value="${customer.code}"/></td>
                                            <td class="customer-table__name" data-label="Tên đại lý"><c:out value="${customer.name}"/></td>
                                            <td data-label="Khu vực"><c:out value="${customer.regionName}"/></td>
                                            <td data-label="Nhóm khách hàng"><c:out value="${customer.customerGroupName}"/></td>
                                            <td data-label="Nhân viên phụ trách">
                                                <c:choose>
                                                    <c:when test="${empty customer.salesRepName}"><span class="assignment-unassigned">Chưa phân công</span></c:when>
                                                    <c:otherwise>
                                                        <span class="assignment-rep">
                                                            <span class="assignment-rep__avatar" aria-hidden="true"><c:out value="${fn:substring(customer.salesRepName, 0, 1)}"/></span>
                                                            <c:out value="${customer.salesRepName}"/>
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td data-label="Ngày phân công"><c:out value="${empty customer.assignedAtText ? '—' : customer.assignedAtText}"/></td>
                                            <td data-label="Thao tác">
                                                <a class="row-actions__link" href="${listHref}&amp;page=${customerPage.page}&amp;history=${customer.id}#assignment-history">Lịch sử</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty customerPage.items}">
                                        <tr class="customer-table__empty-row">
                                            <td class="customer-table__empty" colspan="${canAssign ? 8 : 7}">
                                                <c:choose>
                                                    <c:when test="${filter.unassigned}">Mọi đại lý đều đã có nhân viên phụ trách.</c:when>
                                                    <c:when test="${not filter.filtered}">Chưa có đại lý nào trong phạm vi bạn được xem.</c:when>
                                                    <c:otherwise>Không tìm thấy đại lý phù hợp. Thử bỏ bớt điều kiện lọc.</c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>

                        <div class="account-pagination">
                            <p class="account-pagination__info">
                                <c:choose>
                                    <c:when test="${customerPage.totalItems == 0}">Không có đại lý nào</c:when>
                                    <c:otherwise>Hiển thị ${customerPage.firstRowNumber} - ${customerPage.firstRowNumber + fn:length(customerPage.items) - 1} trong tổng ${customerPage.totalItems} đại lý</c:otherwise>
                                </c:choose>
                            </p>
                            <nav aria-label="Phân trang">
                                <ul class="pagination">
                                    <li>
                                        <c:choose>
                                            <c:when test="${customerPage.page > 1}"><a class="pagination__item" aria-label="Trang trước" href="${listHref}&amp;page=${customerPage.page - 1}">‹</a></c:when>
                                            <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                        </c:choose>
                                    </li>
                                    <c:forEach var="pageNumber" begin="${customerPage.startPage}" end="${customerPage.endPage}">
                                        <li>
                                            <c:choose>
                                                <c:when test="${pageNumber == customerPage.page}"><span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span></c:when>
                                                <c:otherwise><a class="pagination__item" aria-label="Trang ${pageNumber}" href="${listHref}&amp;page=${pageNumber}">${pageNumber}</a></c:otherwise>
                                            </c:choose>
                                        </li>
                                    </c:forEach>
                                    <li>
                                        <c:choose>
                                            <c:when test="${customerPage.page < customerPage.totalPages}"><a class="pagination__item" aria-label="Trang sau" href="${listHref}&amp;page=${customerPage.page + 1}">›</a></c:when>
                                            <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                        </c:choose>
                                    </li>
                                </ul>
                            </nav>
                        </div>
                    </section>
                </div>

                <c:if test="${not empty historyCustomer}">
                    <aside class="assignment-history" id="assignment-history" aria-labelledby="assignment-history-title">
                        <div class="assignment-history__head">
                            <h2 class="assignment-history__title" id="assignment-history-title">Lịch sử chuyển giao</h2>
                            <a class="assignment-history__close" href="${listHref}&amp;page=${customerPage.page}" aria-label="Đóng lịch sử chuyển giao">×</a>
                        </div>
                        <div class="assignment-history__customer">
                            <p class="assignment-history__name"><c:out value="${historyCustomer.name}"/></p>
                            <p class="assignment-history__meta">Mã đại lý: <c:out value="${historyCustomer.code}"/></p>
                            <c:set var="currentRepName" value=""/>
                            <c:forEach var="rep" items="${salesReps}">
                                <c:if test="${rep.id == historyCustomer.salesRepId}"><c:set var="currentRepName" value="${rep.name}"/></c:if>
                            </c:forEach>
                            <p class="assignment-history__meta">Nhân viên hiện tại:
                                <strong><c:out value="${empty historyCustomer.salesRepId ? 'Chưa phân công' : (empty currentRepName ? '—' : currentRepName)}"/></strong></p>
                        </div>
                        <c:choose>
                            <c:when test="${empty history}">
                                <p class="sales-empty">Chưa có lần phân công nào được ghi lại cho đại lý này.</p>
                            </c:when>
                            <c:otherwise>
                                <ol class="assignment-timeline">
                                    <c:forEach var="entry" items="${history}">
                                        <li class="assignment-timeline__item">
                                            <p class="assignment-timeline__to"><c:out value="${entry.toName}"/></p>
                                            <p class="assignment-timeline__from">
                                                <c:choose>
                                                    <c:when test="${empty entry.fromName}">Phân công lần đầu</c:when>
                                                    <c:otherwise>Nhận từ <c:out value="${entry.fromName}"/></c:otherwise>
                                                </c:choose>
                                            </p>
                                            <c:if test="${not empty entry.reason}"><p class="assignment-timeline__reason"><c:out value="${entry.reason}"/></p></c:if>
                                            <p class="assignment-timeline__time"><c:out value="${entry.createdAtText}"/><c:if test="${not empty entry.actorName}"> · bởi <c:out value="${entry.actorName}"/></c:if></p>
                                        </li>
                                    </c:forEach>
                                </ol>
                            </c:otherwise>
                        </c:choose>
                        <p class="assignment-history__note">Lịch sử chuyển giao chỉ để xem, không sửa hoặc xoá được.</p>
                    </aside>
                </c:if>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/customers.js'/>"></script>
</body>
</html>
