<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="reservations"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<%-- Liên kết về Sổ tồn kho (S5-05) chỉ hiện khi ứng dụng đã có màn đó --%>
<c:set var="hasStockLedger" value="${not empty pageContext.servletContext.servletRegistrations['com.oms.controller.StockLedgerServlet']}"/>
<c:if test="${hasStockLedger}">
    <c:set var="breadcrumbParent" value="Sổ tồn kho"/>
    <c:set var="breadcrumbParentUrl" value="/stock"/>
</c:if>
<c:set var="breadcrumbPage" value="Chi tiết giữ chỗ tồn"/>
<c:url var="listUrl" value="/stock/reservations">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="date" value="${filter.approvedDate}"/>
    <c:param name="status" value="${filter.status}"/>
    <c:param name="product" value="${filter.productId}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết giữ chỗ tồn | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/reservations.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Chi tiết giữ chỗ tồn</h1>
                    <p class="inv-header__subtitle">Danh sách các đơn đã được duyệt và đang giữ chỗ tồn kho. Chọn một đơn để xem trạng thái giữ chỗ theo từng dòng hàng.</p>
                </div>
                <c:if test="${hasStockLedger}">
                    <div class="inv-header__actions">
                        <a class="inv-button" id="reservation-back" href="<c:url value='/stock'/>">
                            <span class="inv-icon inv-icon--chevron-left" aria-hidden="true"></span>Quay lại sổ tồn
                        </a>
                    </div>
                </c:if>
            </header>

            <section class="inv-stats" aria-label="Tổng quan giữ chỗ">
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Đơn đang giữ chỗ</p>
                    <span class="inv-stat__icon tone-blue"><span class="inv-icon inv-icon--file-text" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-orders">${summary.orders}</p>
                    <p class="inv-stat__note">Các đơn đã được duyệt</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Tổng số lượng giữ chỗ</p>
                    <span class="inv-stat__icon tone-green"><span class="inv-icon inv-icon--package" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-reserved"><fmt:formatNumber value="${summary.reservedQty}" maxFractionDigits="3"/></p>
                    <p class="inv-stat__note">Theo đơn vị cơ sở</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Trạng thái đơn</p>
                    <span class="inv-stat__icon tone-amber"><span class="inv-icon inv-icon--circle-check" aria-hidden="true"></span></span>
                    <p class="inv-stat__value" id="stat-status"><c:out value="${filter.statusLabel}"/></p>
                    <p class="inv-stat__note">Giữ chỗ tự nhả sau ${holdHours} giờ nếu chưa xuất kho</p>
                </article>
            </section>

            <c:if test="${filter.productFiltered}">
                <c:url var="clearProductUrl" value="/stock/reservations">
                    <c:param name="keyword" value="${filter.keyword}"/>
                    <c:param name="date" value="${filter.approvedDate}"/>
                    <c:param name="status" value="${filter.status}"/>
                </c:url>
                <div class="inv-note reservation-product-filter" id="reservation-product-filter">
                    <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                    <p>Đang xem các đơn giữ chỗ <strong><c:out value="${empty productLabel ? 'SKU đã chọn' : productLabel}"/></strong>.
                        <a class="reservation-product-filter__clear" href="${fn:escapeXml(clearProductUrl)}">Xem mọi SKU</a></p>
                </div>
            </c:if>

            <section class="inv-card" aria-labelledby="reservation-search-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="reservation-search-title">Tìm kiếm đơn đang giữ chỗ</h2>
                        <p class="inv-card__subtitle">Tìm theo mã đơn hoặc tên đại lý.</p>
                    </div>
                </div>
                <form class="reservation-filters" id="reservation-filter-form" action="<c:url value='/stock/reservations'/>" method="get" role="search">
                    <c:if test="${filter.productFiltered}">
                        <input type="hidden" name="product" value="${filter.productId}">
                        <input type="hidden" name="warehouse" value="${filter.warehouseId}">
                    </c:if>
                    <div class="inv-field">
                        <label class="reservation-filters__label" for="reservation-keyword">Mã đơn hoặc đại lý</label>
                        <span class="inv-search">
                            <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                            <input class="inv-control" type="search" id="reservation-keyword" name="keyword" maxlength="100"
                                   value="<c:out value='${filter.keyword}'/>" placeholder="Nhập mã đơn hoặc tên đại lý" autocomplete="off">
                        </span>
                    </div>
                    <div class="inv-field">
                        <label class="reservation-filters__label" for="reservation-date">Ngày duyệt</label>
                        <input class="inv-control" type="date" id="reservation-date" name="date" value="${filter.approvedDate}">
                    </div>
                    <div class="inv-field">
                        <label class="reservation-filters__label" for="reservation-status">Trạng thái</label>
                        <select class="inv-control" id="reservation-status" name="status">
                            <c:forEach var="status" items="${statusLabels}">
                                <option value="${status.key}"${status.key == filter.status ? ' selected' : ''}>${status.value}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <button class="inv-button inv-button--primary reservation-filters__submit" type="submit" id="reservation-search">
                        <span class="inv-icon inv-icon--search" aria-hidden="true"></span>Tìm kiếm
                    </button>
                </form>
            </section>

            <section class="inv-card" aria-labelledby="reservation-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="reservation-table-title">Danh sách đơn đang giữ chỗ</h2>
                        <p class="inv-card__subtitle">Chọn “Xem chi tiết” để kiểm tra từng dòng hàng của đơn.</p>
                    </div>
                    <p class="inv-card__meta" id="reservation-count">Tổng cộng <strong>${orderPage.totalItems} đơn</strong></p>
                </div>
                <div class="inv-table-scroll reservation-table-wrap">
                    <table class="inv-table reservation-table" id="reservation-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã đơn</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col" class="is-number">Số lượng giữ chỗ</th>
                                <th scope="col">Ngày duyệt</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Ghi chú</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="order" items="${orderPage.items}">
                                <c:url var="detailUrl" value="/stock/reservations/view"><c:param name="order" value="${order.orderId}"/></c:url>
                                <tr>
                                    <td class="reservation-table__order"><c:out value="${order.orderNo}"/></td>
                                    <td>
                                        <span class="inv-table__main"><c:out value="${order.customerName}"/></span>
                                        <span class="inv-table__sub"><c:out value="${order.customerCode}"/></span>
                                    </td>
                                    <td class="is-number">
                                        <span class="reservation-table__qty"><fmt:formatNumber value="${order.reservedQty}" maxFractionDigits="3"/></span>
                                        <span class="inv-table__sub">đơn vị cơ sở</span>
                                    </td>
                                    <td>${order.approvedAtText}</td>
                                    <td><span class="inv-pill inv-pill--dot ${order.orderLive ? 'inv-pill--success' : 'inv-pill--muted'}">${order.orderStatusLabel}</span></td>
                                    <td class="reservation-table__note reservation-table__note--${order.state.tone}">${order.state.note}</td>
                                    <td>
                                        <a class="inv-button inv-button--primary inv-button--small" href="${fn:escapeXml(detailUrl)}"
                                           aria-label="Xem chi tiết giữ chỗ đơn <c:out value='${order.orderNo}'/>">Xem chi tiết</a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty orderPage.items}">
                                <tr>
                                    <td class="inv-table__empty" colspan="7">
                                        <c:choose>
                                            <c:when test="${filter.filtered}">Không có đơn giữ chỗ nào khớp điều kiện tìm.</c:when>
                                            <c:otherwise>Chưa có đơn đã duyệt nào đang giữ chỗ tồn.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="inv-footer">
                    <p class="inv-footer__info" id="reservation-page-info">
                        <c:choose>
                            <c:when test="${orderPage.totalItems == 0}">Không có đơn nào</c:when>
                            <c:otherwise>Hiển thị <strong>${orderPage.firstRowNumber}–${orderPage.firstRowNumber + fn:length(orderPage.items) - 1}</strong>
                                trong tổng số <strong>${orderPage.totalItems} đơn</strong></c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="inv-pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${orderPage.page > 1}">
                                        <a class="inv-pagination__item" aria-label="Trang trước" href="${pageUrl}${orderPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${orderPage.startPage}" end="${orderPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == orderPage.page}">
                                            <span class="inv-pagination__item inv-pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="inv-pagination__item" aria-label="Trang ${pageNumber}" href="${pageUrl}${pageNumber}">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${orderPage.page < orderPage.totalPages}">
                                        <a class="inv-pagination__item" aria-label="Trang sau" href="${pageUrl}${orderPage.page + 1}">›</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
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
