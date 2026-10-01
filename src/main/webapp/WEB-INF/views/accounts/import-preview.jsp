<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="breadcrumbSection" value="Quản lý tài khoản"/>
<c:set var="breadcrumbPage" value="Import Excel"/>
<c:set var="importStep" value="${2}"/>
<c:set var="importSubtitle" value="Kiểm tra dữ liệu hợp lệ, dữ liệu cập nhật và các dòng đang bị lỗi."/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xem trước dữ liệu import | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <a class="import-link" id="change-file-link" href="<c:url value='/accounts/import'/>">Thay đổi file</a>
            </div>

            <dl class="import-stats">
                <div class="import-stats__item">
                    <dt>Tổng số dòng</dt>
                    <dd>${batch.totalCount}</dd>
                </div>
                <div class="import-stats__item import-stats__item--valid">
                    <dt>Hợp lệ</dt>
                    <dd>${batch.validCount}</dd>
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
                          action="<c:url value='/accounts/import/preview'/>" role="search">
                        <input type="hidden" name="status" value="<c:out value='${statusFilter}'/>">
                        <label class="visually-hidden" for="import-keyword">Tìm trong dữ liệu import</label>
                        <input class="import-search__input" type="search" id="import-keyword" name="keyword"
                               value="<c:out value='${keyword}'/>" placeholder="Tìm theo tên, email, số điện thoại..." maxlength="100">
                    </form>
                    <nav class="import-filters" aria-label="Lọc theo trạng thái">
                        <c:forEach var="option" items="all:Tất cả,valid:Hợp lệ,update:Sẽ cập nhật,error:Có lỗi">
                            <c:set var="optionValue" value="${option.split(':')[0]}"/>
                            <a class="import-filters__chip${statusFilter == optionValue ? ' import-filters__chip--active' : ''}"
                               id="import-filter-${optionValue}"${statusFilter == optionValue ? ' aria-current="true"' : ''}
                               href="<c:url value='/accounts/import/preview'><c:param name='status' value='${optionValue}'/><c:param name='keyword' value='${keyword}'/></c:url>">${option.split(':')[1]}</a>
                        </c:forEach>
                    </nav>
                </div>

                <div class="import-table-wrap">
                    <table class="import-table" id="import-preview-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Họ và tên</th>
                                <th scope="col">Email</th>
                                <th scope="col">Số điện thoại</th>
                                <th scope="col">Vai trò</th>
                                <th scope="col">Kho/Địa bàn</th>
                                <th scope="col">Chi tiết lỗi</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${rowPage.items}">
                                <tr class="${row.valid ? '' : 'import-table__row--error'}">
                                    <td>${row.index}</td>
                                    <td>
                                        <span class="import-status ${row.valid ? 'import-status--valid' : 'import-status--error'}">${row.valid ? 'Hợp lệ' : 'Có lỗi'}</span>
                                    </td>
                                    <td><c:out value="${empty row.fullName ? '—' : row.fullName}"/></td>
                                    <td><c:out value="${empty row.email ? '—' : row.email}"/></td>
                                    <td><c:out value="${empty row.phone ? '—' : row.phoneDisplay}"/></td>
                                    <td><c:out value="${not empty row.roleNames ? row.roleNames : empty row.rolesText ? '—' : row.rolesText}"/></td>
                                    <td><c:out value="${empty row.assignmentNames ? '—' : row.assignmentNames}"/></td>
                                    <td class="${row.valid ? '' : 'import-table__error'}"><c:out value="${row.valid ? '—' : row.errorText}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty rowPage.items}">
                                <tr>
                                    <td class="import-table__empty" colspan="8">Không có dòng nào phù hợp.</td>
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
                                        <a class="import-pagination__item" aria-label="Trang trước" href="<c:url value='/accounts/import/preview'>
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
                                            <a class="import-pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/accounts/import/preview'>
                                                <c:param name='status' value='${statusFilter}'/><c:param name='keyword' value='${keyword}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${rowPage.page < rowPage.totalPages}">
                                        <a class="import-pagination__item" aria-label="Trang sau" href="<c:url value='/accounts/import/preview'>
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
                  action="<c:url value='/accounts/import/preview'/>">
                <a class="import-button import-button--secondary" id="import-back" href="<c:url value='/accounts/import'/>">Quay lại</a>
                <div class="import-actions__group">
                    <a class="import-button import-button--secondary" id="import-reupload" href="<c:url value='/accounts/import'/>">Tải lại file</a>
                    <button class="import-button import-button--primary" type="submit" id="import-confirm"
                            data-busy-text="Đang import… (mỗi tài khoản gửi một email)"${batch.validCount == 0 ? ' disabled' : ''}>
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
