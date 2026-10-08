<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="discount-policies"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<c:set var="breadcrumbPage" value="Chính sách chiết khấu"/>
<c:url var="listUrl" value="/discount-policies">
    <c:param name="keyword" value="${keyword}"/>
    <c:param name="groupId" value="${groupFilter}"/>
    <c:param name="status" value="${statusFilter}"/>
</c:url>
<c:set var="listHref" value="${fn:escapeXml(listUrl)}"/>
<c:set var="isProduct" value="${empty form or form.scopeType != 'CATEGORY'}"/>
<c:set var="isPercent" value="${empty form or form.discountType != 'AMOUNT_PER_UNIT'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chính sách chiết khấu | Hệ thống quản lý bán hàng &amp; kho</title>
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
            <header class="page-header discount-header">
                <div>
                    <h1 class="page-header__title">Chính sách chiết khấu</h1>
                    <p class="page-header__subtitle">Chiết khấu theo sản lượng mua của một SKU hoặc một nhóm hàng, theo nhóm khách hàng và thời gian áp dụng.</p>
                </div>
                <c:if test="${canManage and empty form}">
                    <a class="account-filters__create" id="create-discount-policy" href="${listHref}&amp;edit=new#discount-form">
                        <span aria-hidden="true">+</span> Thêm chính sách
                    </a>
                </c:if>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash" id="discount-message" role="status"><p><c:out value="${flashMessage}"/></p></div>
            </c:if>

            <div class="discount-layout${not empty form ? ' discount-layout--with-form' : ''}">
                <div class="discount-main">
                    <form class="account-filters discount-filters" id="discount-filter-form" action="<c:url value='/discount-policies'/>" method="get" role="search">
                        <div class="account-filters__field discount-filters__keyword">
                            <label class="account-filters__label" for="discount-keyword">Tìm kiếm</label>
                            <input class="account-filters__control" type="search" id="discount-keyword" name="keyword" maxlength="100"
                                   value="<c:out value='${keyword}'/>" placeholder="Mã, tên chính sách, SKU hoặc nhóm hàng">
                        </div>
                        <div class="account-filters__field">
                            <label class="account-filters__label" for="discount-group">Nhóm khách hàng</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="discount-group" name="groupId">
                                    <option value="">Tất cả</option>
                                    <option value="0"${groupFilter == 0 ? ' selected' : ''}>Áp cho mọi nhóm</option>
                                    <c:forEach var="group" items="${groups}">
                                        <option value="${group.id}"${group.id == groupFilter ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="account-filters__field">
                            <label class="account-filters__label" for="discount-status">Trạng thái</label>
                            <div class="select">
                                <select class="account-filters__control select__control" id="discount-status" name="status">
                                    <option value="">Tất cả</option>
                                    <c:forEach var="status" items="${statuses}">
                                        <option value="${status.code}"${status.code == statusFilter ? ' selected' : ''}>${status.label}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="discount-filters__actions">
                            <button class="price-filters__search" type="submit">Lọc</button>
                        </div>
                    </form>

                    <section class="account-table-card discount-card" aria-label="Danh sách chính sách chiết khấu">
                        <div class="account-table-scroll">
                            <table class="account-table discount-table" id="discount-table">
                                <thead>
                                    <tr>
                                        <th scope="col">STT</th>
                                        <th scope="col">Chính sách</th>
                                        <th scope="col">Nhóm KH</th>
                                        <th scope="col">Áp cho</th>
                                        <th scope="col">Bậc chiết khấu</th>
                                        <th scope="col">Thời gian áp dụng</th>
                                        <th scope="col">Trạng thái</th>
                                        <c:if test="${canManage}"><th scope="col">Thao tác</th></c:if>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="policy" items="${policyPage.items}" varStatus="loop">
                                        <tr class="${policy.id == editing.id ? 'discount-table__row--editing' : ''}">
                                            <td class="account-table__index">${policyPage.firstRowNumber + loop.index}</td>
                                            <td>
                                                <span class="discount-table__name"><c:out value="${policy.name}"/></span>
                                                <span class="price-history__sub"><c:out value="${policy.code}"/></span>
                                            </td>
                                            <td><c:out value="${empty policy.customerGroupName ? 'Mọi nhóm' : policy.customerGroupName}"/></td>
                                            <td>
                                                <span class="discount-scope">${policy.productScope ? 'SKU' : 'Nhóm hàng'}</span>
                                                <span class="discount-table__target"><c:out value="${policy.productScope ? policy.targetCode : policy.targetName}"/></span>
                                                <c:if test="${policy.productScope}"><span class="price-history__sub"><c:out value="${policy.targetName}"/></span></c:if>
                                            </td>
                                            <td class="discount-table__tiers"><c:out value="${policy.tierSummary}"/></td>
                                            <td class="price-date">${policy.validFromText} – ${policy.validToText}</td>
                                            <td><span class="status-badge discount-badge--${fn:toLowerCase(policy.status.code)}">${policy.status.label}</span></td>
                                            <c:if test="${canManage}">
                                                <td>
                                                    <div class="row-actions discount-actions">
                                                        <a class="price-view-link" href="${listHref}&amp;page=${policyPage.page}&amp;edit=${policy.id}#discount-form">Sửa</a>
                                                        <form method="post" action="<c:url value='/discount-policies/action'/>">
                                                            <input type="hidden" name="id" value="${policy.id}">
                                                            <c:forEach var="name" items="${['keyword', 'groupId', 'status', 'page']}">
                                                                <c:if test="${not empty param[name]}"><input type="hidden" name="${name}" value="<c:out value='${param[name]}'/>"></c:if>
                                                            </c:forEach>
                                                            <button class="discount-actions__button" type="submit" name="action" value="${policy.active ? 'deactivate' : 'activate'}">${policy.active ? 'Ngừng' : 'Áp dụng lại'}</button>
                                                        </form>
                                                        <form method="post" action="<c:url value='/discount-policies/action'/>"
                                                              data-confirm="${policy.used ? 'Chính sách đã có đơn hàng dùng nên không xoá được, sẽ chuyển sang ngừng áp dụng. Tiếp tục?' : 'Xoá chính sách '.concat(fn:escapeXml(policy.code)).concat('?')}">
                                                            <input type="hidden" name="id" value="${policy.id}">
                                                            <c:forEach var="name" items="${['keyword', 'groupId', 'status', 'page']}">
                                                                <c:if test="${not empty param[name]}"><input type="hidden" name="${name}" value="<c:out value='${param[name]}'/>"></c:if>
                                                            </c:forEach>
                                                            <button class="discount-actions__button discount-actions__button--danger" type="submit" name="action" value="remove">Xoá</button>
                                                        </form>
                                                    </div>
                                                </td>
                                            </c:if>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty policyPage.items}">
                                        <tr>
                                            <td class="price-history__empty" colspan="${canManage ? 8 : 7}">
                                                <c:choose>
                                                    <c:when test="${not empty keyword or not empty groupFilter or not empty statusFilter}">Không có chính sách phù hợp. Thử bỏ bớt điều kiện lọc.</c:when>
                                                    <c:otherwise>Chưa có chính sách chiết khấu nào.<c:if test="${canManage}"> Bấm "Thêm chính sách" để khai báo.</c:if></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>

                        <div class="account-pagination">
                            <p class="account-pagination__info">
                                <c:choose>
                                    <c:when test="${policyPage.totalItems == 0}">Không có chính sách nào</c:when>
                                    <c:otherwise>Hiển thị ${policyPage.firstRowNumber} - ${policyPage.firstRowNumber + fn:length(policyPage.items) - 1} trong tổng ${policyPage.totalItems} chính sách</c:otherwise>
                                </c:choose>
                            </p>
                            <nav aria-label="Phân trang">
                                <ul class="pagination">
                                    <li>
                                        <c:choose>
                                            <c:when test="${policyPage.page > 1}"><a class="pagination__item" aria-label="Trang trước" href="${listHref}&amp;page=${policyPage.page - 1}">‹</a></c:when>
                                            <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">‹</span></c:otherwise>
                                        </c:choose>
                                    </li>
                                    <c:forEach var="pageNumber" begin="${policyPage.startPage}" end="${policyPage.endPage}">
                                        <li>
                                            <c:choose>
                                                <c:when test="${pageNumber == policyPage.page}"><span class="pagination__item pagination__item--active" aria-current="page">${pageNumber}</span></c:when>
                                                <c:otherwise><a class="pagination__item" aria-label="Trang ${pageNumber}" href="${listHref}&amp;page=${pageNumber}">${pageNumber}</a></c:otherwise>
                                            </c:choose>
                                        </li>
                                    </c:forEach>
                                    <li>
                                        <c:choose>
                                            <c:when test="${policyPage.page < policyPage.totalPages}"><a class="pagination__item" aria-label="Trang sau" href="${listHref}&amp;page=${policyPage.page + 1}">›</a></c:when>
                                            <c:otherwise><span class="pagination__item pagination__item--disabled" aria-hidden="true">›</span></c:otherwise>
                                        </c:choose>
                                    </li>
                                </ul>
                            </nav>
                        </div>
                    </section>

                    <aside class="discount-rule" id="discount-rule" aria-labelledby="discount-rule-title">
                        <h2 class="discount-rule__title" id="discount-rule-title">Quy tắc áp dụng khi tạo đơn hàng</h2>
                        <ul class="discount-rule__list">
                            <li>Số lượng so với bậc tính theo <strong>đơn vị cơ sở</strong> (vd 2 Thùng × 24 = 48 Lon); dòng hàng được bậc cao nhất mà số lượng đạt tới.</li>
                            <li>Chính sách theo nhóm hàng áp cho cả sản phẩm trong các nhóm con.</li>
                            <li>Nhiều chính sách cùng áp cho một dòng hàng thì <strong>lấy chính sách có lợi nhất cho khách</strong> (số tiền chiết khấu lớn nhất), không cộng dồn.</li>
                            <li>Đơn đã tạo giữ nguyên chiết khấu đã tính; sửa hoặc ngừng chính sách chỉ ảnh hưởng đơn tạo sau đó.</li>
                        </ul>
                        <p class="discount-rule__doc">Chi tiết và ví dụ: tài liệu <code>docs/quy-tac-chiet-khau.md</code>.</p>
                    </aside>
                </div>

                <c:if test="${not empty form}">
                    <form class="discount-form" id="discount-form" method="post" action="<c:url value='/discount-policies/save'/>" novalidate>
                        <c:if test="${not empty form.id}"><input type="hidden" name="id" value="${form.id}"></c:if>
                        <c:forEach var="name" items="${['keyword', 'groupId', 'status', 'page']}">
                            <c:if test="${not empty param[name]}"><input type="hidden" name="${name}" value="<c:out value='${param[name]}'/>"></c:if>
                        </c:forEach>
                        <div class="discount-form__head">
                            <h2 class="discount-form__title">${empty form.id ? 'Thêm chính sách' : 'Sửa chính sách '.concat(fn:escapeXml(editing.code))}</h2>
                            <a class="discount-form__close" href="${listHref}" aria-label="Đóng">×</a>
                        </div>
                        <c:if test="${not empty editing and editing.used}">
                            <p class="discount-form__note">Chính sách đã có đơn hàng dùng: đơn cũ giữ nguyên chiết khấu đã tính, thay đổi chỉ áp cho đơn tạo sau.</p>
                        </c:if>

                        <section class="discount-form__section" aria-labelledby="discount-basic-title">
                            <h3 class="discount-form__section-title" id="discount-basic-title">1. Thông tin cơ bản</h3>
                            <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="discount-name">Tên chính sách *</label>
                                <input class="form-group__control" type="text" id="discount-name" name="name" maxlength="150" required
                                       value="<c:out value='${form.name}'/>" placeholder="Ví dụ: Đại lý cấp 1 - Bia lon theo thùng">
                                <p class="form-group__error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                            </div>
                            <div class="form-group${not empty errors.customerGroupId ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="discount-form-group">Nhóm khách hàng</label>
                                <div class="select">
                                    <select class="form-group__control select__control" id="discount-form-group" name="customerGroupId">
                                        <option value="">Mọi nhóm khách hàng</option>
                                        <c:forEach var="group" items="${groups}">
                                            <option value="${group.id}"${form.customerGroupId == group.id.toString() ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <p class="form-group__error"${empty errors.customerGroupId ? ' hidden' : ''}><c:out value="${errors.customerGroupId}"/></p>
                            </div>
                            <fieldset class="discount-choice${not empty errors.scopeType ? ' form-group--invalid' : ''}">
                                <legend class="form-group__label">Áp cho *</legend>
                                <label class="discount-choice__option"><input type="radio" name="scopeType" value="PRODUCT" data-scope-choice${isProduct ? ' checked' : ''}> Một SKU</label>
                                <label class="discount-choice__option"><input type="radio" name="scopeType" value="CATEGORY" data-scope-choice${isProduct ? '' : ' checked'}> Một nhóm hàng (gồm nhóm con)</label>
                            </fieldset>
                            <div class="form-group${not empty errors.productSku ? ' form-group--invalid' : ''}" data-scope="PRODUCT"${isProduct ? '' : ' hidden'}>
                                <label class="form-group__label" for="discount-sku">Mã SKU *</label>
                                <input class="form-group__control" type="text" id="discount-sku" name="productSku" maxlength="50" list="discount-products"
                                       value="<c:out value='${form.productSku}'/>" placeholder="Gõ SKU hoặc chọn trong danh sách gợi ý" autocomplete="off">
                                <datalist id="discount-products">
                                    <c:forEach var="product" items="${products}"><option value="<c:out value='${product.code}'/>"><c:out value="${product.name}"/></option></c:forEach>
                                </datalist>
                                <p class="form-group__error"${empty errors.productSku ? ' hidden' : ''}><c:out value="${errors.productSku}"/></p>
                            </div>
                            <div class="form-group${not empty errors.categoryId ? ' form-group--invalid' : ''}" data-scope="CATEGORY"${isProduct ? ' hidden' : ''}>
                                <label class="form-group__label" for="discount-category">Nhóm hàng *</label>
                                <div class="select">
                                    <select class="form-group__control select__control" id="discount-category" name="categoryId">
                                        <option value="">Chọn nhóm hàng</option>
                                        <c:forEach var="category" items="${categories}">
                                            <option value="${category.id}"${form.categoryId == category.id.toString() ? ' selected' : ''}><c:forEach begin="2" end="${category.level}">&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${category.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <p class="form-group__error"${empty errors.categoryId ? ' hidden' : ''}><c:out value="${errors.categoryId}"/></p>
                            </div>
                            <fieldset class="discount-choice${not empty errors.discountType ? ' form-group--invalid' : ''}">
                                <legend class="form-group__label">Cách tính *</legend>
                                <label class="discount-choice__option"><input type="radio" name="discountType" value="PERCENT" data-type-choice${isPercent ? ' checked' : ''}> Phần trăm (%)</label>
                                <label class="discount-choice__option"><input type="radio" name="discountType" value="AMOUNT_PER_UNIT" data-type-choice${isPercent ? '' : ' checked'}> Số tiền trên đơn vị cơ sở</label>
                            </fieldset>
                        </section>

                        <section class="discount-form__section" aria-labelledby="discount-time-title">
                            <h3 class="discount-form__section-title" id="discount-time-title">2. Thời gian áp dụng</h3>
                            <div class="discount-form__row">
                                <div class="form-group${not empty errors.validFrom ? ' form-group--invalid' : ''}">
                                    <label class="form-group__label" for="discount-from">Ngày bắt đầu *</label>
                                    <input class="form-group__control" type="date" id="discount-from" name="validFrom" required value="<c:out value='${form.validFrom}'/>">
                                    <p class="form-group__error"${empty errors.validFrom ? ' hidden' : ''}><c:out value="${errors.validFrom}"/></p>
                                </div>
                                <div class="form-group${not empty errors.validTo ? ' form-group--invalid' : ''}">
                                    <label class="form-group__label" for="discount-to">Ngày kết thúc</label>
                                    <input class="form-group__control" type="date" id="discount-to" name="validTo" value="<c:out value='${form.validTo}'/>">
                                    <p class="${not empty errors.validTo ? 'form-group__error' : 'form-group__hint'}"><c:out value="${not empty errors.validTo ? errors.validTo : 'Để trống nếu không thời hạn.'}"/></p>
                                </div>
                            </div>
                        </section>

                        <section class="discount-form__section" aria-labelledby="discount-tier-title">
                            <h3 class="discount-form__section-title" id="discount-tier-title">3. Bậc chiết khấu theo số lượng</h3>
                            <p class="form-group__hint">Mua từ số lượng tối thiểu (đơn vị cơ sở) trở lên thì được mức chiết khấu tương ứng.</p>
                            <table class="discount-tiers" id="discount-tiers">
                                <thead>
                                    <tr>
                                        <th scope="col">Mua từ (đơn vị cơ sở)</th>
                                        <th scope="col">Chiết khấu <span data-type-unit>${isPercent ? '(%)' : '(đ / đơn vị)'}</span></th>
                                        <th scope="col"><span class="visually-hidden">Xoá bậc</span></th>
                                    </tr>
                                </thead>
                                <tbody id="discount-tier-rows">
                                    <c:forEach var="tier" items="${form.tiers}" varStatus="loop">
                                        <tr data-tier-row>
                                            <td><input class="form-group__control" type="text" name="tierMinQty" inputmode="decimal" maxlength="16"
                                                       value="<c:out value='${tier.minQty()}'/>" placeholder="Vd 24" aria-label="Số lượng tối thiểu bậc ${loop.index + 1}"></td>
                                            <td><input class="form-group__control" type="text" name="tierValue" inputmode="decimal" maxlength="16"
                                                       value="<c:out value='${tier.value()}'/>" placeholder="${isPercent ? 'Vd 2,5' : 'Vd 500'}" aria-label="Mức chiết khấu bậc ${loop.index + 1}"></td>
                                            <td><button class="discount-tiers__remove" type="button" data-tier-remove aria-label="Xoá bậc ${loop.index + 1}">×</button></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                            <c:if test="${not empty errors.tiers}"><p class="form-group__error discount-tiers__error" role="alert"><c:out value="${errors.tiers}"/></p></c:if>
                            <button class="discount-tiers__add" type="button" id="discount-tier-add">+ Thêm bậc</button>
                        </section>

                        <label class="discount-switch">
                            <input class="discount-switch__input" type="checkbox" name="active" value="1"${form.active ? ' checked' : ''}>
                            <span class="discount-switch__track" aria-hidden="true"></span>
                            <span>Đang áp dụng</span>
                        </label>

                        <div class="discount-form__actions">
                            <a class="discount-form__cancel" href="${listHref}">Hủy</a>
                            <button class="price-filters__search discount-form__save" type="submit" id="discount-save">Lưu chính sách</button>
                        </div>
                    </form>
                </c:if>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/discount-policies.js'/>"></script>
</body>
</html>
