<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="stock-card"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Thẻ kho"/>
<c:url var="cardUrl" value="/stock/card">
    <c:param name="product" value="${filter.productId}"/>
    <c:param name="warehouse" value="${filter.warehouseId}"/>
    <c:param name="from" value="${filter.dateFromText}"/>
    <c:param name="to" value="${filter.dateToText}"/>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thẻ kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/stock-card.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Thẻ kho</h1>
                    <p class="inv-header__subtitle">Mọi phát sinh nhập, xuất, chuyển, điều chỉnh của một sản phẩm theo thứ tự thời gian, để truy ra phiếu nào làm số tồn lệch.</p>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="card-filter-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="card-filter-title">Chọn sản phẩm và phạm vi</h2>
                </div>
                <form class="stock-card-filters" id="stock-card-form" action="<c:url value='/stock/card'/>" method="get">
                    <div class="inv-field stock-card-filters__product">
                        <label class="inv-field__label" for="card-product">Sản phẩm</label>
                        <select class="inv-control" id="card-product" name="product" required>
                            <option value="">Chọn sản phẩm</option>
                            <c:forEach var="product" items="${products}">
                                <option value="${product.id}"${product.id == filter.productId ? ' selected' : ''}><c:out value="${product.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="card-warehouse">Kho</label>
                        <select class="inv-control" id="card-warehouse" name="warehouse">
                            <option value="">Tất cả kho</option>
                            <c:forEach var="warehouse" items="${warehouses}">
                                <option value="${warehouse.id}"${warehouse.id == filter.warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="card-from">Từ ngày</label>
                        <input class="inv-control" type="date" id="card-from" name="from" value="${filter.dateFromText}">
                    </div>
                    <div class="inv-field">
                        <label class="inv-field__label" for="card-to">Đến ngày</label>
                        <input class="inv-control" type="date" id="card-to" name="to" value="${filter.dateToText}">
                    </div>
                    <button class="inv-button inv-button--primary" type="submit" id="card-submit">Xem thẻ kho</button>
                </form>
            </section>

            <c:choose>
                <c:when test="${empty filter.productId}">
                    <p class="inv-card stock-card-empty" id="stock-card-hint">Chọn một sản phẩm để xem thẻ kho.</p>
                </c:when>
                <c:otherwise>
                    <section class="inv-card" aria-labelledby="card-table-title">
                        <div class="inv-card__head">
                            <div>
                                <h2 class="inv-card__title" id="card-table-title">Phát sinh</h2>
                                <p class="inv-card__subtitle">Đơn vị: <strong><c:out value="${baseUnit}"/></strong> (đơn vị cơ sở). Tồn thực tế hiện tại: <strong id="card-current"><fmt:formatNumber value="${currentOnHand}" maxFractionDigits="3"/></strong></p>
                            </div>
                            <p class="inv-card__meta" id="card-count"><strong>${entryPage.totalItems}</strong> phát sinh</p>
                        </div>
                        <div class="inv-table-scroll">
                            <table class="inv-table stock-card-table" id="stock-card-table">
                                <thead>
                                    <tr>
                                        <th scope="col">Thời gian</th>
                                        <th scope="col">Kho</th>
                                        <th scope="col">Lô</th>
                                        <th scope="col">Loại</th>
                                        <th scope="col" class="is-number">Thay đổi</th>
                                        <th scope="col" class="is-number">Tồn sau</th>
                                        <th scope="col">Chứng từ gốc</th>
                                        <th scope="col">Người thực hiện</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="entry" items="${entryPage.items}">
                                        <tr>
                                            <td><c:out value="${entry.occurredAtText}"/></td>
                                            <td><c:out value="${entry.warehouseCode}"/></td>
                                            <td><c:out value="${entry.lotNo}"/></td>
                                            <td><span class="stock-card-type stock-card-type--${entry.increase ? 'in' : 'out'}"><c:out value="${entry.movementLabel}"/></span></td>
                                            <td class="is-number stock-card-change--${entry.increase ? 'in' : 'out'}">${entry.increase ? '+' : ''}<fmt:formatNumber value="${entry.qtyChange}" maxFractionDigits="3"/></td>
                                            <td class="is-number"><strong><fmt:formatNumber value="${entry.balanceAfter}" maxFractionDigits="3"/></strong></td>
                                            <td>
                                                <span class="stock-card-doc"><c:out value="${entry.documentLabel}"/></span>
                                                <c:choose>
                                                    <c:when test="${not empty entry.documentPath and linkableTypes.contains(entry.refType)}">
                                                        <a class="stock-card-doc__link" href="<c:url value='${entry.documentPath}'><c:param name='id' value='${entry.refId}'/></c:url>"><c:out value="${entry.documentNo}"/></a>
                                                    </c:when>
                                                    <c:otherwise><span><c:out value="${entry.documentNo}"/></span></c:otherwise>
                                                </c:choose>
                                                <c:if test="${not empty entry.note}"><span class="stock-card-note"><c:out value="${entry.note}"/></span></c:if>
                                            </td>
                                            <td><c:out value="${empty entry.actorName ? '—' : entry.actorName}"/></td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty entryPage.items}">
                                        <tr><td class="inv-table__empty" colspan="8">Sản phẩm chưa có phát sinh nào trong phạm vi đã chọn.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                        <c:if test="${entryPage.totalPages > 1}">
                            <nav class="stock-card-paging" aria-label="Phân trang">
                                <c:if test="${entryPage.page > 1}">
                                    <a class="inv-button" href="${fn:escapeXml(cardUrl)}&amp;page=${entryPage.page - 1}">‹ Trang trước</a>
                                </c:if>
                                <span>Trang ${entryPage.page} / ${entryPage.totalPages}</span>
                                <c:if test="${entryPage.page < entryPage.totalPages}">
                                    <a class="inv-button" href="${fn:escapeXml(cardUrl)}&amp;page=${entryPage.page + 1}">Trang sau ›</a>
                                </c:if>
                            </nav>
                        </c:if>
                    </section>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
