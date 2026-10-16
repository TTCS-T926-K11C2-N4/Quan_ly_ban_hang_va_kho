<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="lots"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Lô và hạn sử dụng"/>
<c:url var="listUrl" value="/lots">
    <c:param name="keyword" value="${keyword}"/>
    <c:param name="warehouse" value="${warehouseId}"/>
    <c:param name="status" value="${status.code}"/>
    <c:param name="sku" value="${productKeyword}"/>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lô và hạn sử dụng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/lots.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Lô và hạn sử dụng</h1>
                    <p class="inv-header__subtitle">Theo dõi số lô, ngày sản xuất, hạn dùng và số còn lại; cảnh báo lô sắp hết hạn để không giao nhầm cho đại lý.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="lot-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="lot-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-stats" aria-label="Tình trạng hạn dùng của các lô còn hàng">
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Lô quá hạn</p>
                    <p class="inv-stat__value" id="stat-expired"><fmt:formatNumber value="${expiredCount}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">Không được soạn / xuất trừ khi Quản lý kho cho phép</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Lô sắp hết hạn</p>
                    <p class="inv-stat__value" id="stat-near"><fmt:formatNumber value="${nearCount}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">Trong <strong>${nearDays}</strong> ngày tới</p>
                </article>
                <article class="inv-stat inv-stat--stacked">
                    <p class="inv-stat__label">Lô còn hạn</p>
                    <p class="inv-stat__value" id="stat-ok"><fmt:formatNumber value="${okCount}" minIntegerDigits="2"/></p>
                    <p class="inv-stat__note">${noneCount} lô không khai báo hạn</p>
                </article>
            </section>

            <c:if test="${canApprove}">
                <section class="inv-card" aria-labelledby="lot-threshold-title">
                    <h2 class="inv-card__title" id="lot-threshold-title">Ngưỡng cảnh báo sắp hết hạn</h2>
                    <form class="lot-threshold" id="lot-threshold-form" method="post" action="<c:url value='/lots/threshold'/>">
                        <input type="hidden" name="r_keyword" value="<c:out value='${keyword}'/>">
                        <input type="hidden" name="r_warehouse" value="${warehouseId}">
                        <input type="hidden" name="r_status" value="${status.code}">
                        <label class="stock-filters__label" for="lot-days">Cảnh báo khi hạn dùng còn không quá (ngày)</label>
                        <input class="inv-control lot-threshold__input" type="number" id="lot-days" name="days" min="1" max="365" value="${nearDays}" required>
                        <button class="inv-button inv-button--primary" type="submit" id="lot-threshold-save">Lưu ngưỡng</button>
                    </form>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="lot-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="lot-table-title">Danh sách lô còn hàng</h2>
                        <p class="inv-card__subtitle">Hạn dùng gần nhất lên đầu; số lượng theo đơn vị cơ sở.</p>
                    </div>
                    <p class="inv-card__meta" id="lot-count"><strong>${lotPage.totalItems}</strong> dòng</p>
                </div>
                <form class="lot-filters" id="lot-filter-form" action="<c:url value='/lots'/>" method="get" role="search">
                    <div class="inv-field lot-filters__keyword">
                        <label class="stock-filters__label" for="lot-keyword">SKU, tên hàng hoặc số lô</label>
                        <input class="inv-control" type="search" id="lot-keyword" name="keyword" maxlength="100" value="<c:out value='${keyword}'/>">
                    </div>
                    <div class="inv-field">
                        <label class="stock-filters__label" for="lot-warehouse">Kho</label>
                        <select class="inv-control" id="lot-warehouse" name="warehouse">
                            <option value="">Tất cả kho</option>
                            <c:forEach var="warehouse" items="${warehouses}">
                                <option value="${warehouse.id}"${warehouse.id == warehouseId ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="inv-field">
                        <label class="stock-filters__label" for="lot-status">Tình trạng</label>
                        <select class="inv-control" id="lot-status" name="status">
                            <option value="">Tất cả</option>
                            <c:forEach var="item" items="${statuses}">
                                <option value="${item.code}"${item == status ? ' selected' : ''}>${item.label}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <input type="hidden" name="sku" value="<c:out value='${productKeyword}'/>">
                    <button class="inv-button inv-button--primary" type="submit" id="lot-filter-submit">Lọc</button>
                </form>
                <div class="inv-table-scroll">
                    <table class="inv-table lot-table" id="lot-table">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm / SKU</th>
                                <th scope="col">Số lô</th>
                                <th scope="col">Ngày sản xuất</th>
                                <th scope="col">Hạn dùng</th>
                                <th scope="col">Tình trạng</th>
                                <th scope="col">Kho</th>
                                <th scope="col" class="is-number">Còn lại</th>
                                <c:if test="${canApprove}"><th scope="col">Xuất lô quá hạn</th></c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="lot" items="${lotPage.items}">
                                <tr class="lot-table__row--${lot.status.tone}">
                                    <td><span class="inv-table__main"><c:out value="${lot.productName}"/></span><span class="inv-table__sub">SKU: <c:out value="${lot.sku}"/></span></td>
                                    <td><c:out value="${lot.lotNo}"/></td>
                                    <td><c:out value="${empty lot.manufactureDateText ? '—' : lot.manufactureDateText}"/></td>
                                    <td><c:out value="${empty lot.expiryDateText ? '—' : lot.expiryDateText}"/></td>
                                    <td>
                                        <span class="lot-pill lot-pill--${lot.status.tone}">${lot.status.label}</span>
                                        <c:if test="${not empty lot.daysLeft}">
                                            <span class="lot-days">${lot.daysLeft >= 0 ? 'còn ' : 'quá '}${lot.daysLeft >= 0 ? lot.daysLeft : -lot.daysLeft} ngày</span>
                                        </c:if>
                                    </td>
                                    <td><c:out value="${lot.warehouseCode}"/></td>
                                    <td class="is-number"><fmt:formatNumber value="${lot.qty}" maxFractionDigits="3"/> <c:out value="${lot.baseUnitName}"/></td>
                                    <c:if test="${canApprove}">
                                        <td>
                                            <c:choose>
                                                <c:when test="${lot.status == 'EXPIRED' and lot.overridden}">
                                                    <span class="lot-override">Đã cho phép (<c:out value="${lot.overrideBy}"/>, <c:out value="${lot.overrideAtText}"/>): <c:out value="${lot.overrideReason}"/></span>
                                                    <form method="post" action="<c:url value='/lots/override'/>">
                                                        <input type="hidden" name="lot" value="${lot.lotId}">
                                                        <input type="hidden" name="mode" value="revoke">
                                                        <input type="hidden" name="r_keyword" value="<c:out value='${keyword}'/>">
                                                        <input type="hidden" name="r_warehouse" value="${warehouseId}">
                                                        <input type="hidden" name="r_status" value="${status.code}">
                                                        <button class="inv-button inv-button--small" type="submit">Thu hồi</button>
                                                    </form>
                                                </c:when>
                                                <c:when test="${lot.status == 'EXPIRED'}">
                                                    <form class="lot-override-form" method="post" action="<c:url value='/lots/override'/>">
                                                        <input type="hidden" name="lot" value="${lot.lotId}">
                                                        <input type="hidden" name="mode" value="allow">
                                                        <input type="hidden" name="r_keyword" value="<c:out value='${keyword}'/>">
                                                        <input type="hidden" name="r_warehouse" value="${warehouseId}">
                                                        <input type="hidden" name="r_status" value="${status.code}">
                                                        <input class="inv-control" type="text" name="reason" maxlength="${reasonMaxLength}" placeholder="Lý do (bắt buộc)" required aria-label="Lý do cho phép xuất lô ${lot.lotNo}">
                                                        <button class="inv-button inv-button--small" type="submit">Cho phép xuất</button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty lotPage.items}">
                                <tr><td class="inv-table__empty" colspan="${canApprove ? 8 : 7}">Chưa có lô nào còn hàng trong phạm vi đã chọn. Bật quản lý lô cho SKU rồi nhập kho kèm số lô và hạn dùng.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
                <c:if test="${lotPage.totalPages > 1}">
                    <nav class="lot-paging" aria-label="Phân trang">
                        <c:if test="${lotPage.page > 1}"><a class="inv-button" href="${fn:escapeXml(listUrl)}&amp;page=${lotPage.page - 1}">‹ Trang trước</a></c:if>
                        <span>Trang ${lotPage.page} / ${lotPage.totalPages}</span>
                        <c:if test="${lotPage.page < lotPage.totalPages}"><a class="inv-button" href="${fn:escapeXml(listUrl)}&amp;page=${lotPage.page + 1}">Trang sau ›</a></c:if>
                    </nav>
                </c:if>
            </section>

            <section class="inv-card" aria-labelledby="lot-products-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="lot-products-title">Quản lý lô theo SKU</h2>
                        <p class="inv-card__subtitle">SKU bật quản lý lô thì nhập kho phải ghi số lô và hạn dùng; SKU tắt dùng một lô mặc định.</p>
                    </div>
                </div>
                <form class="lot-filters" action="<c:url value='/lots'/>" method="get" role="search">
                    <div class="inv-field lot-filters__keyword">
                        <label class="stock-filters__label" for="lot-sku">Tìm SKU</label>
                        <input class="inv-control" type="search" id="lot-sku" name="sku" maxlength="100" value="<c:out value='${productKeyword}'/>">
                    </div>
                    <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                    <input type="hidden" name="warehouse" value="${warehouseId}">
                    <input type="hidden" name="status" value="${status.code}">
                    <button class="inv-button" type="submit">Tìm</button>
                </form>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="lot-products-table">
                        <thead>
                            <tr>
                                <th scope="col">Sản phẩm / SKU</th>
                                <th scope="col">Quản lý lô</th>
                                <th scope="col" class="is-number">Lô còn hàng</th>
                                <c:if test="${canManage}"><th scope="col"><span class="visually-hidden">Thao tác</span></th></c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="product" items="${products}">
                                <tr>
                                    <td><span class="inv-table__main"><c:out value="${product.name}"/></span><span class="inv-table__sub">SKU: <c:out value="${product.sku}"/></span></td>
                                    <td><span class="lot-pill lot-pill--${product.tracked ? 'success' : 'neutral'}">${product.tracked ? 'Đang bật' : 'Tắt'}</span></td>
                                    <td class="is-number">${product.lotsInStock}</td>
                                    <c:if test="${canManage}">
                                        <td>
                                            <form method="post" action="<c:url value='/lots/track'/>">
                                                <input type="hidden" name="product" value="${product.id}">
                                                <input type="hidden" name="mode" value="${product.tracked ? 'off' : 'on'}">
                                                <input type="hidden" name="r_keyword" value="<c:out value='${keyword}'/>">
                                                <input type="hidden" name="r_warehouse" value="${warehouseId}">
                                                <input type="hidden" name="r_status" value="${status.code}">
                                                <input type="hidden" name="r_sku" value="<c:out value='${productKeyword}'/>">
                                                <button class="inv-button inv-button--small" type="submit">${product.tracked ? 'Tắt quản lý lô' : 'Bật quản lý lô'}</button>
                                            </form>
                                        </td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty products}">
                                <tr><td class="inv-table__empty" colspan="${canManage ? 4 : 3}">Không có SKU nào khớp.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
