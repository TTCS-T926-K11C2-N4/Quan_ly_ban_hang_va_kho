<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Quản lý đại lý"/>
<c:set var="breadcrumbPage" value="${empty editing ? 'Thêm đại lý' : 'Sửa hồ sơ đại lý'}"/>
<c:choose>
    <c:when test="${empty editing}"><c:url var="cancelUrl" value="/customers"/><c:url var="actionUrl" value="/customers/new"/></c:when>
    <c:otherwise>
        <c:url var="cancelUrl" value="/customers/profile"><c:param name="id" value="${editing.id}"/></c:url>
        <c:url var="actionUrl" value="/customers/edit"><c:param name="id" value="${editing.id}"/></c:url>
    </c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${empty editing ? 'Thêm đại lý' : 'Sửa hồ sơ đại lý'} | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/customers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content sales-page">
            <header class="page-header">
                <h1 class="page-header__title">${empty editing ? 'Thêm đại lý' : 'Sửa hồ sơ đại lý'}</h1>
                <p class="page-header__subtitle">${empty editing ? 'Khai báo hồ sơ đại lý mới. Mã đại lý là duy nhất và không đổi được sau khi tạo.' : 'Cập nhật thông tin hồ sơ của đại lý.'}</p>
            </header>

            <form class="customer-form" id="customer-form" method="post" action="${actionUrl}" novalidate>
                <c:if test="${not empty form.version}"><input type="hidden" name="version" value="<c:out value='${form.version}'/>"></c:if>
                <c:if test="${not empty errors.form}">
                    <p class="sales-flash sales-flash--error" role="alert"><c:out value="${errors.form}"/></p>
                </c:if>

                <div class="profile-grid">
                    <section class="sales-card" aria-labelledby="form-info-title">
                        <h2 class="profile-card__title" id="form-info-title">Thông tin hồ sơ</h2>
                        <div class="customer-form__grid">
                            <div class="sales-field${not empty errors.code ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-code">Mã đại lý<span class="sales-field__required">*</span></label>
                                <input class="sales-field__control customer-form__code" type="text" id="customer-code" name="code" maxlength="30"
                                       value="<c:out value='${form.code}'/>"${empty editing ? ' required' : ' readonly'} aria-describedby="customer-code-hint">
                                <p class="${not empty errors.code ? 'sales-field__error' : 'sales-field__hint'}" id="customer-code-hint"><c:out value="${not empty errors.code ? errors.code : (empty editing ? 'Gợi ý sẵn mã kế tiếp, có thể sửa. Không đổi được sau khi tạo.' : 'Mã đại lý không đổi được.')}"/></p>
                            </div>
                            <div class="sales-field${not empty errors.name ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-name">Tên đại lý<span class="sales-field__required">*</span></label>
                                <input class="sales-field__control" type="text" id="customer-name" name="name" maxlength="200" required
                                       value="<c:out value='${form.name}'/>" placeholder="Ví dụ: Đại lý Phú Thịnh">
                                <c:if test="${not empty errors.name}"><p class="sales-field__error"><c:out value="${errors.name}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.taxCode ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-tax">Mã số thuế</label>
                                <input class="sales-field__control" type="text" id="customer-tax" name="taxCode" maxlength="14" inputmode="numeric"
                                       value="<c:out value='${form.taxCode}'/>" placeholder="10 số, 10-3 số hoặc 12 số">
                                <c:if test="${not empty errors.taxCode}"><p class="sales-field__error"><c:out value="${errors.taxCode}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.phone ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-phone">Số điện thoại</label>
                                <input class="sales-field__control" type="tel" id="customer-phone" name="phone" maxlength="20" inputmode="numeric"
                                       value="<c:out value='${form.phone}'/>" placeholder="10 chữ số">
                                <c:if test="${not empty errors.phone}"><p class="sales-field__error"><c:out value="${errors.phone}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.email ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-email">Email</label>
                                <input class="sales-field__control" type="email" id="customer-email" name="email" maxlength="150"
                                       value="<c:out value='${form.email}'/>" placeholder="daily@example.com">
                                <c:if test="${not empty errors.email}"><p class="sales-field__error"><c:out value="${errors.email}"/></p></c:if>
                            </div>
                            <div class="sales-field customer-form__wide${not empty errors.address ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-address">Địa chỉ trụ sở</label>
                                <input class="sales-field__control" type="text" id="customer-address" name="address" maxlength="300"
                                       value="<c:out value='${form.address}'/>" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh">
                                <c:if test="${not empty errors.address}"><p class="sales-field__error"><c:out value="${errors.address}"/></p></c:if>
                            </div>
                        </div>
                    </section>

                    <section class="sales-card" aria-labelledby="form-manage-title">
                        <h2 class="profile-card__title" id="form-manage-title">Thông tin quản lý</h2>
                        <div class="customer-form__grid">
                            <div class="sales-field customer-form__wide${not empty errors.customerGroupId ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-group">Nhóm khách hàng<span class="sales-field__required">*</span></label>
                                <select class="sales-field__control" id="customer-group" name="customerGroupId" required>
                                    <option value="">Chọn nhóm khách hàng</option>
                                    <c:forEach var="group" items="${groups}">
                                        <option value="${group.id}"${form.customerGroupId == group.id.toString() ? ' selected' : ''}><c:out value="${group.name}"/></option>
                                    </c:forEach>
                                </select>
                                <p class="${not empty errors.customerGroupId ? 'sales-field__error' : 'sales-field__hint'}"><c:out value="${not empty errors.customerGroupId ? errors.customerGroupId : 'Nhóm khách hàng quyết định bảng giá áp dụng khi tạo đơn hàng.'}"/></p>
                            </div>
                            <div class="sales-field${not empty errors.regionId ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-region">Khu vực<span class="sales-field__required">*</span></label>
                                <select class="sales-field__control" id="customer-region" name="regionId" required>
                                    <option value="">Chọn khu vực</option>
                                    <c:forEach var="region" items="${regions}">
                                        <option value="${region.id}"${form.regionId == region.id.toString() ? ' selected' : ''}><c:out value="${region.name}"/></option>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty errors.regionId}"><p class="sales-field__error"><c:out value="${errors.regionId}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.defaultWarehouseId ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-warehouse">Kho phục vụ mặc định<span class="sales-field__required">*</span></label>
                                <select class="sales-field__control" id="customer-warehouse" name="defaultWarehouseId" required>
                                    <option value="">Chọn kho</option>
                                    <c:forEach var="warehouse" items="${warehouses}">
                                        <option value="${warehouse.id}"${form.defaultWarehouseId == warehouse.id.toString() ? ' selected' : ''}><c:out value="${warehouse.name}"/></option>
                                    </c:forEach>
                                </select>
                                <c:if test="${not empty errors.defaultWarehouseId}"><p class="sales-field__error"><c:out value="${errors.defaultWarehouseId}"/></p></c:if>
                            </div>
                            <div class="sales-field${not empty errors.salesRepId ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-rep">Người phụ trách</label>
                                <c:choose>
                                    <c:when test="${canAssign}">
                                        <select class="sales-field__control" id="customer-rep" name="salesRepId">
                                            <option value="">Chưa phân công</option>
                                            <c:if test="${not empty editing.salesRepId}">
                                                <c:set var="currentRepActive" value="false"/>
                                                <c:forEach var="rep" items="${activeReps}"><c:if test="${rep.id == editing.salesRepId}"><c:set var="currentRepActive" value="true"/></c:if></c:forEach>
                                                <c:if test="${not currentRepActive}"><option value="${editing.salesRepId}"${form.salesRepId == editing.salesRepId.toString() ? ' selected' : ''}><c:out value="${editing.salesRepName}"/> (không còn hoạt động)</option></c:if>
                                            </c:if>
                                            <c:forEach var="rep" items="${activeReps}">
                                                <option value="${rep.id}"${form.salesRepId == rep.id.toString() ? ' selected' : ''}><c:out value="${rep.name}"/><c:if test="${not empty rep.code}"> — <c:out value="${rep.code}"/></c:if></option>
                                            </c:forEach>
                                        </select>
                                        <p class="${not empty errors.salesRepId ? 'sales-field__error' : 'sales-field__hint'}"><c:out value="${not empty errors.salesRepId ? errors.salesRepId : 'Đổi người phụ trách sẽ ghi lịch sử chuyển giao.'}"/></p>
                                    </c:when>
                                    <c:otherwise>
                                        <input class="sales-field__control" type="text" id="customer-rep" readonly
                                               value="<c:out value='${not empty editing ? (empty editing.salesRepName ? "Chưa phân công" : editing.salesRepName) : "Theo người tạo hoặc phân công sau"}'/>">
                                        <p class="sales-field__hint">Chỉ Quản lý kinh doanh và Admin đổi được người phụ trách (màn Phân công kinh doanh).</p>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="sales-field${not empty errors.status ? ' sales-field--invalid' : ''}">
                                <label class="sales-field__label" for="customer-status-select">Trạng thái<span class="sales-field__required">*</span></label>
                                <select class="sales-field__control" id="customer-status-select" name="status" required>
                                    <option value="ACTIVE"${form.status == 'ACTIVE' ? ' selected' : ''}>Đang hoạt động</option>
                                    <option value="INACTIVE"${form.status == 'INACTIVE' ? ' selected' : ''}>Ngừng giao dịch</option>
                                </select>
                                <c:if test="${not empty errors.status}"><p class="sales-field__error"><c:out value="${errors.status}"/></p></c:if>
                            </div>
                        </div>
                    </section>
                </div>

                <div class="sales-actions customer-form__actions">
                    <a class="sales-button" href="${cancelUrl}">Hủy</a>
                    <button class="sales-button sales-button--primary" type="submit" id="save-customer">${empty editing ? 'Thêm đại lý' : 'Lưu thay đổi'}</button>
                </div>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
