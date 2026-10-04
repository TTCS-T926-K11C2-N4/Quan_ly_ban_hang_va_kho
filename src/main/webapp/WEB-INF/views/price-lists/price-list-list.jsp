<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="price-lists"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Bảng giá"/>
<%-- Người chỉ có quyền xem không thấy nút tạo, sửa, xoá, tạo phiên bản; cột giá vốn chỉ hiện khi có COST_PRICE_VIEW --%>
<c:set var="canManage" value="${currentUser.can('PRODUCT_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý bảng giá | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/price-lists.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Quản lý bảng giá</h1>
                <p class="page-header__subtitle">Danh sách bảng giá theo nhóm khách hàng. Mỗi nhóm mỗi ngày chỉ có một bảng giá đang hiệu lực.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="price-list-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="price-list-flash-error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <form class="account-filters price-filters" id="price-filter-form" action="<c:url value='/price-lists'/>" method="get" role="search">
                <div class="account-filters__field">
                    <label class="account-filters__label" for="price-group-filter">Nhóm khách hàng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="price-group-filter" name="groupId">
                            <option value="">Tất cả nhóm</option>
                            <c:forEach var="group" items="${groups}">
                                <option value="${group.id}"${group.id == groupFilter ? ' selected' : ''}><c:out value="${group.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="price-from">Từ ngày</label>
                    <input class="account-filters__control" type="date" id="price-from" name="from" value="${fromDate}">
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="price-to">Đến ngày</label>
                    <input class="account-filters__control" type="date" id="price-to" name="to" value="${toDate}">
                </div>
                <div class="price-filters__actions">
                    <button class="price-filters__search" type="submit">Tìm kiếm</button>
                    <c:if test="${canManage}">
                        <a class="account-filters__create price-filters__create" id="create-price-list-link" href="<c:url value='/price-lists/new'/>">
                            <span aria-hidden="true">+</span> Tạo bảng giá mới
                        </a>
                    </c:if>
                </div>
            </form>

            <section class="account-table-card" aria-label="Danh sách bảng giá">
                <div class="account-table-scroll">
                    <table class="account-table price-table" id="price-list-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Mã bảng giá</th>
                                <th scope="col">Nhóm khách hàng</th>
                                <th scope="col">Tên bảng giá</th>
                                <th scope="col">Ngày bắt đầu</th>
                                <th scope="col">Ngày kết thúc</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="list" items="${listPage.items}" varStatus="loop">
                                <tr class="${list.id == selected.id ? 'price-table__row--selected' : ''}">
                                    <td class="account-table__index">${listPage.firstRowNumber + loop.index}</td>
                                    <td class="price-code">
                                        <c:out value="${list.code}"/>
                                        <c:if test="${list.locked}"><span class="price-lock" title="Đã có đơn sử dụng" aria-label="Đã có đơn sử dụng">🔒</span></c:if>
                                    </td>
                                    <td><c:out value="${list.customerGroupName}"/></td>
                                    <td class="account-table__name"><c:out value="${list.name}"/></td>
                                    <td class="price-date">${list.validFromText}</td>
                                    <td class="price-date">${list.validToText}</td>
                                    <td><span class="status-badge price-badge--${fn:toLowerCase(list.status.code)}">${list.status.label}</span></td>
                                    <td>
                                        <div class="row-actions">
                                            <a class="price-view-link" href="<c:url value='/price-lists'><c:param name='groupId' value='${groupFilter}'/><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='page' value='${listPage.page}'/><c:param name='id' value='${list.id}'/></c:url>#price-detail">Xem</a>
                                            <c:if test="${canManage}">
                                                <details class="row-menu">
                                                    <summary class="row-menu__toggle" aria-label="Thao tác khác cho bảng giá ${fn:escapeXml(list.code)}">⌄</summary>
                                                    <div class="row-menu__list">
                                                        <c:choose>
                                                            <c:when test="${list.locked}">
                                                                <a class="row-menu__item" href="<c:url value='/price-lists/version'><c:param name='id' value='${list.id}'/></c:url>">Tạo phiên bản mới</a>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <a class="row-menu__item" href="<c:url value='/price-lists/edit'><c:param name='id' value='${list.id}'/></c:url>">Sửa</a>
                                                                <a class="row-menu__item" href="<c:url value='/price-lists/version'><c:param name='id' value='${list.id}'/></c:url>">Tạo phiên bản mới</a>
                                                                <form action="<c:url value='/price-lists/delete'/>" method="post"
                                                                      data-confirm="Xóa bảng giá &quot;${fn:escapeXml(list.code)}&quot;?">
                                                                    <input type="hidden" name="id" value="${list.id}">
                                                                    <button class="row-menu__item row-menu__item--button row-menu__item--danger" type="submit">Xóa</button>
                                                                </form>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </details>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty listPage.items}">
                                <tr>
                                    <td class="category-table__empty" colspan="8">
                                        ${empty groupFilter and empty fromDate and empty toDate ? 'Chưa có bảng giá nào.' : 'Không tìm thấy bảng giá phù hợp.'}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">
                        <c:choose>
                            <c:when test="${listPage.totalItems == 0}">Hiển thị 0 bảng giá</c:when>
                            <c:otherwise>Hiển thị ${listPage.firstRowNumber} - ${listPage.firstRowNumber + fn:length(listPage.items) - 1} trên ${listPage.totalItems} bảng giá</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${listPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/price-lists'><c:param name='groupId' value='${groupFilter}'/><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='page' value='${listPage.page - 1}'/></c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${listPage.startPage}" end="${listPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == listPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/price-lists'><c:param name='groupId' value='${groupFilter}'/><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='page' value='${pageNumber}'/></c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${listPage.page < listPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/price-lists'><c:param name='groupId' value='${groupFilter}'/><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='page' value='${listPage.page + 1}'/></c:url>">›</a>
                                    </c:when>
                                    <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>

            <c:if test="${not empty selected}">
                <div class="price-detail-layout" id="price-detail">
                    <section class="account-table-card price-detail" aria-labelledby="price-detail-title">
                        <div class="price-detail__header">
                            <div>
                                <h2 class="price-detail__title" id="price-detail-title">Chi tiết bảng giá</h2>
                                <p class="price-detail__desc">
                                    <c:choose>
                                        <c:when test="${selected.locked}">Bảng giá này đã được sử dụng trong đơn hàng nên chỉ xem, không thể chỉnh sửa.</c:when>
                                        <c:otherwise>Bảng giá chưa có đơn hàng sử dụng nên còn sửa được.</c:otherwise>
                                    </c:choose>
                                </p>
                            </div>
                            <span class="status-badge ${selected.locked ? 'price-badge--locked' : 'price-badge--editable'}">${selected.locked ? 'Đã có đơn sử dụng' : 'Chưa có đơn sử dụng'}</span>
                        </div>

                        <dl class="price-detail__meta">
                            <div><dt>Mã bảng giá</dt><dd><c:out value="${selected.code}"/><c:if test="${selected.versionNo > 1}"> (phiên bản ${selected.versionNo})</c:if></dd></div>
                            <div><dt>Nhóm khách hàng</dt><dd><c:out value="${selected.customerGroupName}"/></dd></div>
                            <div><dt>Ngày bắt đầu</dt><dd>${selected.validFromText}</dd></div>
                            <div><dt>Tên bảng giá</dt><dd><c:out value="${selected.name}"/></dd></div>
                            <div><dt>Trạng thái</dt><dd>${selected.status.label}</dd></div>
                            <div><dt>Ngày kết thúc</dt><dd>${selected.validToText}</dd></div>
                        </dl>

                        <h3 class="price-detail__subtitle">Danh sách giá theo SKU (${fn:length(selectedItems)})</h3>
                        <div class="account-table-scroll">
                            <table class="account-table price-items-table">
                                <thead>
                                    <tr>
                                        <th scope="col">STT</th>
                                        <th scope="col">SKU</th>
                                        <th scope="col">Tên sản phẩm</th>
                                        <th scope="col">Đơn vị</th>
                                        <c:if test="${canViewCost}"><th scope="col" class="price-number">Giá vốn (VNĐ)</th></c:if>
                                        <th scope="col" class="price-number">Giá bán (VNĐ)</th>
                                        <th scope="col" class="price-number">Giá sàn (VNĐ)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="item" items="${selectedItems}" varStatus="loop">
                                        <tr>
                                            <td class="account-table__index">${loop.index + 1}</td>
                                            <td class="price-code"><c:out value="${item.sku}"/></td>
                                            <td><c:out value="${item.productName}"/></td>
                                            <td><c:out value="${item.unitName}"/></td>
                                            <c:if test="${canViewCost}"><td class="price-number price-number--muted"><fmt:formatNumber value="${item.costPrice}" maxFractionDigits="0"/></td></c:if>
                                            <td class="price-number price-number--strong"><fmt:formatNumber value="${item.price}" maxFractionDigits="0"/></td>
                                            <td class="price-number"><fmt:formatNumber value="${item.floorPrice}" maxFractionDigits="0"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </section>

                    <c:if test="${canManage}">
                        <aside class="price-side-card" aria-labelledby="price-side-title">
                            <c:choose>
                                <c:when test="${selected.locked}">
                                    <h2 class="price-side-card__title" id="price-side-title"><span class="price-side-card__icon" aria-hidden="true">i</span> Muốn thay đổi giá?</h2>
                                    <p>Bảng giá này đã có đơn hàng sử dụng. Không thể chỉnh sửa trực tiếp.</p>
                                    <p>Vui lòng tạo phiên bản mới để thay đổi giá. Bản này sẽ kết thúc hiệu lực trước ngày phiên bản mới bắt đầu.</p>
                                </c:when>
                                <c:otherwise>
                                    <h2 class="price-side-card__title" id="price-side-title"><span class="price-side-card__icon" aria-hidden="true">i</span> Thay đổi giá</h2>
                                    <p>Bảng giá chưa có đơn sử dụng nên sửa trực tiếp được.</p>
                                    <p>Muốn giữ nguyên giá hiện tại tới một ngày rồi mới đổi, hãy tạo phiên bản mới.</p>
                                </c:otherwise>
                            </c:choose>
                            <div class="price-side-card__actions">
                                <a class="button button--secondary" href="<c:url value='/price-lists'><c:param name='groupId' value='${groupFilter}'/><c:param name='from' value='${fromDate}'/><c:param name='to' value='${toDate}'/><c:param name='page' value='${listPage.page}'/></c:url>">Đóng</a>
                                <c:choose>
                                    <c:when test="${selectedHasNext}">
                                        <span class="price-side-card__note">Đã có phiên bản sau.</span>
                                    </c:when>
                                    <c:when test="${selected.locked}">
                                        <a class="button button--primary" id="create-version-link" href="<c:url value='/price-lists/version'><c:param name='id' value='${selected.id}'/></c:url>">+ Tạo phiên bản</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="button button--primary" id="edit-price-list-link" href="<c:url value='/price-lists/edit'><c:param name='id' value='${selected.id}'/></c:url>">Sửa bảng giá</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </aside>
                    </c:if>
                </div>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/categories.js'/>"></script>
</body>
</html>
