<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="product-list"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Import Excel"/>
<c:set var="importType" value="products"/>
<c:set var="importStep" value="${2}"/>
<c:set var="importSubtitle" value="Kiểm tra sản phẩm sẽ thêm mới, sản phẩm sẽ cập nhật theo SKU đã có và các dòng đang bị lỗi."/>
<fmt:setLocale value="vi_VN"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xem trước sản phẩm import | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/import.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content import-page">
            <%@ include file="/WEB-INF/views/accounts/import-header.jspf" %>

            <div class="import-file import-file--bar">
                <span class="import-file__icon" aria-hidden="true">X</span>
                <p class="import-file__summary">
                    <strong><c:out value="${batch.fileName}"/></strong> — ${batch.totalCount} dòng dữ liệu
                </p>
                <a class="import-link" id="change-file-link" href="<c:url value='/products/import'/>">Thay đổi file</a>
            </div>

            <dl class="import-stats">
                <div class="import-stats__item">
                    <dt>Tổng số dòng</dt>
                    <dd>${batch.totalCount}</dd>
                </div>
                <div class="import-stats__item import-stats__item--valid">
                    <dt>Thêm mới</dt>
                    <dd>${batch.newCount}</dd>
                </div>
                <div class="import-stats__item import-stats__item--update">
                    <dt>Sẽ cập nhật</dt>
                    <dd>${batch.updateCount}</dd>
                </div>
                <div class="import-stats__item import-stats__item--error">
                    <dt>Có lỗi</dt>
                    <dd>${batch.errorCount}</dd>
                </div>
            </dl>

            <section class="import-card import-table-card" aria-label="Dữ liệu trong file">
                <div class="import-toolbar">
                    <form class="import-search" id="import-search-form" method="get"
                          action="<c:url value='/products/import/preview'/>" role="search">
                        <input type="hidden" name="status" value="<c:out value='${statusFilter}'/>">
                        <label class="visually-hidden" for="import-keyword">Tìm trong dữ liệu import</label>
                        <input class="import-search__input" type="search" id="import-keyword" name="keyword"
                               value="<c:out value='${keyword}'/>" placeholder="Tìm theo mã SKU, tên sản phẩm..." maxlength="100">
                    </form>
                    <nav class="import-filters" aria-label="Lọc theo trạng thái">
                        <c:forEach var="option" items="all:Tất cả,new:Thêm mới,update:Sẽ cập nhật,error:Có lỗi">
                            <c:set var="optionValue" value="${option.split(':')[0]}"/>
                            <a class="import-filters__chip${statusFilter == optionValue ? ' import-filters__chip--active' : ''}"
                               id="import-filter-${optionValue}"${statusFilter == optionValue ? ' aria-current="true"' : ''}
                               href="<c:url value='/products/import/preview'><c:param name='status' value='${optionValue}'/><c:param name='keyword' value='${keyword}'/></c:url>">${option.split(':')[1]}</a>
                        </c:forEach>
                    </nav>
                </div>

                <%-- Dòng hợp lệ hiện giá trị sẽ lưu (ô trống của SKU đã có = giá trị đang lưu); dòng lỗi hiện nguyên ô trong file --%>
                <div class="import-table-wrap">
                    <table class="import-table" id="import-preview-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Mã SKU</th>
                                <th scope="col">Tên sản phẩm</th>
                                <th scope="col">Nhóm hàng</th>
                                <th scope="col">Đơn vị cơ sở</th>
                                <th scope="col">Quy đổi</th>
                                <c:if test="${canViewCost}"><th scope="col">Giá vốn</th></c:if>
                                <th scope="col">Chi tiết lỗi</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${rowPage.items}">
                                <tr class="${row.valid ? '' : 'import-table__row--error'}">
                                    <td>${row.index}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not row.valid}"><span class="import-status import-status--error">Có lỗi</span></c:when>
                                            <c:when test="${row.update}"><span class="import-status import-status--update">Cập nhật</span></c:when>
                                            <c:otherwise><span class="import-status import-status--valid">Thêm mới</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><c:out value="${row.valid ? row.sku : empty row.cells[0] ? '—' : row.cells[0]}"/></td>
                                    <c:choose>
                                        <c:when test="${row.valid}">
                                            <td><c:out value="${row.form.name}"/></td>
                                            <td><c:out value="${row.categoryName}"/></td>
                                            <td><c:out value="${row.unitName}"/></td>
                                            <td><c:out value="${empty row.conversionText ? '—' : row.conversionText}"/></td>
                                            <c:if test="${canViewCost}">
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${empty row.costPrice}">Giữ nguyên</c:when>
                                                        <c:otherwise><fmt:formatNumber value="${row.costPrice}" maxFractionDigits="0"/> ₫</c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </c:if>
                                        </c:when>
                                        <c:otherwise>
                                            <td><c:out value="${empty row.cells[1] ? '—' : row.cells[1]}"/></td>
                                            <td><c:out value="${empty row.cells[2] ? '—' : row.cells[2]}"/></td>
                                            <td><c:out value="${empty row.cells[3] ? '—' : row.cells[3]}"/></td>
                                            <td><c:out value="${empty row.cells[8] ? '—' : row.cells[8]}"/></td>
                                            <c:if test="${canViewCost}"><td><c:out value="${empty row.cells[5] ? '—' : row.cells[5]}"/></td></c:if>
                                        </c:otherwise>
                                    </c:choose>
                                    <td class="${row.valid ? '' : 'import-table__error'}"><c:out value="${row.valid ? '—' : row.errorText}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rowPage.items}">
                                <tr>
                                    <td class="import-table__empty" colspan="${canViewCost ? 9 : 8}">Không có dòng nào phù hợp.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="import-pagination">
                    <p class="import-pagination__info">
                        <c:choose>
                            <c:when test="${rowPage.totalItems == 0}">Hiển thị 0 dòng</c:when>
                            <c:otherwise>Hiển thị ${rowPage.firstRowNumber}–${rowPage.firstRowNumber + rowPage.items.size() - 1} trong ${rowPage.totalItems} dòng</c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="import-pagination__list">
                            <li>
                                <c:choose>
                                    <c:when test="${rowPage.page > 1}">
                                        <a class="import-pagination__item" aria-label="Trang trước" href="<c:url value='/products/import/preview'>
                                            <c:param name='status' value='${statusFilter}'/><c:param name='keyword' value='${keyword}'/><c:param name='page' value='${rowPage.page - 1}'/>
                                        </c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="import-pagination__item import-pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${rowPage.startPage}" end="${rowPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == rowPage.page}">
                                            <span class="import-pagination__item import-pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="import-pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/products/import/preview'>
                                                <c:param name='status' value='${statusFilter}'/><c:param name='keyword' value='${keyword}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${rowPage.page < rowPage.totalPages}">
                                        <a class="import-pagination__item" aria-label="Trang sau" href="<c:url value='/products/import/preview'>
                                            <c:param name='status' value='${statusFilter}'/><c:param name='keyword' value='${keyword}'/><c:param name='page' value='${rowPage.page + 1}'/>
                                        </c:url>">›</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="import-pagination__item import-pagination__item--disabled" aria-hidden="true">›</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                        </ul>
                    </nav>
                </div>
            </section>

            <form class="import-actions import-actions--split" id="import-confirm-form" method="post"
                  action="<c:url value='/products/import/preview'/>">
                <a class="import-button import-button--secondary" id="import-back" href="<c:url value='/products/import'/>">Quay lại</a>
                <div class="import-actions__group">
                    <a class="import-button import-button--secondary" id="import-reupload" href="<c:url value='/products/import'/>">Tải lại file</a>
                    <button class="import-button import-button--primary" type="submit" id="import-confirm"
                            data-busy-text="Đang import… (file lớn có thể mất vài phút)"${batch.validCount == 0 ? ' disabled' : ''}>
                        Import ${batch.validCount} dòng hợp lệ
                    </button>
                </div>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/account-import.js'/>"></script>
</body>
</html>
