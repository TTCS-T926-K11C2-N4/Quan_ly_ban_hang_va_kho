<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="goods-receipts"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Phiếu nhập kho"/>
<c:set var="breadcrumbParentUrl" value="/goods-receipts"/>
<c:set var="pageTitle" value="${not empty editing ? 'Sửa phiếu nhập nháp' : (form.adjustment ? 'Lập phiếu điều chỉnh' : 'Tạo phiếu nhập kho')}"/>
<c:set var="breadcrumbPage" value="${not empty editing ? 'Sửa phiếu' : (form.adjustment ? 'Phiếu điều chỉnh' : 'Tạo phiếu')}"/>
<c:choose>
    <c:when test="${empty editing}"><c:url var="formAction" value="/goods-receipts/new"/></c:when>
    <c:otherwise><c:url var="formAction" value="/goods-receipts/edit"><c:param name="id" value="${editing.id}"/></c:url></c:otherwise>
</c:choose>
<c:set var="lineLocations" value="${locationsByWarehouse[form.warehouseId]}"/>
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
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/goods-receipts.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page receipt-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">${pageTitle}</h1>
                    <p class="inv-header__subtitle">Nhập thông tin chứng từ và danh sách hàng hóa nhận vào kho.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="receipt-back" href="<c:url value='/goods-receipts'/>">
                        <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Quay lại
                    </a>
                </div>
            </header>

            <c:if test="${not empty errors.post}">
                <div class="inv-flash inv-flash--error" id="receipt-post-error" role="alert">
                    Chưa ghi sổ được: <c:out value="${errors.post}"/>
                </div>
            </c:if>
            <c:if test="${form.adjustment and not empty original}">
                <c:url var="originalUrl" value="/goods-receipts/view"><c:param name="id" value="${original.id}"/></c:url>
                <div class="inv-note" id="receipt-adjust-note">
                    <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                    <p><strong>Phiếu điều chỉnh cho <a class="receipt-note__link" href="${fn:escapeXml(originalUrl)}"><c:out value="${original.receiptNo}"/></a>:</strong>
                        nhập số dương để tăng, số âm để giảm số lượng đã nhập; nhà cung cấp và kho nhận giữ theo phiếu gốc.</p>
                </div>
            </c:if>

            <form class="receipt-form" id="receipt-form" method="post" action="${fn:escapeXml(formAction)}" novalidate
                  data-adjustment="${form.adjustment}">
                <c:if test="${form.adjustment}">
                    <input type="hidden" name="adjustsReceiptId" value="<c:out value='${form.adjustsReceiptId}'/>">
                </c:if>

                <section class="inv-card" aria-labelledby="receipt-info-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="receipt-info-title">Thông tin phiếu nhập</h2>
                            <p class="inv-card__subtitle">Nhập đầy đủ các trường bắt buộc trước khi lưu phiếu.</p>
                        </div>
                        <span class="inv-pill inv-pill--orange">Nháp</span>
                    </div>
                    <div class="inv-card__body receipt-info">
                        <div class="inv-field">
                            <label class="inv-field__label" for="receipt-no">Mã phiếu</label>
                            <input class="inv-control" type="text" id="receipt-no" value="<c:out value='${receiptNo}'/>" readonly>
                            <p class="inv-field__hint">${empty editing ? 'Mã dự kiến; mã chính thức được cấp khi lưu phiếu.' : 'Mã phiếu đã cấp.'}</p>
                        </div>
                        <div class="inv-field${not empty errors.receiptDate ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="receipt-date">Ngày nhập<span class="inv-field__required">*</span></label>
                            <input class="inv-control" type="date" id="receipt-date" name="receiptDate" max="${today}" required
                                   value="<c:out value='${form.receiptDate}'/>">
                            <c:if test="${not empty errors.receiptDate}"><p class="inv-field__error"><c:out value="${errors.receiptDate}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.supplierId ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="receipt-supplier">Nhà cung cấp<span class="inv-field__required">*</span></label>
                            <c:choose>
                                <c:when test="${form.adjustment}">
                                    <input type="hidden" name="supplierId" value="<c:out value='${form.supplierId}'/>">
                                    <input class="inv-control" type="text" id="receipt-supplier" value="<c:out value='${original.supplierName}'/>" readonly>
                                </c:when>
                                <c:otherwise>
                                    <select class="inv-control" id="receipt-supplier" name="supplierId" required>
                                        <option value="">Chọn nhà cung cấp</option>
                                        <c:forEach var="supplier" items="${options.suppliers}">
                                            <option value="${supplier.id}"${supplier.id.toString() == form.supplierId ? ' selected' : ''}><c:out value="${supplier.name}"/></option>
                                        </c:forEach>
                                        <%-- Nhà cung cấp của phiếu nháp vừa ngừng giao dịch vẫn giữ được khi lưu lại --%>
                                        <c:if test="${not empty currentSupplier}">
                                            <option value="${currentSupplier.id}"${currentSupplier.id.toString() == form.supplierId ? ' selected' : ''}><c:out value="${currentSupplier.name}"/> (ngừng giao dịch)</option>
                                        </c:if>
                                    </select>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty errors.supplierId}"><p class="inv-field__error"><c:out value="${errors.supplierId}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.warehouseId ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="receipt-warehouse">Kho nhận<span class="inv-field__required">*</span></label>
                            <c:choose>
                                <c:when test="${form.adjustment}">
                                    <input type="hidden" id="receipt-warehouse" name="warehouseId" value="<c:out value='${form.warehouseId}'/>">
                                    <input class="inv-control" type="text" value="<c:out value='${original.warehouseCode} · ${original.warehouseName}'/>" readonly>
                                </c:when>
                                <c:otherwise>
                                    <select class="inv-control" id="receipt-warehouse" name="warehouseId" required>
                                        <option value="">Chọn kho nhận</option>
                                        <c:forEach var="warehouse" items="${options.warehouses}">
                                            <option value="${warehouse.id}"${warehouse.id.toString() == form.warehouseId ? ' selected' : ''}><c:out value="${warehouse.code}"/> · <c:out value="${warehouse.name}"/></option>
                                        </c:forEach>
                                    </select>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty errors.warehouseId}"><p class="inv-field__error"><c:out value="${errors.warehouseId}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.documentRef ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="receipt-document">Số chứng từ nhà cung cấp</label>
                            <input class="inv-control" type="text" id="receipt-document" name="documentRef" maxlength="60"
                                   value="<c:out value='${form.documentRef}'/>" placeholder="VD: HĐ 0001234 hoặc số phiếu giao hàng">
                            <c:if test="${not empty errors.documentRef}"><p class="inv-field__error"><c:out value="${errors.documentRef}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.note ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="receipt-note">Ghi chú</label>
                            <input class="inv-control" type="text" id="receipt-note" name="note" maxlength="1000"
                                   value="<c:out value='${form.note}'/>" placeholder="Nhập ghi chú cho phiếu nhập (nếu có)">
                            <c:if test="${not empty errors.note}"><p class="inv-field__error"><c:out value="${errors.note}"/></p></c:if>
                        </div>
                    </div>
                </section>

                <section class="inv-card" aria-labelledby="receipt-lines-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="receipt-lines-title">Danh sách hàng nhập</h2>
                            <p class="inv-card__subtitle">Số lượng cơ sở được hiển thị trực tiếp trên từng dòng hàng.</p>
                        </div>
                        <button class="inv-button inv-button--outline" type="button" id="receipt-add-line">
                            <span class="inv-icon inv-icon--plus" aria-hidden="true"></span>Thêm dòng hàng
                        </button>
                    </div>
                    <div class="inv-card__body receipt-lines">
                        <c:if test="${not empty errors.lines}">
                            <p class="inv-field__error receipt-lines__error" id="receipt-lines-error" role="alert"><c:out value="${errors.lines}"/></p>
                        </c:if>
                        <div class="inv-table-scroll">
                            <table class="receipt-table-edit" id="receipt-lines">
                                <thead>
                                    <tr>
                                        <th scope="col" class="receipt-col-product">SKU / Sản phẩm</th>
                                        <th scope="col" class="receipt-col-unit">Đơn vị</th>
                                        <th scope="col" class="receipt-col-qty">SL nhập</th>
                                        <th scope="col" class="receipt-col-factor">Hệ số</th>
                                        <th scope="col" class="receipt-col-base">SL cơ sở</th>
                                        <th scope="col" class="receipt-col-price">Đơn giá nhập</th>
                                        <th scope="col" class="receipt-col-amount">Thành tiền</th>
                                        <th scope="col" class="receipt-col-location">Vị trí</th>
                                        <th scope="col" class="receipt-col-action">Thao tác</th>
                                    </tr>
                                </thead>
                                <c:forEach var="line" items="${form.lines}">
                                    <c:set var="product" value="${productsById[line.productId]}"/>
                                    <tbody class="receipt-line">
                                        <tr class="receipt-line__main">
                                            <td class="receipt-col-product">
                                                <input type="hidden" class="receipt-line__product-id" name="productId" value="<c:out value='${line.productId}'/>">
                                                <input class="inv-control receipt-line__control receipt-line__product${not empty line.errors.productId ? ' is-invalid' : ''}"
                                                       type="text" name="productText" list="receipt-products" autocomplete="off"
                                                       value="<c:out value='${line.productText}'/>" placeholder="Chọn SKU" aria-label="Sản phẩm">
                                                <span class="receipt-line__sku">${empty product ? 'Chưa chọn sản phẩm' : 'SKU: '.concat(product.sku)}</span>
                                                <c:if test="${not empty line.errors.productId}"><span class="receipt-line__error"><c:out value="${line.errors.productId}"/></span></c:if>
                                            </td>
                                            <td class="receipt-col-unit">
                                                <select class="inv-control receipt-line__control receipt-line__unit${not empty line.errors.unitId ? ' is-invalid' : ''}" name="unitId" aria-label="Đơn vị">
                                                    <c:if test="${empty product}"><option value="">—</option></c:if>
                                                    <c:forEach var="unit" items="${product.units}">
                                                        <option value="${unit.id}" data-factor="${unit.factorText}"${unit.id.toString() == line.unitId ? ' selected' : ''}><c:out value="${unit.name}"/></option>
                                                    </c:forEach>
                                                </select>
                                                <c:if test="${not empty line.errors.unitId}"><span class="receipt-line__error"><c:out value="${line.errors.unitId}"/></span></c:if>
                                            </td>
                                            <td class="receipt-col-qty">
                                                <input class="inv-control receipt-line__control receipt-line__qty${not empty line.errors.qty ? ' is-invalid' : ''}" type="text"
                                                       name="qty" inputmode="decimal" value="<c:out value='${line.qty}'/>" placeholder="0" aria-label="Số lượng nhập">
                                                <c:if test="${not empty line.errors.qty}"><span class="receipt-line__error"><c:out value="${line.errors.qty}"/></span></c:if>
                                            </td>
                                            <td class="receipt-col-factor">
                                                <input class="inv-control receipt-line__control receipt-line__factor" type="text" value="" readonly tabindex="-1" aria-label="Hệ số quy đổi">
                                            </td>
                                            <td class="receipt-col-base">
                                                <span class="receipt-line__base">—</span>
                                                <span class="receipt-line__base-label">SL cơ sở</span>
                                            </td>
                                            <td class="receipt-col-price">
                                                <input class="inv-control receipt-line__control receipt-line__price${not empty line.errors.unitPrice ? ' is-invalid' : ''}" type="text"
                                                       name="unitPrice" inputmode="numeric" value="<c:out value='${line.unitPrice}'/>" placeholder="0 đ" aria-label="Đơn giá nhập">
                                                <c:if test="${not empty line.errors.unitPrice}"><span class="receipt-line__error"><c:out value="${line.errors.unitPrice}"/></span></c:if>
                                            </td>
                                            <td class="receipt-col-amount receipt-line__amount">—</td>
                                            <td class="receipt-col-location">
                                                <select class="inv-control receipt-line__control receipt-line__location${not empty line.errors.locationId ? ' is-invalid' : ''}" name="locationId" aria-label="Vị trí">
                                                    <option value="">Chưa xếp</option>
                                                    <c:forEach var="location" items="${lineLocations}">
                                                        <option value="${location.id}"${location.id.toString() == line.locationId ? ' selected' : ''}><c:out value="${location.code}"/></option>
                                                    </c:forEach>
                                                </select>
                                                <c:if test="${not empty line.errors.locationId}"><span class="receipt-line__error"><c:out value="${line.errors.locationId}"/></span></c:if>
                                            </td>
                                            <td class="receipt-col-action">
                                                <button class="inv-icon-button inv-icon-button--danger receipt-line__remove" type="button" title="Xoá dòng" aria-label="Xoá dòng hàng">
                                                    <span class="inv-icon inv-icon--x" aria-hidden="true"></span>
                                                </button>
                                            </td>
                                        </tr>
                                        <tr class="receipt-line__lot"${product.lotTracked ? '' : ' hidden'}>
                                            <td colspan="9">
                                                <div class="receipt-lot">
                                                    <span class="receipt-lot__badge">Quản lý lô</span>
                                                    <label class="receipt-lot__field">
                                                        <span>Số lô<span class="inv-field__required">*</span></span>
                                                        <input class="inv-control receipt-line__control${not empty line.errors.lotNo ? ' is-invalid' : ''}" type="text"
                                                               name="lotNo" maxlength="50" value="<c:out value='${line.lotNo}'/>" placeholder="VD: L2310A">
                                                    </label>
                                                    <label class="receipt-lot__field">
                                                        <span>Hạn sử dụng</span>
                                                        <input class="inv-control receipt-line__control${not empty line.errors.expiryDate ? ' is-invalid' : ''}" type="date"
                                                               name="expiryDate" value="<c:out value='${line.expiryDate}'/>">
                                                    </label>
                                                    <c:if test="${not empty line.errors.lotNo or not empty line.errors.expiryDate}">
                                                        <span class="receipt-line__error"><c:out value="${line.errors.lotNo}${line.errors.expiryDate}"/></span>
                                                    </c:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </tbody>
                                </c:forEach>
                            </table>
                        </div>

                        <div class="receipt-bottom">
                            <dl class="receipt-summary" id="receipt-summary">
                                <div class="receipt-summary__row">
                                    <dt>Số dòng hàng</dt>
                                    <dd id="summary-lines">0</dd>
                                </div>
                                <div class="receipt-summary__row">
                                    <dt>Tổng số lượng cơ sở</dt>
                                    <dd id="summary-base">0</dd>
                                </div>
                                <div class="receipt-summary__row receipt-summary__row--total">
                                    <dt>Tổng tiền nhập</dt>
                                    <dd id="summary-total">0 đ</dd>
                                </div>
                            </dl>
                        </div>

                        <div class="inv-note receipt-conversion">
                            <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                            <p>Quy đổi đơn vị: Số lượng cơ sở = Số lượng nhập × Hệ số quy đổi. Ví dụ: 10 thùng × 24 chai = 240 chai.</p>
                        </div>
                    </div>
                </section>

                <div class="receipt-actionbar">
                    <div class="receipt-actionbar__status">
                        <span class="inv-pill inv-pill--orange">Nháp</span>
                        <span>Phiếu chưa được ghi sổ.</span>
                    </div>
                    <div class="receipt-actionbar__buttons">
                        <a class="inv-button inv-button--outline" id="receipt-cancel" href="<c:url value='/goods-receipts'/>">Hủy</a>
                        <button class="inv-button inv-button--outline" type="submit" name="action" value="draft" id="receipt-save-draft">Lưu nháp</button>
                        <button class="inv-button inv-button--primary" type="submit" name="action" value="post" id="receipt-post"
                                data-confirm="Ghi sổ phiếu nhập? Sau khi ghi sổ, tồn kho được cộng và phiếu không sửa được nữa.">Ghi sổ</button>
                    </div>
                </div>
            </form>

            <%-- Gợi ý khi gõ SKU/tên hàng; goods-receipts.js dò productId theo đúng chữ của gợi ý --%>
            <datalist id="receipt-products">
                <c:forEach var="option" items="${options.products}">
                    <option value="<c:out value='${option.label}'/>" data-id="${option.id}"></option>
                </c:forEach>
            </datalist>
            <%-- Đơn vị quy đổi của từng sản phẩm và vị trí của từng kho, để đổi ô chọn trên dòng hàng không cần tải lại --%>
            <div hidden id="receipt-data">
                <c:forEach var="option" items="${options.products}">
                    <select data-product="${option.id}" data-sku="<c:out value='${option.sku}'/>" data-lot="${option.lotTracked}" aria-hidden="true" tabindex="-1">
                        <c:forEach var="unit" items="${option.units}">
                            <option value="${unit.id}" data-factor="${unit.factorText}"${unit.base ? ' selected' : ''}><c:out value="${unit.name}"/></option>
                        </c:forEach>
                    </select>
                </c:forEach>
                <c:forEach var="entry" items="${locationsByWarehouse}">
                    <select data-warehouse="${entry.key}" aria-hidden="true" tabindex="-1">
                        <c:forEach var="location" items="${entry.value}">
                            <option value="${location.id}" title="<c:out value='${location.name}'/>"><c:out value="${location.code}"/></option>
                        </c:forEach>
                    </select>
                </c:forEach>
            </div>
            <template id="receipt-line-template">
                <tbody class="receipt-line">
                    <tr class="receipt-line__main">
                        <td class="receipt-col-product">
                            <input type="hidden" class="receipt-line__product-id" name="productId" value="">
                            <input class="inv-control receipt-line__control receipt-line__product" type="text" name="productText" list="receipt-products"
                                   autocomplete="off" value="" placeholder="Chọn SKU" aria-label="Sản phẩm">
                            <span class="receipt-line__sku">Chưa chọn sản phẩm</span>
                        </td>
                        <td class="receipt-col-unit">
                            <select class="inv-control receipt-line__control receipt-line__unit" name="unitId" aria-label="Đơn vị"><option value="">—</option></select>
                        </td>
                        <td class="receipt-col-qty">
                            <input class="inv-control receipt-line__control receipt-line__qty" type="text" name="qty" inputmode="decimal" value="" placeholder="0" aria-label="Số lượng nhập">
                        </td>
                        <td class="receipt-col-factor">
                            <input class="inv-control receipt-line__control receipt-line__factor" type="text" value="" readonly tabindex="-1" aria-label="Hệ số quy đổi">
                        </td>
                        <td class="receipt-col-base">
                            <span class="receipt-line__base">—</span>
                            <span class="receipt-line__base-label">SL cơ sở</span>
                        </td>
                        <td class="receipt-col-price">
                            <input class="inv-control receipt-line__control receipt-line__price" type="text" name="unitPrice" inputmode="numeric" value="" placeholder="0 đ" aria-label="Đơn giá nhập">
                        </td>
                        <td class="receipt-col-amount receipt-line__amount">—</td>
                        <td class="receipt-col-location">
                            <select class="inv-control receipt-line__control receipt-line__location" name="locationId" aria-label="Vị trí"><option value="">Chưa xếp</option></select>
                        </td>
                        <td class="receipt-col-action">
                            <button class="inv-icon-button inv-icon-button--danger receipt-line__remove" type="button" title="Xoá dòng" aria-label="Xoá dòng hàng">
                                <span class="inv-icon inv-icon--x" aria-hidden="true"></span>
                            </button>
                        </td>
                    </tr>
                    <tr class="receipt-line__lot" hidden>
                        <td colspan="9">
                            <div class="receipt-lot">
                                <span class="receipt-lot__badge">Quản lý lô</span>
                                <label class="receipt-lot__field">
                                    <span>Số lô<span class="inv-field__required">*</span></span>
                                    <input class="inv-control receipt-line__control" type="text" name="lotNo" maxlength="50" value="" placeholder="VD: L2310A">
                                </label>
                                <label class="receipt-lot__field">
                                    <span>Hạn sử dụng</span>
                                    <input class="inv-control receipt-line__control" type="date" name="expiryDate" value="">
                                </label>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </template>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/goods-receipts.js'/>"></script>
</body>
</html>
