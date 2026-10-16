<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="warehouses"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Danh sách kho"/>
<c:set var="breadcrumbParentUrl" value="/warehouses"/>
<c:set var="breadcrumbPage" value="${warehouse.code}"/>
<c:url var="selfUrl" value="/warehouses/locations"><c:param name="id" value="${warehouse.id}"/></c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Vị trí kho <c:out value="${warehouse.code}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/warehouses.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Vị trí lưu trong kho</h1>
                    <p class="inv-header__subtitle">Khai báo khu và kệ của <c:out value="${warehouse.name}"/> để biết chính xác hàng nằm ở kệ nào khi soạn đơn.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="location-back" href="<c:url value='/warehouses'/>">
                        <span class="inv-icon inv-icon--chevron-left" aria-hidden="true"></span>Quay lại danh sách
                    </a>
                    <c:if test="${canManage}">
                        <c:url var="editWarehouseUrl" value="/warehouses/edit"><c:param name="id" value="${warehouse.id}"/></c:url>
                        <a class="inv-button inv-button--primary" id="location-edit-warehouse" href="${fn:escapeXml(editWarehouseUrl)}">
                            <span class="inv-icon inv-icon--pencil" aria-hidden="true"></span>Sửa thông tin kho
                        </a>
                    </c:if>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="location-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="location-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <section class="inv-card" aria-labelledby="warehouse-info-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="warehouse-info-title">Thông tin kho</h2>
                        <p class="inv-card__subtitle">Mã kho <c:out value="${warehouse.code}"/> · <c:out value="${warehouse.name}"/></p>
                    </div>
                    <span class="inv-pill inv-pill--dot ${warehouse.active ? 'inv-pill--success' : 'inv-pill--muted'}">${warehouse.statusLabel}</span>
                </div>
                <dl class="warehouse-info">
                    <div>
                        <dt class="warehouse-info__label">Tên kho</dt>
                        <dd class="warehouse-info__value"><c:out value="${warehouse.name}"/></dd>
                        <dd class="warehouse-info__note"><c:out value="${empty warehouse.description ? '—' : warehouse.description}"/></dd>
                    </div>
                    <div>
                        <dt class="warehouse-info__label">Địa chỉ</dt>
                        <dd class="warehouse-info__value"><c:out value="${warehouse.address}"/></dd>
                    </div>
                    <div>
                        <dt class="warehouse-info__label">Người phụ trách</dt>
                        <dd class="warehouse-info__value"><c:out value="${empty warehouse.managerName ? 'Chưa phân công' : warehouse.managerName}"/></dd>
                    </div>
                    <div>
                        <dt class="warehouse-info__label">Loại kho</dt>
                        <dd class="warehouse-info__value">
                            <span class="inv-pill ${warehouse.defect ? 'inv-pill--amber' : 'inv-pill--blue'}">${warehouse.typeLabel}</span>
                        </dd>
                    </div>
                </dl>
            </section>

            <div class="location-layout${canManage ? '' : ' location-layout--single'}">
                <section class="inv-card" aria-labelledby="location-tree-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="location-tree-title">Cây vị trí trong kho</h2>
                            <p class="inv-card__subtitle">Khu và kệ theo thứ tự đi soạn hàng.</p>
                        </div>
                        <p class="inv-card__meta" id="location-count"><strong>${zoneCount}</strong> khu · <strong>${rackCount}</strong> kệ</p>
                    </div>
                    <c:choose>
                        <c:when test="${empty tree}">
                            <p class="location-empty">Kho chưa khai báo vị trí nào.<c:if test="${canManage}"> Thêm khu (vd A) rồi thêm các kệ trong khu (vd A-01).</c:if></p>
                        </c:when>
                        <c:otherwise>
                            <ul class="location-tree" id="location-tree">
                                <c:forEach var="node" items="${tree}">
                                    <li class="location-tree__item">
                                        <c:set var="location" value="${node}"/>
                                        <%@ include file="/WEB-INF/views/warehouses/location-node.jspf" %>
                                        <c:if test="${node.zone and not empty node.children}">
                                            <ul class="location-tree__children">
                                                <c:forEach var="child" items="${node.children}">
                                                    <li class="location-tree__item">
                                                        <c:set var="location" value="${child}"/>
                                                        <%@ include file="/WEB-INF/views/warehouses/location-node.jspf" %>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                        </c:if>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </section>

                <c:if test="${canManage}">
                    <form class="inv-card location-form" id="location-form" method="post" action="<c:url value='/warehouses/locations/save'/>" novalidate>
                        <input type="hidden" name="warehouseId" value="${warehouse.id}">
                        <c:if test="${form.editing}"><input type="hidden" name="locationId" value="<c:out value='${form.locationId}'/>"></c:if>
                        <div class="inv-card__head">
                            <div>
                                <h2 class="inv-card__title" id="location-form-title">${form.editing ? 'Sửa vị trí' : 'Thêm vị trí'}</h2>
                                <p class="inv-card__subtitle">Các trường có dấu <span class="inv-field__required">*</span> là thông tin bắt buộc.</p>
                            </div>
                        </div>
                        <div class="inv-card__body location-form__body">
                            <fieldset class="inv-field${not empty errors.type ? ' inv-field--invalid' : ''}">
                                <legend class="inv-field__label">Loại vị trí<span class="inv-field__required">*</span></legend>
                                <div class="warehouse-type-options">
                                    <c:forEach var="type" items="${typeLabels}">
                                        <label class="warehouse-type-option">
                                            <input class="warehouse-type-option__input location-form__type" type="radio" name="type"
                                                   value="${type.key}"${type.key == form.type ? ' checked' : ''}>
                                            <span>${type.value}</span>
                                        </label>
                                    </c:forEach>
                                </div>
                                <c:if test="${not empty errors.type}"><p class="inv-field__error"><c:out value="${errors.type}"/></p></c:if>
                            </fieldset>

                            <div class="inv-field location-form__parent${not empty errors.parentId ? ' inv-field--invalid' : ''}" id="location-parent-field">
                                <label class="inv-field__label" for="location-parent">Khu chứa kệ</label>
                                <select class="inv-control" id="location-parent" name="parentId">
                                    <option value="">Kệ đứng riêng (không thuộc khu)</option>
                                    <c:forEach var="zone" items="${zones}">
                                        <c:if test="${zone.id.toString() != form.locationId}">
                                            <option value="${zone.id}"${zone.id.toString() == form.parentId ? ' selected' : ''}><c:out value="${zone.label}"/></option>
                                        </c:if>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty errors.parentId}"><p class="inv-field__error"><c:out value="${errors.parentId}"/></p></c:if>
                            </div>

                            <div class="inv-field${not empty errors.code ? ' inv-field--invalid' : ''}">
                                <label class="inv-field__label" for="location-code">Mã vị trí<span class="inv-field__required">*</span></label>
                                <input class="inv-control" type="text" id="location-code" name="code" maxlength="30" required
                                       value="<c:out value='${form.code}'/>" placeholder="VD: A hoặc A-01" autocomplete="off">
                                <p class="inv-field__hint">Mã kệ nên bắt đầu bằng mã khu, ví dụ kệ A-01 thuộc khu A.</p>
                                <c:if test="${not empty errors.code}"><p class="inv-field__error"><c:out value="${errors.code}"/></p></c:if>
                            </div>

                            <div class="inv-field${not empty errors.name ? ' inv-field--invalid' : ''}">
                                <label class="inv-field__label" for="location-name">Tên vị trí</label>
                                <input class="inv-control" type="text" id="location-name" name="name" maxlength="100"
                                       value="<c:out value='${form.name}'/>" placeholder="VD: Khu đồ uống, Kệ bia lon">
                                <c:if test="${not empty errors.name}"><p class="inv-field__error"><c:out value="${errors.name}"/></p></c:if>
                            </div>

                            <div class="inv-field${not empty errors.pickSequence ? ' inv-field--invalid' : ''}">
                                <label class="inv-field__label" for="location-sequence">Thứ tự đi soạn hàng</label>
                                <input class="inv-control" type="number" id="location-sequence" name="pickSequence" min="0" max="99999"
                                       value="<c:out value='${form.pickSequence}'/>" inputmode="numeric">
                                <p class="inv-field__hint">Số nhỏ đi trước; dùng để xếp đường đi khi soạn hàng.</p>
                                <c:if test="${not empty errors.pickSequence}"><p class="inv-field__error"><c:out value="${errors.pickSequence}"/></p></c:if>
                            </div>

                            <div class="location-form__actions">
                                <a class="inv-button" id="location-cancel" href="${fn:escapeXml(selfUrl)}">${form.editing ? 'Hủy sửa' : 'Làm lại'}</a>
                                <button class="inv-button inv-button--primary" type="submit" id="location-save">
                                    <span class="inv-icon inv-icon--save" aria-hidden="true"></span>Lưu vị trí
                                </button>
                            </div>
                        </div>
                    </form>
                </c:if>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/warehouses.js'/>"></script>
</body>
</html>
