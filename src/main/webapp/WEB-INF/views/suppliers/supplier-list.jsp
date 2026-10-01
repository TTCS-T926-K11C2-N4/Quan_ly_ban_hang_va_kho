<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="suppliers"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Nhà cung cấp"/>
<%-- Người chỉ có quyền xem (kinh doanh, kế toán) không thấy nút thêm, sửa, ngừng giao dịch, xoá --%>
<c:set var="canManage" value="${currentUser.can('INVENTORY_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh mục nhà cung cấp | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <%-- Dùng lại bộ lọc, bảng, phân trang và nút của nhóm màn hình Quản lý tài khoản --%>
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/suppliers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">Danh mục nhà cung cấp</h1>
                <p class="page-header__subtitle">Nguồn hàng gắn với phiếu nhập kho, dùng để truy nguyên khi có lô lỗi.</p>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="supplier-message" role="status">
                    <p><c:out value="${flashMessage}"/></p>
                </div>
            </c:if>
            <c:if test="${not empty flashError}">
                <p class="supplier-alert" id="supplier-error" role="alert"><c:out value="${flashError}"/></p>
            </c:if>

            <form class="account-filters" id="supplier-filter-form" action="<c:url value='/suppliers'/>" method="get" role="search">
                <div class="account-filters__field account-filters__field--keyword">
                    <label class="account-filters__label" for="supplier-keyword">Tìm kiếm</label>
                    <input class="account-filters__control" type="search" id="supplier-keyword" name="keyword"
                           value="<c:out value='${keyword}'/>" placeholder="Tìm theo mã, tên, mã số thuế, người liên hệ, SĐT"
                           maxlength="100">
                </div>

                <div class="account-filters__field account-filters__field--status">
                    <label class="account-filters__label" for="status-filter">Trạng thái</label>
                    <div class="select">
                        <select class="account-filters__control select__control" id="status-filter" name="statusFilter">
                            <option value="">Tất cả</option>
                            <option value="ACTIVE"${statusFilter == 'ACTIVE' ? ' selected' : ''}>Đang giao dịch</option>
                            <option value="INACTIVE"${statusFilter == 'INACTIVE' ? ' selected' : ''}>Ngừng giao dịch</option>
                        </select>
                    </div>
                </div>

                <c:if test="${canManage}">
                    <a class="account-filters__create" id="create-supplier-link" href="<c:url value='/suppliers/new'/>">
                        <span aria-hidden="true">+</span> Thêm nhà cung cấp
                    </a>
                </c:if>
            </form>

            <section class="account-table-card" aria-label="Danh sách nhà cung cấp">
                <div class="account-table-scroll">
                    <table class="account-table supplier-table">
                        <thead>
                            <tr>
                                <th scope="col">STT</th>
                                <th scope="col">Mã</th>
                                <th scope="col">Tên nhà cung cấp</th>
                                <th scope="col">Mã số thuế</th>
                                <th scope="col">Người liên hệ</th>
                                <th scope="col">Điều khoản thanh toán</th>
                                <th scope="col">Trạng thái</th>
                                <c:if test="${canManage}"><th scope="col">Thao tác</th></c:if>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="supplier" items="${supplierPage.items}" varStatus="loop">
                                <tr>
                                    <td class="account-table__index">${supplierPage.firstRowNumber + loop.index}</td>
                                    <td class="supplier-table__code"><c:out value="${supplier.code}"/></td>
                                    <td class="account-table__name"><c:out value="${supplier.name}"/></td>
                                    <td><c:out value="${empty supplier.taxCode ? '—' : supplier.taxCode}"/></td>
                                    <td>
                                        <c:out value="${empty supplier.contactName ? '—' : supplier.contactName}"/>
                                        <c:if test="${not empty supplier.phone or not empty supplier.email}">
                                            <span class="supplier-table__contact">
                                                <c:out value="${supplier.phone}"/><c:if test="${not empty supplier.phone and not empty supplier.email}"> · </c:if><c:out value="${supplier.email}"/>
                                            </span>
                                        </c:if>
                                    </td>
                                    <td><c:out value="${empty supplier.paymentTerms ? '—' : supplier.paymentTerms}"/></td>
                                    <td>
                                        <span class="status-badge status-badge--${fn:toLowerCase(supplier.status)}">${supplier.active ? 'Đang giao dịch' : 'Ngừng giao dịch'}</span>
                                    </td>
                                    <c:if test="${canManage}">
                                    <td>
                                        <div class="row-actions">
                                            <a class="row-actions__link" href="<c:url value='/suppliers/edit'><c:param name='id' value='${supplier.id}'/></c:url>">Sửa</a>
                                            <details class="row-menu">
                                                <summary class="row-menu__toggle" aria-label="Thao tác khác cho nhà cung cấp ${fn:escapeXml(supplier.code)}">⋮</summary>
                                                <div class="row-menu__list">
                                                    <%-- Thao tác đổi dữ liệu dùng POST; data-confirm để supplier-list.js hỏi lại trước khi gửi --%>
                                                    <form action="<c:url value='/suppliers/status'/>" method="post"
                                                          data-confirm="${supplier.active ? 'Ngừng giao dịch với nhà cung cấp ' : 'Cho giao dịch lại với nhà cung cấp '}${fn:escapeXml(supplier.code)}?">
                                                        <input type="hidden" name="id" value="${supplier.id}">
                                                        <input type="hidden" name="active" value="${not supplier.active}">
                                                        <button class="row-menu__item" type="submit">${supplier.active ? 'Ngừng giao dịch' : 'Giao dịch lại'}</button>
                                                    </form>
                                                    <%-- Đã có phiếu nhập kho thì không xoá được (S2-09), chỉ ngừng giao dịch --%>
                                                    <c:if test="${supplier.deletable}">
                                                        <form action="<c:url value='/suppliers/delete'/>" method="post"
                                                              data-confirm="Xoá nhà cung cấp ${fn:escapeXml(supplier.code)}? Thao tác này không hoàn tác được.">
                                                            <input type="hidden" name="id" value="${supplier.id}">
                                                            <button class="row-menu__item row-menu__item--danger" type="submit">Xoá</button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </details>
                                        </div>
                                    </td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty supplierPage.items}">
                                <tr>
                                    <td class="supplier-table__empty" colspan="${canManage ? 8 : 7}">
                                        ${empty keyword and empty statusFilter ? 'Chưa có nhà cung cấp nào.' : 'Không tìm thấy nhà cung cấp phù hợp.'}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">${supplierPage.totalItems} nhà cung cấp · ${supplierPage.pageSize} dòng/trang</p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${supplierPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/suppliers'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='statusFilter' value='${statusFilter}'/>
                                            <c:param name='page' value='${supplierPage.page - 1}'/>
                                        </c:url>">‹</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span>
                                    </c:otherwise>
                                </c:choose>
                            </li>
                            <c:forEach var="pageNumber" begin="${supplierPage.startPage}" end="${supplierPage.endPage}">
                                <li>
                                    <c:choose>
                                        <c:when test="${pageNumber == supplierPage.page}">
                                            <span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="pagination__item" aria-label="Trang ${pageNumber}" href="<c:url value='/suppliers'>
                                                <c:param name='keyword' value='${keyword}'/><c:param name='statusFilter' value='${statusFilter}'/>
                                                <c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${supplierPage.page < supplierPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/suppliers'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='statusFilter' value='${statusFilter}'/>
                                            <c:param name='page' value='${supplierPage.page + 1}'/>
                                        </c:url>">›</a>
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

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/supplier-list.js'/>"></script>
</body>
</html>
