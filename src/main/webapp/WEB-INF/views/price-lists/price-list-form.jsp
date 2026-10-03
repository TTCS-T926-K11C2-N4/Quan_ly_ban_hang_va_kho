<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="price-lists"/>
<c:set var="breadcrumbSection" value="Bảng giá"/>
<%-- Dùng chung cho Tạo (mode=create), Sửa (edit) và Tạo phiên bản (version); source là bảng đang sửa / bản cũ --%>
<c:set var="pageTitle" value="${mode == 'edit' ? 'Sửa bảng giá' : mode == 'version' ? 'Tạo phiên bản bảng giá' : 'Tạo bảng giá mới'}"/>
<c:set var="breadcrumbPage" value="${pageTitle}"/>
<c:set var="formAction" value="${mode == 'edit' ? '/price-lists/edit' : mode == 'version' ? '/price-lists/version' : '/price-lists/new'}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle} | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/accounts.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/categories.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/price-lists.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">
                    <c:choose>
                        <c:when test="${mode == 'version'}">Phiên bản mới của bảng giá <strong><c:out value="${source.code}"/></strong>: dòng giá được chép sẵn để sửa. Bản cũ sẽ kết thúc hiệu lực hôm trước ngày bản mới bắt đầu.</c:when>
                        <c:when test="${mode == 'edit'}">Sửa bảng giá <strong><c:out value="${source.code}"/></strong> (chưa có đơn sử dụng).</c:when>
                        <c:otherwise>Mã bảng giá được cấp tự động. Mỗi nhóm khách hàng mỗi ngày chỉ có một bảng giá.</c:otherwise>
                    </c:choose>
                </p>
            </header>

            <c:if test="${not empty errors}">
                <div class="account-flash account-flash--error" role="alert"><p>Bảng giá chưa được lưu. Vui lòng kiểm tra các ô báo lỗi bên dưới.</p></div>
            </c:if>

            <form class="account-form" id="price-list-form" method="post" action="<c:url value='${formAction}'/>">
                <c:if test="${mode != 'create'}">
                    <input type="hidden" name="id" value="${source.id}">
                </c:if>

                <section class="account-form__section" aria-labelledby="price-info-title">
                    <h2 class="account-form__section-title" id="price-info-title">1. Thông tin chung</h2>
                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.customerGroupId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="price-group">Nhóm khách hàng *</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="price-group" name="customerGroupId" required
                                        ${mode == 'version' ? 'disabled' : ''} aria-describedby="price-group-hint price-group-error"
                                        aria-invalid="${not empty errors.customerGroupId}">
                                    <option value="">— Chọn nhóm —</option>
                                    <c:forEach var="group" items="${groups}">
                                        <option value="${group.id}"${form.customerGroupId == group.id ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__hint" id="price-group-hint">${mode == 'version' ? 'Phiên bản mới giữ nguyên nhóm của bản cũ.' : 'Đại lý thuộc nhóm nào nhận giá của bảng giá nhóm đó.'}</p>
                            <p class="form-group__error" id="price-group-error"${empty errors.customerGroupId ? ' hidden' : ''}><c:out value="${errors.customerGroupId}"/></p>
                        </div>

                        <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="price-name">Tên bảng giá *</label>
                            <input class="form-group__control" type="text" id="price-name" name="name" maxlength="150" required
                                   value="<c:out value='${form.name}'/>" placeholder="Ví dụ: Giá đại lý cấp 1 - 2026" autocomplete="off"
                                   aria-describedby="price-name-error" aria-invalid="${not empty errors.name}">
                            <p class="form-group__error" id="price-name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                        </div>

                        <div class="form-group${not empty errors.validFrom ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="price-from">Ngày bắt đầu hiệu lực *</label>
                            <input class="form-group__control" type="date" id="price-from" name="validFrom" required
                                   value="${form.validFrom}"<c:if test="${mode == 'version'}"> min="${earliestStart}"</c:if>
                                   aria-describedby="price-from-error" aria-invalid="${not empty errors.validFrom}">
                            <p class="form-group__error" id="price-from-error"${empty errors.validFrom ? ' hidden' : ''}><c:out value="${errors.validFrom}"/></p>
                        </div>

                        <div class="form-group${not empty errors.validTo ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="price-to">Ngày kết thúc hiệu lực *</label>
                            <input class="form-group__control" type="date" id="price-to" name="validTo" required
                                   value="${form.validTo}" aria-describedby="price-to-error" aria-invalid="${not empty errors.validTo}">
                            <p class="form-group__error" id="price-to-error"${empty errors.validTo ? ' hidden' : ''}><c:out value="${errors.validTo}"/></p>
                        </div>
                    </div>
                </section>

                <section class="account-form__section account-form__section--divided" aria-labelledby="price-lines-title">
                    <h2 class="account-form__section-title" id="price-lines-title">2. Nhập giá bán &amp; giá sàn</h2>
                    <p class="account-form__section-desc">Giá theo đơn vị tính cơ sở của sản phẩm. Bán dưới giá sàn sẽ phải qua duyệt. Giá sàn không được cao hơn giá bán.</p>
                    <c:if test="${not empty errors.lines}">
                        <p class="form-group__error price-lines__error" role="alert"><c:out value="${errors.lines}"/></p>
                    </c:if>

                    <div class="account-table-scroll">
                        <table class="account-table price-lines" id="price-lines">
                            <thead>
                                <tr>
                                    <th scope="col">STT</th>
                                    <th scope="col">Sản phẩm (SKU)</th>
                                    <th scope="col">Đơn vị</th>
                                    <c:if test="${canViewCost}"><th scope="col" class="price-number">Giá vốn</th></c:if>
                                    <th scope="col">Giá bán (VNĐ) *</th>
                                    <th scope="col">Giá sàn (VNĐ) *</th>
                                    <th scope="col"><span class="visually-hidden">Xoá dòng</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="line" items="${form.lines}" varStatus="loop">
                                    <c:set var="lineKey">lines.${loop.index}</c:set>
                                    <tr class="price-line${not empty errors[lineKey] ? ' price-line--invalid' : ''}">
                                        <td class="account-table__index price-line__index">${loop.index + 1}</td>
                                        <td>
                                            <div class="select">
                                                <select class="form-group__control select__control price-line__product" name="productId" aria-label="Sản phẩm dòng ${loop.index + 1}">
                                                    <option value="">— Chọn sản phẩm —</option>
                                                    <c:forEach var="product" items="${products}">
                                                        <option value="${product.productId}" data-unit="<c:out value='${product.unitName}'/>"
                                                                data-cost="<fmt:formatNumber value='${product.costPrice}' maxFractionDigits='0'/>"${line.productId == product.productId ? ' selected' : ''}><c:out value="${product.sku}"/> — <c:out value="${product.productName}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <c:if test="${not empty errors[lineKey]}"><p class="form-group__error"><c:out value="${errors[lineKey]}"/></p></c:if>
                                        </td>
                                        <td class="price-line__unit">—</td>
                                        <c:if test="${canViewCost}"><td class="price-number price-number--muted price-line__cost">—</td></c:if>
                                        <td><input class="form-group__control price-line__money" type="text" name="price" inputmode="numeric" maxlength="20"
                                                   value="<c:out value='${line.price}'/>" placeholder="0" aria-label="Giá bán dòng ${loop.index + 1}"></td>
                                        <td><input class="form-group__control price-line__money" type="text" name="floorPrice" inputmode="numeric" maxlength="20"
                                                   value="<c:out value='${line.floorPrice}'/>" placeholder="0" aria-label="Giá sàn dòng ${loop.index + 1}"></td>
                                        <td><button class="price-line__remove" type="button" aria-label="Xoá dòng ${loop.index + 1}" title="Xoá dòng">×</button></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <p class="price-lines__empty" id="price-lines-empty"${empty form.lines ? '' : ' hidden'}>Chưa có dòng giá nào. Bấm "+ Nhập giá theo SKU" để thêm.</p>
                    <button class="price-lines__add" type="button" id="add-price-line">+ Nhập giá theo SKU</button>
                </section>

                <footer class="account-form__footer">
                    <a class="button button--secondary" href="<c:url value='/price-lists'><c:if test="${mode != 'create'}"><c:param name='id' value='${source.id}'/></c:if></c:url>">Hủy</a>
                    <button class="button button--primary" type="submit" id="save-price-list">${mode == 'version' ? 'Tạo phiên bản' : 'Lưu bảng giá'}</button>
                </footer>
            </form>

            <%-- Mẫu dòng giá trống, price-lists.js chép khi bấm "+ Nhập giá theo SKU" --%>
            <template id="price-line-template">
                <tr class="price-line">
                    <td class="account-table__index price-line__index"></td>
                    <td>
                        <div class="select">
                            <select class="form-group__control select__control price-line__product" name="productId" aria-label="Sản phẩm">
                                <option value="">— Chọn sản phẩm —</option>
                                <c:forEach var="product" items="${products}">
                                    <option value="${product.productId}" data-unit="<c:out value='${product.unitName}'/>"
                                            data-cost="<fmt:formatNumber value='${product.costPrice}' maxFractionDigits='0'/>"><c:out value="${product.sku}"/> — <c:out value="${product.productName}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </td>
                    <td class="price-line__unit">—</td>
                    <c:if test="${canViewCost}"><td class="price-number price-number--muted price-line__cost">—</td></c:if>
                    <td><input class="form-group__control price-line__money" type="text" name="price" inputmode="numeric" maxlength="20" placeholder="0" aria-label="Giá bán"></td>
                    <td><input class="form-group__control price-line__money" type="text" name="floorPrice" inputmode="numeric" maxlength="20" placeholder="0" aria-label="Giá sàn"></td>
                    <td><button class="price-line__remove" type="button" aria-label="Xoá dòng" title="Xoá dòng">×</button></td>
                </tr>
            </template>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/price-lists.js'/>"></script>
</body>
</html>
