<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="goods-receipts"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Phiếu nhập kho"/>
<c:set var="breadcrumbParentUrl" value="/goods-receipts"/>
<c:set var="breadcrumbPage" value="${receipt.receiptNo}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu nhập <c:out value="${receipt.receiptNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Phiếu nhập kho <c:out value="${receipt.receiptNo}"/></h1>
                    <p class="inv-header__subtitle">Hàng nhập từ <c:out value="${receipt.supplierName}"/> vào <c:out value="${receipt.warehouseName}"/>.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="receipt-back" href="<c:url value='/goods-receipts'/>">
                        <span class="inv-icon inv-icon--arrow-left" aria-hidden="true"></span>Quay lại
                    </a>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="receipt-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${receipt.adjustment}">
                <c:url var="originalUrl" value="/goods-receipts/view"><c:param name="id" value="${receipt.adjustsReceiptId}"/></c:url>
                <div class="inv-note">
                    <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                    <p><strong>Phiếu điều chỉnh</strong> cho phiếu <a class="receipt-note__link" href="${fn:escapeXml(originalUrl)}"><c:out value="${receipt.adjustsReceiptNo}"/></a>;
                        số âm là giảm số lượng đã nhập.</p>
                </div>
            </c:if>

            <section class="inv-card" aria-labelledby="receipt-info-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="receipt-info-title">Thông tin phiếu nhập</h2>
                        <p class="inv-card__subtitle">Lập bởi <c:out value="${empty receipt.createdByName ? '—' : receipt.createdByName}"/> lúc ${receipt.createdAtText}.</p>
                    </div>
                    <span class="inv-pill ${receipt.confirmed ? 'inv-pill--success' : 'inv-pill--orange'}">${receipt.statusLabel}</span>
                </div>
                <dl class="inv-card__body receipt-info receipt-info--readonly">
                    <div>
                        <dt>Mã phiếu</dt>
                        <dd><c:out value="${receipt.receiptNo}"/></dd>
                    </div>
                    <div>
                        <dt>Ngày nhập</dt>
                        <dd>${receipt.receiptDateText}</dd>
                    </div>
                    <div>
                        <dt>Nhà cung cấp</dt>
                        <dd><c:out value="${receipt.supplierName}"/></dd>
                    </div>
                    <div>
                        <dt>Kho nhận</dt>
                        <dd><c:out value="${receipt.warehouseCode}"/> · <c:out value="${receipt.warehouseName}"/></dd>
                    </div>
                    <div>
                        <dt>Số chứng từ nhà cung cấp</dt>
                        <dd><c:out value="${empty receipt.documentRef ? '—' : receipt.documentRef}"/></dd>
                    </div>
                    <div>
                        <dt>Ghi chú</dt>
                        <dd><c:out value="${empty receipt.note ? '—' : receipt.note}"/></dd>
                    </div>
                </dl>
            </section>

            <section class="inv-card" aria-labelledby="receipt-lines-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="receipt-lines-title">Danh sách hàng nhập</h2>
                        <p class="inv-card__subtitle">Số lượng nhập và số lượng quy về đơn vị cơ sở của từng dòng hàng.</p>
                    </div>
                </div>
                <div class="inv-card__body receipt-lines">
                    <div class="inv-table-scroll">
                        <table class="receipt-table-edit receipt-table-view" id="receipt-lines">
                            <thead>
                                <tr>
                                    <th scope="col" class="receipt-col-product">SKU / Sản phẩm</th>
                                    <th scope="col" class="receipt-col-unit">Đơn vị</th>
                                    <th scope="col" class="receipt-col-qty is-number">SL nhập</th>
                                    <th scope="col" class="receipt-col-factor is-number">Hệ số</th>
                                    <th scope="col" class="receipt-col-base">SL cơ sở</th>
                                    <th scope="col" class="receipt-col-price is-number">Đơn giá nhập</th>
                                    <th scope="col" class="receipt-col-amount">Thành tiền</th>
                                    <th scope="col" class="receipt-col-location">Vị trí</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${receipt.items}">
                                    <tr>
                                        <td class="receipt-col-product">
                                            <span class="receipt-line__name"><c:out value="${item.productName}"/></span>
                                            <span class="receipt-line__sku"><c:out value="${item.sku}"/><c:if test="${not empty item.lotNo}"> · Lô <c:out value="${item.lotNo}"/><c:if test="${not empty item.expiryDate}"> · HSD ${item.expiryDateText}</c:if></c:if></span>
                                        </td>
                                        <td class="receipt-col-unit"><c:out value="${item.unitName}"/></td>
                                        <td class="receipt-col-qty is-number">${item.qtyText}</td>
                                        <td class="receipt-col-factor is-number">${item.unitFactorText}</td>
                                        <td class="receipt-col-base">
                                            <span class="receipt-line__base">${item.qtyBaseText}</span>
                                            <span class="receipt-line__base-label"><c:out value="${item.baseUnitName}"/></span>
                                        </td>
                                        <td class="receipt-col-price is-number"><fmt:formatNumber value="${item.unitPrice}" maxFractionDigits="0"/> đ</td>
                                        <td class="receipt-col-amount receipt-line__amount"><fmt:formatNumber value="${item.amount}" maxFractionDigits="0"/> đ</td>
                                        <td class="receipt-col-location"><c:out value="${empty item.locationCode ? 'Chưa xếp' : item.locationCode}"/></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>

                    <div class="receipt-bottom">
                        <dl class="receipt-summary">
                            <div class="receipt-summary__row">
                                <dt>Số dòng hàng</dt>
                                <dd>${fn:length(receipt.items)}</dd>
                            </div>
                            <div class="receipt-summary__row">
                                <dt>Tổng số lượng cơ sở</dt>
                                <dd><fmt:formatNumber value="${receipt.totalQtyBase}" maxFractionDigits="3"/></dd>
                            </div>
                            <div class="receipt-summary__row receipt-summary__row--total">
                                <dt>Tổng tiền nhập</dt>
                                <dd><fmt:formatNumber value="${receipt.totalAmount}" maxFractionDigits="0"/> đ</dd>
                            </div>
                        </dl>
                    </div>
                </div>
            </section>

            <div class="receipt-actionbar">
                <div class="receipt-actionbar__status">
                    <span class="inv-pill ${receipt.confirmed ? 'inv-pill--success' : 'inv-pill--orange'}">${receipt.statusLabel}</span>
                    <span>
                        <c:choose>
                            <c:when test="${receipt.confirmed}">Ghi sổ lúc ${receipt.confirmedAtText}<c:if test="${not empty receipt.confirmedByName}"> bởi <c:out value="${receipt.confirmedByName}"/></c:if>. Phiếu đã ghi sổ không sửa được.</c:when>
                            <c:otherwise>Phiếu chưa được ghi sổ, chưa ảnh hưởng tồn kho.</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="receipt-actionbar__buttons">
                    <a class="inv-button inv-button--outline" href="<c:url value='/goods-receipts'/>">Quay lại</a>
                    <c:if test="${canManage and receipt.confirmed}">
                        <c:url var="adjustUrl" value="/goods-receipts/new"><c:param name="adjusts" value="${receipt.id}"/></c:url>
                        <a class="inv-button inv-button--primary" id="receipt-adjust" href="${fn:escapeXml(adjustUrl)}">Lập phiếu điều chỉnh</a>
                    </c:if>
                    <c:if test="${canManage and receipt.draft}">
                        <c:url var="editUrl" value="/goods-receipts/edit"><c:param name="id" value="${receipt.id}"/></c:url>
                        <a class="inv-button inv-button--primary" id="receipt-edit" href="${fn:escapeXml(editUrl)}">Sửa phiếu nháp</a>
                    </c:if>
                </div>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
