<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="products"/>
<c:set var="activeSubmenu" value="product-list"/>
<c:set var="breadcrumbSection" value="Sản phẩm"/>
<%-- Dùng chung cho Thêm và Sửa sản phẩm; Servlet sửa gán editing = true, product và hasTransactions --%>
<c:set var="pageTitle" value="${editing ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}"/>
<c:set var="breadcrumbPage" value="${pageTitle}"/>
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
    <link rel="stylesheet" href="<c:url value='/assets/css/products.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">
                    ${editing ? 'Cập nhật thông tin sản phẩm. Mã SKU dùng chung cho cả công ty nên không được trùng.' : 'Khai báo sản phẩm mới vào danh mục. Mã SKU dùng chung cho cả công ty nên không được trùng.'}
                </p>
            </header>

            <c:if test="${not empty formError}">
                <div class="account-flash account-flash--error" id="product-form-error" role="alert"><p><c:out value="${formError}"/></p></div>
            </c:if>

            <form class="account-form" id="product-form" method="post" enctype="multipart/form-data"
                  action="<c:url value='${editing ? "/products/edit" : "/products/new"}'/>">
                <c:if test="${editing}">
                    <input type="hidden" name="id" value="${product.id}">
                    <input type="hidden" name="version" value="${form.version}">
                </c:if>
                <section class="account-form__section" aria-labelledby="product-info-title">
                    <h2 class="account-form__section-title" id="product-info-title">Thông tin sản phẩm</h2>
                    <p class="account-form__section-desc">Các ô có dấu * là bắt buộc. Tồn kho và sổ sách luôn tính theo đơn vị tính cơ sở.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.sku ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-sku">Mã SKU *</label>
                            <input class="form-group__control" type="text" id="product-sku" name="sku"
                                   value="<c:out value='${form.sku}'/>" placeholder="Ví dụ: SP001" maxlength="50"
                                   autocomplete="off" required
                                   aria-describedby="product-sku-hint product-sku-error" aria-invalid="${not empty errors.sku}">
                            <p class="form-group__hint" id="product-sku-hint">2–50 ký tự: chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang. Tự đổi sang chữ hoa.</p>
                            <p class="form-group__error" id="product-sku-error"${empty errors.sku ? ' hidden' : ''}><c:out value="${errors.sku}"/></p>
                        </div>

                        <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-name">Tên sản phẩm *</label>
                            <input class="form-group__control" type="text" id="product-name" name="name"
                                   value="<c:out value='${form.name}'/>" placeholder="Ví dụ: Sữa Milo hộp 180ml" maxlength="250"
                                   autocomplete="off" required
                                   aria-describedby="product-name-error" aria-invalid="${not empty errors.name}">
                            <p class="form-group__error" id="product-name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                        </div>

                        <div class="form-group${not empty errors.categoryId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-category">Nhóm hàng *</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="product-category" name="categoryId" required
                                        aria-describedby="product-category-error" aria-invalid="${not empty errors.categoryId}">
                                    <option value="">— Chọn nhóm hàng —</option>
                                    <c:forEach var="option" items="${categoryOptions}">
                                        <option value="${option.id}"${form.categoryId == option.id ? ' selected' : ''}><c:forEach begin="2" end="${option.level}">&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach><c:out value="${option.name}"/>${option.active ? '' : ' (ngừng hoạt động)'}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__error" id="product-category-error"${empty errors.categoryId ? ' hidden' : ''}><c:out value="${errors.categoryId}"/></p>
                        </div>

                        <div class="form-group${not empty errors.baseUnitId ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-unit">Đơn vị tính cơ sở *</label>
                            <div class="select">
                                <select class="form-group__control select__control" id="product-unit" name="baseUnitId" required
                                        aria-describedby="product-unit-hint product-unit-error" aria-invalid="${not empty errors.baseUnitId}">
                                    <option value="">— Chọn đơn vị —</option>
                                    <c:forEach var="unit" items="${units}">
                                        <option value="${unit.id}"${form.baseUnitId == unit.id ? ' selected' : ''}><c:out value="${unit.name}"/></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <p class="form-group__hint" id="product-unit-hint">
                                ${editing and hasTransactions ? 'Sản phẩm đã phát sinh giao dịch nên không đổi được đơn vị cơ sở.' : 'Đơn vị nhỏ nhất khi xuất nhập, vd lon, chai, gói.'}
                            </p>
                            <p class="form-group__error" id="product-unit-error"${empty errors.baseUnitId ? ' hidden' : ''}><c:out value="${errors.baseUnitId}"/></p>
                        </div>

                        <div class="form-group${not empty errors.packagingSpec ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-packaging">Quy cách đóng gói</label>
                            <input class="form-group__control" type="text" id="product-packaging" name="packagingSpec"
                                   value="<c:out value='${form.packagingSpec}'/>" placeholder="Ví dụ: Thùng 24 lon x 330ml" maxlength="100"
                                   autocomplete="off"
                                   aria-describedby="product-packaging-error" aria-invalid="${not empty errors.packagingSpec}">
                            <p class="form-group__error" id="product-packaging-error"${empty errors.packagingSpec ? ' hidden' : ''}><c:out value="${errors.packagingSpec}"/></p>
                        </div>

                        <c:if test="${canEditCost}">
                            <div class="form-group${not empty errors.costPrice ? ' form-group--invalid' : ''}">
                                <label class="form-group__label" for="product-cost">Giá vốn (VNĐ)</label>
                                <input class="form-group__control" type="text" id="product-cost" name="costPrice" inputmode="numeric"
                                       value="<c:out value='${form.costPrice}'/>" placeholder="0" maxlength="25" autocomplete="off"
                                       aria-describedby="product-cost-hint product-cost-error" aria-invalid="${not empty errors.costPrice}">
                                <p class="form-group__hint" id="product-cost-hint">Chỉ Quản lý kinh doanh xem và sửa được. Số tiền đồng, vd 30.000.</p>
                                <p class="form-group__error" id="product-cost-error"${empty errors.costPrice ? ' hidden' : ''}><c:out value="${errors.costPrice}"/></p>
                            </div>
                        </c:if>

                        <div class="form-group${not empty errors.status ? ' form-group--invalid' : ''}">
                            <span class="form-group__label" id="product-status-label">Trạng thái *</span>
                            <%-- Bỏ chọn công tắc thì trình duyệt không gửi ô checkbox: ô ẩn phía sau gửi DISCONTINUED.
                                 Khi bật, ACTIVE đứng trước nên getParameter("status") lấy ACTIVE. --%>
                            <label class="switch" for="product-status">
                                <input class="switch__input" type="checkbox" id="product-status" name="status" value="ACTIVE"
                                       ${form.status != 'DISCONTINUED' ? 'checked' : ''} aria-labelledby="product-status-label product-status-text">
                                <span class="switch__track" aria-hidden="true"></span>
                                <span class="switch__text" id="product-status-text" data-on="Đang kinh doanh" data-off="Ngừng kinh doanh">${form.status == 'DISCONTINUED' ? 'Ngừng kinh doanh' : 'Đang kinh doanh'}</span>
                            </label>
                            <input type="hidden" name="status" value="DISCONTINUED">
                            <p class="form-group__error" id="product-status-error"${empty errors.status ? ' hidden' : ''}><c:out value="${errors.status}"/></p>
                        </div>

                        <div class="form-group form-group--full${not empty errors.image ? ' form-group--invalid' : ''}">
                            <span class="form-group__label" id="product-image-label">Ảnh sản phẩm</span>
                            <div class="product-image-field" id="product-image-drop">
                                <img class="product-image-field__preview" id="product-image-preview" alt="" width="96" height="96"
                                     src="<c:url value='${editing ? "/products/image" : "/assets/img/product-placeholder.svg"}'><c:if test="${editing}"><c:param name='id' value='${product.id}'/><c:param name='v' value='${product.imageFileId}'/></c:if></c:url>">
                                <div>
                                    <p class="product-image-field__drop-text">Kéo thả ảnh vào đây hoặc chọn từ máy tính</p>
                                    <label class="button button--primary product-image-field__button" for="product-image">
                                        <img src="<c:url value='/assets/img/icons/upload.svg'/>" alt="" width="16" height="16">
                                        ${editing and not empty product.imageFileId ? 'Đổi ảnh' : 'Chọn ảnh'}
                                    </label>
                                    <input class="product-image-field__input" type="file" id="product-image" name="image"
                                           accept="image/jpeg,image/png" data-max-bytes="2097152"
                                           aria-labelledby="product-image-label" aria-describedby="product-image-hint product-image-error">
                                    <p class="form-group__hint" id="product-image-hint">JPG hoặc PNG, tối đa 2MB. Ảnh được cắt vuông ở giữa.<c:if test="${not empty errors and empty errors.image}"> Nếu đã chọn ảnh, hãy chọn lại.</c:if></p>
                                    <p class="form-group__error" id="product-image-error"${empty errors.image ? ' hidden' : ''}><c:out value="${errors.image}"/></p>
                                </div>
                            </div>
                        </div>

                        <div class="form-group form-group--full${not empty errors.description ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="product-description">Mô tả</label>
                            <textarea class="form-group__control form-group__control--textarea" id="product-description"
                                      name="description" maxlength="2000" placeholder="Thông tin thêm về sản phẩm"
                                      aria-describedby="product-description-error" aria-invalid="${not empty errors.description}"><c:out value="${form.description}"/></textarea>
                            <p class="form-group__error" id="product-description-error"${empty errors.description ? ' hidden' : ''}><c:out value="${errors.description}"/></p>
                        </div>
                    </div>
                </section>

                <section class="account-form__section account-form__section--divided" aria-labelledby="product-conversion-title">
                    <h2 class="account-form__section-title" id="product-conversion-title">Quy đổi đơn vị</h2>
                    <p class="account-form__section-desc">
                        Khai báo các đơn vị quy đổi từ đơn vị cơ sở (lon/lốc/thùng). Đơn hàng và phiếu kho nhập theo đơn vị nào cũng được quy về đơn vị cơ sở khi ghi sổ.
                        <c:if test="${editing and hasTransactions}"> Sản phẩm đã có giao dịch: đổi hệ số chỉ áp dụng cho giao dịch mới, giao dịch đã ghi giữ hệ số lúc ghi.</c:if>
                    </p>
                    <c:if test="${not empty errors.conversions}">
                        <p class="form-group__error" role="alert"><c:out value="${errors.conversions}"/></p>
                    </c:if>
                    <div class="account-table-scroll">
                        <table class="account-table conversion-table" id="conversion-table">
                            <thead>
                                <tr>
                                    <th scope="col">Đơn vị</th>
                                    <th scope="col">Hệ số quy đổi</th>
                                    <th scope="col">Mã đơn vị</th>
                                    <th scope="col">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="conversion" items="${form.conversions}" varStatus="loop">
                                    <c:set var="conversionKey">conversions.${loop.index}</c:set>
                                    <tr class="conversion-row${not empty errors[conversionKey] ? ' conversion-row--invalid' : ''}">
                                        <td>
                                            <div class="select">
                                                <select class="form-group__control select__control conversion-row__unit" name="conversionUnitId" aria-label="Đơn vị quy đổi dòng ${loop.index + 1}">
                                                    <option value="">— Chọn đơn vị —</option>
                                                    <c:forEach var="unit" items="${units}">
                                                        <option value="${unit.id}" data-code="<c:out value='${unit.code}'/>"${conversion.unitId == unit.id ? ' selected' : ''}><c:out value="${unit.name}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                            <c:if test="${not empty errors[conversionKey]}"><p class="form-group__error"><c:out value="${errors[conversionKey]}"/></p></c:if>
                                        </td>
                                        <td>
                                            <input class="form-group__control conversion-row__factor" type="text" name="conversionFactor" inputmode="decimal" maxlength="20"
                                                   value="<c:out value='${conversion.factor}'/>" placeholder="Ví dụ: 24" aria-label="Hệ số quy đổi dòng ${loop.index + 1}">
                                            <p class="form-group__hint conversion-row__hint"></p>
                                        </td>
                                        <td class="conversion-row__code">—</td>
                                        <td><button class="conversion-row__remove" type="button" aria-label="Xoá dòng ${loop.index + 1}" title="Xoá dòng">×</button></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <p class="conversion-empty" id="conversion-empty"${empty form.conversions ? '' : ' hidden'}>Chưa có đơn vị quy đổi. Sản phẩm chỉ xuất nhập theo đơn vị cơ sở.</p>
                    <button class="conversion-add" type="button" id="add-conversion">+ Thêm đơn vị quy đổi</button>
                </section>

                <footer class="account-form__footer">
                    <a class="button button--secondary" id="cancel-product-form" href="<c:url value='/products'/>">Hủy</a>
                    <button class="button button--primary" type="submit" id="save-product-submit">${editing ? 'Lưu thay đổi' : 'Thêm sản phẩm'}</button>
                </footer>
            </form>

            <%-- Mẫu dòng quy đổi trống, products.js chép khi bấm "+ Thêm đơn vị quy đổi" --%>
            <template id="conversion-template">
                <tr class="conversion-row">
                    <td>
                        <div class="select">
                            <select class="form-group__control select__control conversion-row__unit" name="conversionUnitId" aria-label="Đơn vị quy đổi">
                                <option value="">— Chọn đơn vị —</option>
                                <c:forEach var="unit" items="${units}">
                                    <option value="${unit.id}" data-code="<c:out value='${unit.code}'/>"><c:out value="${unit.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </td>
                    <td>
                        <input class="form-group__control conversion-row__factor" type="text" name="conversionFactor" inputmode="decimal" maxlength="20" placeholder="Ví dụ: 24" aria-label="Hệ số quy đổi">
                        <p class="form-group__hint conversion-row__hint"></p>
                    </td>
                    <td class="conversion-row__code">—</td>
                    <td><button class="conversion-row__remove" type="button" aria-label="Xoá dòng" title="Xoá dòng">×</button></td>
                </tr>
            </template>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/products.js'/>"></script>
</body>
</html>
