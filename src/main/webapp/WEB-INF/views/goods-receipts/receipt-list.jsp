<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="goods-receipts"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Phiếu nhập kho"/>
<c:url var="listUrl" value="/goods-receipts">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="status" value="${filter.status}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="date" value="${filter.receiptDate}"/>
</c:url>
<c:url var="exportUrl" value="/goods-receipts/export">
    <c:param name="keyword" value="${filter.keyword}"/>
    <c:param name="status" value="${filter.status}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="date" value="${filter.receiptDate}"/>
</c:url>
<c:set var="pageUrl" value="${fn:escapeXml(listUrl)}&amp;page="/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu nhập kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/goods-receipts.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Phiếu nhập kho</h1>
                    <p class="inv-header__subtitle">Theo dõi phiếu nhập hàng và trạng thái ghi sổ trong hệ thống kho.</p>
                </div>
                <c:if test="${canManage}">
                    <div class="inv-header__actions">
                        <a class="inv-button inv-button--primary" id="receipt-create" href="<c:url value='/goods-receipts/new'/>">
                            <span class="inv-icon inv-icon--plus" aria-hidden="true"></span>Tạo phiếu nhập
                        </a>
                    </div>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="receipt-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="receipt-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-stats" aria-label="Tổng quan phiếu nhập">
                <article class="inv-stat inv-stat--stacked receipt-stat">
                    <p class="inv-stat__label">Tổng số phiếu nhập</p>
                    <p class="inv-stat__value" id="stat-total">${summary.total}</p>
                    <p class="inv-stat__note">Tất cả phiếu nhập trong hệ thống</p>
                </article>
                <article class="inv-stat inv-stat--stacked receipt-stat">
                    <p class="inv-stat__label">Phiếu nháp</p>
                    <p class="inv-stat__value" id="stat-draft">${summary.draft}</p>
                    <p class="inv-stat__note">Đang chờ ghi sổ</p>
                </article>
                <article class="inv-stat inv-stat--stacked receipt-stat">
                    <p class="inv-stat__label">Đã ghi sổ</p>
                    <p class="inv-stat__value" id="stat-confirmed">${summary.confirmed}</p>
                    <p class="inv-stat__note">Đã xác nhận nhập kho</p>
                </article>
            </section>

            <form class="inv-card receipt-filters" id="receipt-filter-form" action="<c:url value='/goods-receipts'/>" method="get" role="search">
                <div class="inv-field">
                    <label class="receipt-filters__label" for="receipt-keyword">Tìm kiếm phiếu nhập</label>
                    <input class="inv-control" type="search" id="receipt-keyword" name="keyword" maxlength="100"
                           value="<c:out value='${filter.keyword}'/>" placeholder="Nhập mã phiếu, nhà cung cấp..." autocomplete="off">
                </div>
                <div class="inv-field">
                    <label class="receipt-filters__label" for="receipt-status">Trạng thái</label>
                    <select class="inv-control" id="receipt-status" name="status" data-auto-submit>
                        <option value="">Tất cả trạng thái</option>
                        <c:forEach var="status" items="${statusLabels}">
                            <option value="${status.key}"${status.key == filter.status ? ' selected' : ''}>${status.value}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="inv-field">
                    <label class="receipt-filters__label" for="receipt-warehouse">Kho nhận</label>
                    <select class="inv-control" id="receipt-warehouse" name="warehouse" data-auto-submit>
                        <option value="">Tất cả kho</option>
                        <c:forEach var="warehouse" items="${warehouses}">
                            <option value="${warehouse.id}"${warehouse.id == filter.warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="inv-field">
                    <label class="receipt-filters__label" for="receipt-date">Ngày nhập</label>
                    <input class="inv-control" type="date" id="receipt-date" name="date" value="${filter.receiptDate}" data-auto-submit>
                </div>
                <div class="receipt-filters__actions">
                    <button class="visually-hidden" type="submit">Tìm kiếm</button>
                    <c:if test="${filter.filtered}">
                        <a class="receipt-filters__clear" id="receipt-filter-clear" href="<c:url value='/goods-receipts'/>">Xoá bộ lọc</a>
                    </c:if>
                </div>
            </form>

            <section class="inv-card" aria-labelledby="receipt-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="receipt-table-title">Danh sách phiếu nhập</h2>
                        <p class="inv-card__subtitle">Thông tin nhà cung cấp, kho nhận và giá trị phiếu nhập.</p>
                    </div>
                    <a class="inv-button" id="receipt-export" href="${fn:escapeXml(exportUrl)}">
                        <span class="inv-icon inv-icon--download" aria-hidden="true"></span>Xuất danh sách
                    </a>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table receipt-table" id="receipt-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã phiếu</th>
                                <th scope="col">Nhà cung cấp</th>
                                <th scope="col">Kho nhận</th>
                                <th scope="col">Ngày nhập</th>
                                <th scope="col" class="is-number">Tổng tiền</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="receipt" items="${receiptPage.items}">
                                <c:url var="viewUrl" value="/goods-receipts/view"><c:param name="id" value="${receipt.id}"/></c:url>
                                <c:url var="editUrl" value="/goods-receipts/edit"><c:param name="id" value="${receipt.id}"/></c:url>
                                <tr>
                                    <td>
                                        <a class="inv-table__code" href="${fn:escapeXml(viewUrl)}"><c:out value="${receipt.receiptNo}"/></a>
                                        <c:if test="${not empty receipt.adjustsReceiptNo}">
                                            <span class="inv-table__sub">Điều chỉnh <c:out value="${receipt.adjustsReceiptNo}"/></span>
                                        </c:if>
                                    </td>
                                    <td><span class="inv-table__main"><c:out value="${receipt.supplierName}"/></span></td>
                                    <td>
                                        <span class="receipt-table__warehouse"><c:out value="${receipt.warehouseCode}"/></span>
                                        <span class="inv-table__sub"><c:out value="${receipt.warehouseName}"/></span>
                                    </td>
                                    <td>${receipt.receiptDateText}</td>
                                    <td class="is-number receipt-table__money"><fmt:formatNumber value="${receipt.totalAmount}" maxFractionDigits="0"/> ₫</td>
                                    <td><span class="inv-pill ${receipt.draft ? 'inv-pill--draft' : 'inv-pill--posted'}">${receipt.statusLabel}</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${receipt.draft and canManage}">
                                                <a class="inv-icon-button" href="${fn:escapeXml(editUrl)}" title="Sửa phiếu nháp"
                                                   aria-label="Sửa phiếu nháp <c:out value='${receipt.receiptNo}'/>">
                                                    <span class="inv-icon inv-icon--pencil" aria-hidden="true"></span>
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <a class="inv-icon-button" href="${fn:escapeXml(viewUrl)}" title="Xem phiếu"
                                                   aria-label="Xem phiếu <c:out value='${receipt.receiptNo}'/>">
                                                    <span class="inv-icon inv-icon--search" aria-hidden="true"></span>
                                                </a>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty receiptPage.items}">
                                <tr>
                                    <td class="inv-table__empty" colspan="7">
                                        <c:choose>
                                            <c:when test="${filter.filtered}">Không tìm thấy phiếu nhập phù hợp. Thử bỏ bớt điều kiện lọc.</c:when>
                                            <c:otherwise>Chưa có phiếu nhập nào.</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="inv-footer">
                    <p class="inv-footer__info" id="receipt-page-info">
                        <c:choose>
                            <c:when test="${receiptPage.totalItems == 0}">Không có phiếu nhập nào</c:when>
                            <c:otherwise>Hiển thị ${receiptPage.firstRowNumber}–${receiptPage.firstRowNumber + fn:length(receiptPage.items) - 1}
                                trong tổng số ${receiptPage.totalItems} phiếu</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="inv-pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${receiptPage.page > 1}">
                                        <a class="inv-pagination__item" aria-label="Trang trước" href="${pageUrl}${receiptPage.page - 1}">‹</a>
                                    </c:when>
                                    <c:otherwise><span class="inv-pagination__item inv-pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${receiptPage.startPage}" end="${receiptPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == receiptPage.page}">
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
                                    <c:when test="${receiptPage.page < receiptPage.totalPages}">
                                        <a class="inv-pagination__item" aria-label="Trang sau" href="${pageUrl}${receiptPage.page + 1}">›</a>
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
    <script src="<c:url value='/assets/js/goods-receipts.js'/>"></script>
</body>
</html>
