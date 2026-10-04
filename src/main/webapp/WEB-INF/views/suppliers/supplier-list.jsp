<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="suppliers"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Nhà cung cấp"/>
<%-- Người chỉ có quyền xem không thấy nút thêm, sửa, xoá, ngừng giao dịch và form --%>
<c:set var="canManage" value="${currentUser.can('INVENTORY_MANAGE')}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhà cung cấp | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/suppliers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header supplier-header">
                <div>
                    <h1 class="page-header__title">Danh sách nhà cung cấp</h1>
                    <p class="page-header__subtitle">Nguồn hàng của phiếu nhập kho. Nhà cung cấp đã có phiếu nhập không xoá được, chỉ ngừng giao dịch.</p>
                </div>
                <c:if test="${canManage}">
                    <a class="account-filters__create" id="create-supplier-link" href="<c:url value='/suppliers'/>#supplier-form">
                        <span aria-hidden="true">+</span> Thêm nhà cung cấp
                    </a>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="supplier-flash" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error" id="supplier-flash-error" role="alert"><p><c:out value="${flashError}"/></p></div>
            </c:if>

            <form class="account-filters" id="supplier-filter-form" action="<c:url value='/suppliers'/>" method="get" role="search">
                <div class="account-filters__field account-filters__field--keyword">
                    <label class="account-filters__label" for="supplier-keyword">Tìm kiếm</label>
                    <input class="account-filters__control" type="search" id="supplier-keyword" name="keyword"
                           value="<c:out value='${keyword}'/>" placeholder="Mã, tên, mã số thuế hoặc người liên hệ..." maxlength="100">
                </div>
            </form>

            <section class="account-table-card" aria-label="Danh sách nhà cung cấp">
                <div class="account-table-scroll">
                    <table class="account-table supplier-table" id="supplier-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã NCC</th>
                                <th scope="col">Tên nhà cung cấp</th>
                                <th scope="col">Mã số thuế</th>
                                <th scope="col">Người liên hệ</th>
                                <th scope="col">Điều khoản thanh toán</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="supplier" items="${supplierPage.items}">
                                <tr class="${editing.id == supplier.id ? 'supplier-row--editing' : ''}">
                                    <td><c:out value="${supplier.code}"/></td>
                                    <td class="account-table__name"><c:out value="${supplier.name}"/></td>
                                    <td><c:out value="${empty supplier.taxCode ? '—' : supplier.taxCode}"/></td>
                                    <td><c:out value="${empty supplier.contactName ? '—' : supplier.contactName}"/></td>
                                    <td><c:out value="${empty supplier.paymentTerms ? '—' : supplier.paymentTerms}"/></td>
                                    <td>
                                        <span class="supplier-status supplier-status--${supplier.active ? 'active' : 'inactive'}">${supplier.active ? 'Đang hoạt động' : 'Tạm ngưng'}</span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${canManage}">
                                                <div class="row-actions">
                                                    <a class="row-actions__link" href="<c:url value='/suppliers'><c:param name='edit' value='${supplier.id}'/><c:param name='keyword' value='${keyword}'/><c:param name='page' value='${supplierPage.page}'/></c:url>#supplier-form">Sửa</a>
                                                    <c:choose>
                                                        <%-- AC2: đã có phiếu nhập thì nút ngừng giao dịch thay cho nút xoá --%>
                                                        <c:when test="${supplier.hasReceipts}">
                                                            <form action="<c:url value='/suppliers/status'/>" method="post"<c:if test="${supplier.active}">
                                                                  data-confirm="Ngừng giao dịch với &quot;${fn:escapeXml(supplier.name)}&quot;? Phiếu nhập mới sẽ không chọn được nhà cung cấp này."</c:if>>
                                                                <input type="hidden" name="id" value="${supplier.id}">
                                                                <input type="hidden" name="status" value="${supplier.active ? 'INACTIVE' : 'ACTIVE'}">
                                                                <button class="row-actions__link row-actions__button" type="submit">${supplier.active ? 'Ngừng giao dịch' : 'Giao dịch lại'}</button>
                                                            </form>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <form action="<c:url value='/suppliers/delete'/>" method="post"
                                                                  data-confirm="Xóa nhà cung cấp &quot;${fn:escapeXml(supplier.name)}&quot;?">
                                                                <input type="hidden" name="id" value="${supplier.id}">
                                                                <button class="row-actions__link row-actions__button" type="submit">Xóa</button>
                                                            </form>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty supplierPage.items}">
                                <tr>
                                    <td class="category-table__empty" colspan="7">
                                        ${empty keyword ? 'Chưa có nhà cung cấp nào.' : 'Không tìm thấy nhà cung cấp phù hợp.'}
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <div class="account-pagination">
                    <p class="account-pagination__info">
                        <c:choose>
                            <c:when test="${supplierPage.totalItems == 0}">Hiển thị 0 nhà cung cấp</c:when>
                            <c:otherwise>
                                Hiển thị ${supplierPage.firstRowNumber} - ${supplierPage.firstRowNumber + supplierPage.items.size() - 1}
                                trong ${supplierPage.totalItems} nhà cung cấp
                            </c:otherwise>
                        </c:choose>
                    </p>
                    <nav aria-label="Phân trang">
                        <ul class="pagination">
                            <li>
                                <c:choose>
                                    <c:when test="${supplierPage.page > 1}">
                                        <a class="pagination__item" aria-label="Trang trước" href="<c:url value='/suppliers'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='page' value='${supplierPage.page - 1}'/>
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
                                                <c:param name='keyword' value='${keyword}'/><c:param name='page' value='${pageNumber}'/>
                                            </c:url>">${pageNumber}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                            <li>
                                <c:choose>
                                    <c:when test="${supplierPage.page < supplierPage.totalPages}">
                                        <a class="pagination__item" aria-label="Trang sau" href="<c:url value='/suppliers'>
                                            <c:param name='keyword' value='${keyword}'/><c:param name='page' value='${supplierPage.page + 1}'/>
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

            <c:if test="${canManage}">
                <form class="account-form supplier-form" id="supplier-form" method="post" novalidate
                      action="<c:url value='${empty editing ? "/suppliers/new" : "/suppliers/edit"}'/>"
                      data-has-errors="${not empty errors}">
                    <c:if test="${not empty editing}">
                        <input type="hidden" name="id" value="${editing.id}">
                    </c:if>
                    <section class="account-form__section" aria-labelledby="supplier-form-title">
                        <h2 class="account-form__section-title" id="supplier-form-title">
                            <c:choose>
                                <c:when test="${empty editing}">Thêm nhà cung cấp</c:when>
                                <c:otherwise>Sửa nhà cung cấp <c:out value="${editing.code}"/></c:otherwise>
                            </c:choose>
                        </h2>
                        <div class="supplier-form__grid">
                            <div class="form-group${not empty errors.code ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-code">Mã nhà cung cấp *</label>
                                <input class="form-group__control" type="text" id="supplier-code" name="code" maxlength="30" required
                                       value="<c:out value='${form.code}'/>" placeholder="Nhập mã nhà cung cấp" autocomplete="off"
                                       aria-describedby="supplier-code-error" aria-invalid="${not empty errors.code}">
                                <p class="form-group__error" id="supplier-code-error"${empty errors.code ? ' hidden' : ''}><c:out value="${errors.code}"/></p>
                            </div>
                            <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-name">Tên nhà cung cấp *</label>
                                <input class="form-group__control" type="text" id="supplier-name" name="name" maxlength="200" required
                                       value="<c:out value='${form.name}'/>" placeholder="Nhập tên nhà cung cấp"
                                       aria-describedby="supplier-name-error" aria-invalid="${not empty errors.name}">
                                <p class="form-group__error" id="supplier-name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                            </div>
                            <div class="form-group${not empty errors.paymentTerms ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-terms">Điều khoản thanh toán *</label>
                                <div class="select">
                                    <select class="form-group__control select__control" id="supplier-terms" name="paymentTerms" required
                                            aria-describedby="supplier-terms-error" aria-invalid="${not empty errors.paymentTerms}">
                                        <option value="">Chọn điều khoản thanh toán</option>
                                        <c:forEach var="term" items="${paymentTerms}">
                                            <option value="<c:out value='${term}'/>"${form.paymentTerms == term ? ' selected' : ''}><c:out value="${term}"/></option>
                                        </c:forEach>
                                        <%-- Điều khoản cũ ngoài danh sách vẫn hiện để giữ nguyên khi sửa ô khác --%>
                                        <c:if test="${not empty otherPaymentTerms}">
                                            <option value="<c:out value='${otherPaymentTerms}'/>" selected><c:out value="${otherPaymentTerms}"/></option>
                                        </c:if>
                                    </select>
                                </div>
                                <p class="form-group__error" id="supplier-terms-error"${empty errors.paymentTerms ? ' hidden' : ''}><c:out value="${errors.paymentTerms}"/></p>
                            </div>
                            <div class="form-group${not empty errors.taxCode ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-tax-code">Mã số thuế *</label>
                                <input class="form-group__control" type="text" id="supplier-tax-code" name="taxCode" maxlength="14" required
                                       value="<c:out value='${form.taxCode}'/>" placeholder="Nhập mã số thuế" inputmode="numeric" autocomplete="off"
                                       aria-describedby="supplier-tax-code-error" aria-invalid="${not empty errors.taxCode}">
                                <p class="form-group__error" id="supplier-tax-code-error"${empty errors.taxCode ? ' hidden' : ''}><c:out value="${errors.taxCode}"/></p>
                            </div>
                            <div class="form-group${not empty errors.contactName ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-contact">Người liên hệ *</label>
                                <input class="form-group__control" type="text" id="supplier-contact" name="contactName" maxlength="150" required
                                       value="<c:out value='${form.contactName}'/>" placeholder="Nhập tên người liên hệ"
                                       aria-describedby="supplier-contact-error" aria-invalid="${not empty errors.contactName}">
                                <p class="form-group__error" id="supplier-contact-error"${empty errors.contactName ? ' hidden' : ''}><c:out value="${errors.contactName}"/></p>
                            </div>
                            <div class="form-group${not empty errors.status ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="supplier-status">Trạng thái *</label>
                                <div class="select">
                                    <select class="form-group__control select__control" id="supplier-status" name="status" required
                                            aria-describedby="supplier-status-error" aria-invalid="${not empty errors.status}">
                                        <option value="ACTIVE"${form.status == 'ACTIVE' ? ' selected' : ''}>Đang hoạt động</option>
                                        <option value="INACTIVE"${form.status == 'INACTIVE' ? ' selected' : ''}>Tạm ngưng</option>
                                    </select>
                                </div>
                                <p class="form-group__error" id="supplier-status-error"${empty errors.status ? ' hidden' : ''}><c:out value="${errors.status}"/></p>
                            </div>
                        </div>
                    </section>
                    <footer class="account-form__footer">
                        <a class="button button--secondary" id="cancel-supplier" href="<c:url value='/suppliers'/>">Hủy</a>
                        <button class="button button--primary" type="submit" id="save-supplier">Lưu</button>
                    </footer>
                </form>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/categories.js'/>"></script>
    <script src="<c:url value='/assets/js/suppliers.js'/>"></script>
</body>
</html>
