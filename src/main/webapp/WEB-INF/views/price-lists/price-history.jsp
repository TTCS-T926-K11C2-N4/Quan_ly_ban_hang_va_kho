<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="price-history"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Lịch sử thay đổi giá"/>
<%-- Link giữ điều kiện lọc hiện tại cho phân trang --%>
<c:url var="listUrl" value="/price-history">
    <c:param name="keyword" value="${keyword}"/>
    <c:param name="categoryId" value="${categoryFilter}"/>
    <c:param name="groupId" value="${groupFilter}"/>
    <c:param name="status" value="${statusFilter}"/>
    <c:param name="from" value="${fromDate}"/>
    <c:param name="to" value="${toDate}"/>
    <c:param name="priceList" value="${priceList.id}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<c:set var="filtered" value="${not empty keyword or not empty categoryFilter or not empty groupFilter or not empty statusFilter
        or not empty fromDate or not empty toDate or not empty priceList}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử thay đổi giá | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/price-lists.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Lịch sử thay đổi giá</h1>
                <p class="page-header__subtitle">Giá bán, giá sàn trước và sau mỗi lần điều chỉnh bảng giá, người sửa và ngày áp dụng.
                    Lịch sử chỉ để xem, không sửa hoặc xoá được.</p>
            </header>

            <c:if test="${not empty priceList}">
                <div class="account-flash price-history-scope" id="price-history-scope" role="status">
                    <p>Đang xem lịch sử của bảng giá <strong><c:out value="${priceList.code}"/></strong>
                        (<c:out value="${priceList.customerGroupName}"/>, phiên bản ${priceList.versionNo}).
                        <a href="<c:url value='/price-history'/>">Xem tất cả bảng giá</a></p>
                </div>
            </c:if>

            <form class="account-filters price-history-filters" id="price-history-filter-form" action="<c:url value='/price-history'/>" method="get" role="search">
                <c:if test="${not empty priceList}"><input type="hidden" name="priceList" value="${priceList.id}"></c:if>
                <div class="account-filters__field price-history-filters__keyword">
                    <label class="account-filters__label" for="history-keyword">Tìm kiếm</label>
                    <input class="account-filters__control" type="search" id="history-keyword" name="keyword" maxlength="100"
                           value="<c:out value='${keyword}'/>" placeholder="Mã SKU hoặc tên sản phẩm">
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="history-category">Nhóm hàng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="history-category" name="categoryId">
                            <option value="">Tất cả</option>
                            <c:forEach var="category" items="${categories}">
                                <option value="${category.id}"${category.id == categoryFilter ? ' selected' : ''}><c:forEach begin="2" end="${category.level}">&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${category.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="history-group">Nhóm khách hàng</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="history-group" name="groupId">
                            <option value="">Tất cả</option>
                            <c:forEach var="group" items="${groups}">
                                <option value="${group.id}"${group.id == groupFilter ? ' selected' : ''}><c:out value="${group.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="history-status">Trạng thái bảng giá</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="history-status" name="status">
                            <option value="">Tất cả</option>
                            <c:forEach var="status" items="${statuses}">
                                <option value="${status.code}"${status.code == statusFilter ? ' selected' : ''}>${status.label}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="history-from">Áp dụng từ ngày</label>
                    <input class="account-filters__control" type="date" id="history-from" name="from" value="${fromDate}">
                </div>
                <div class="account-filters__field">
                    <label class="account-filters__label" for="history-to">Đến ngày</label>
                    <input class="account-filters__control" type="date" id="history-to" name="to" value="${toDate}">
                </div>
                <div class="price-history-filters__actions">
                    <button class="price-filters__search" type="submit">Lọc</button>
                    <c:if test="${filtered}">
                        <a class="price-history-filters__clear" href="<c:url value='/price-history'/>">Xoá bộ lọc</a>
                    </c:if>
                </div>
            </form>

            <section class="account-table-card price-history-card" aria-label="Lịch sử thay đổi giá">
                <div class="account-table-scroll">
                    <table class="account-table price-history-table" id="price-history-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Áp dụng từ</th>
                                <th scope="col">Sản phẩm</th>
                                <th scope="col">Đơn vị</th>
                                <th scope="col">Bảng giá</th>
                                <th scope="col" class="price-number">Giá bán cũ → mới</th>
                                <th scope="col" class="price-number">Giá sàn cũ → mới</th>
                                <th scope="col">Người sửa</th>
                                <th scope="col">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${historyPage.items}" varStatus="loop">
                                <tr>
                                    <td class="account-table__index">${historyPage.firstRowNumber + loop.index}</td>
                                    <td class="price-date">${row.effectiveFromText}</td>
                                    <td>
                                        <span class="price-history__product"><span class="price-code"><c:out value="${row.sku}"/></span> · <c:out value="${row.productName}"/></span>
                                        <span class="price-history__sub"><c:out value="${row.categoryName}"/></span>
                                    </td>
                                    <td><c:out value="${row.unitName}"/></td>
                                    <td>
                                        <span class="price-code"><c:out value="${row.priceListCode}"/></span><span class="price-history__version"> · v${row.versionNo}</span>
                                        <span class="price-history__sub"><c:out value="${row.customerGroupName}"/></span>
                                    </td>
                                    <td class="price-number">
                                        <span class="price-history__change">
                                            <c:choose>
                                                <c:when test="${empty row.oldPrice}"><span class="price-history__old">Mới</span></c:when>
                                                <c:otherwise><span class="price-history__old"><fmt:formatNumber value="${row.oldPrice}" maxFractionDigits="0"/></span></c:otherwise>
                                            </c:choose>
                                            <span aria-hidden="true">→</span>
                                            <strong><fmt:formatNumber value="${row.newPrice}" maxFractionDigits="0"/></strong>
                                        </span>
                                        <c:if test="${not empty row.changePercent and row.changePercent != 0}">
                                            <span class="price-history__delta price-history__delta--${row.changePercent > 0 ? 'up' : 'down'}">
                                                <fmt:formatNumber value="${row.changePercent}" pattern="+#,##0.0;-#,##0.0"/>%
                                            </span>
                                        </c:if>
                                    </td>
                                    <td class="price-number">
                                        <span class="price-history__change">
                                            <span class="price-history__old"><c:choose><c:when test="${empty row.oldFloorPrice}">—</c:when><c:otherwise><fmt:formatNumber value="${row.oldFloorPrice}" maxFractionDigits="0"/></c:otherwise></c:choose></span>
                                            <span aria-hidden="true">→</span>
                                            <fmt:formatNumber value="${row.newFloorPrice}" maxFractionDigits="0"/>
                                        </span>
                                    </td>
                                    <td>
                                        <c:out value="${empty row.actorName ? '—' : row.actorName}"/>
                                        <span class="price-history__sub">${row.changedAtText}</span>
                                    </td>
                                    <td><span class="status-badge price-badge--${fn:toLowerCase(row.status.code)}">${row.status.label}</span></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty historyPage.items}">
                                <tr>
                                    <td class="price-history__empty" colspan="9">
                                        <c:choose>
                                            <c:when test="${filtered}">Không có lần thay đổi giá nào phù hợp. Thử bỏ bớt điều kiện lọc.</c:when>
                                            <c:otherwise>Chưa có lần thay đổi giá nào. Lịch sử được ghi khi sửa giá trong bảng giá hoặc tạo phiên bản bảng giá mới.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info" id="price-history-count">
                        <c:choose>
                            <c:when test="${historyPage.totalItems == 0}">Không có bản ghi nào</c:when>
                            <c:otherwise>Hiển thị ${historyPage.firstRowNumber} - ${historyPage.firstRowNumber + fn:length(historyPage.items) - 1} trên ${historyPage.totalItems} bản ghi</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${historyPage.page > 1}"><a class="pagination__item" aria-label="Trang trước" href="${pageUrl}${historyPage.page - 1}">‹</a></c:when>
                                    <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${historyPage.startPage}" end="${historyPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == historyPage.page}"><span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span></c:when>
                                        <c:otherwise><a class="pagination__item" aria-label="Trang ${pageNumber}" href="${pageUrl}${pageNumber}">${pageNumber}</a></c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${historyPage.page < historyPage.totalPages}"><a class="pagination__item" aria-label="Trang sau" href="${pageUrl}${historyPage.page + 1}">›</a></c:when>
                                    <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
