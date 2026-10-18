<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="warehouses"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbParent" value="Danh sách kho"/>
<c:set var="breadcrumbParentUrl" value="/warehouses"/>
<c:set var="breadcrumbPage" value="${empty editing ? 'Thêm kho mới' : 'Sửa kho'}"/>
<c:choose>
    <c:when test="${empty editing}"><c:url var="formAction" value="/warehouses/new"/></c:when>
    <c:otherwise><c:url var="formAction" value="/warehouses/edit"><c:param name="id" value="${editing.id}"/></c:url></c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${empty editing ? 'Thêm kho mới' : 'Sửa kho'} | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">${empty editing ? 'Thêm kho mới' : 'Sửa kho'}</h1>
                    <p class="inv-header__subtitle">
                        <c:choose>
                            <c:when test="${empty editing}">Nhập thông tin cơ bản để tạo kho trong hệ thống quản lý bán hàng.</c:when>
                            <c:otherwise>Cập nhật thông tin kho <c:out value="${editing.code}"/> – <c:out value="${editing.name}"/>.</c:otherwise>
                        </c:choose>
                    </p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" id="warehouse-back" href="<c:url value='/warehouses'/>">
                        <span class="inv-icon inv-icon--chevron-left" aria-hidden="true"></span>Quay lại danh sách
                    </a>
                </div>
            </header>

            <div class="warehouse-form-layout">
                <form class="inv-card warehouse-form" id="warehouse-form" method="post" action="${fn:escapeXml(formAction)}" novalidate>
                    <div class="warehouse-form__intro">
                        <h2 class="inv-card__title">Thông tin kho</h2>
                        <p class="inv-card__subtitle">Các trường có dấu <span class="inv-field__required">*</span> là thông tin bắt buộc.</p>
                    </div>

                    <div class="warehouse-form__grid">
                        <div class="inv-field${not empty errors.code ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="warehouse-code">Mã kho<span class="inv-field__required">*</span></label>
                            <input class="inv-control" type="text" id="warehouse-code" name="code" maxlength="30" required
                                   value="<c:out value='${form.code}'/>" placeholder="VD: KHO005" autocomplete="off"
                                   aria-describedby="warehouse-code-hint${not empty errors.code ? ' warehouse-code-error' : ''}">
                            <p class="inv-field__hint" id="warehouse-code-hint">Mã kho dùng để nhận diện kho trong hệ thống.</p>
                            <c:if test="${not empty errors.code}"><p class="inv-field__error" id="warehouse-code-error"><c:out value="${errors.code}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.name ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="warehouse-name">Tên kho<span class="inv-field__required">*</span></label>
                            <input class="inv-control" type="text" id="warehouse-name" name="name" maxlength="150" required
                                   value="<c:out value='${form.name}'/>" placeholder="Nhập tên kho">
                            <c:if test="${not empty errors.name}"><p class="inv-field__error"><c:out value="${errors.name}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.managerUserId ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="warehouse-manager">Người phụ trách</label>
                            <select class="inv-control" id="warehouse-manager" name="managerUserId">
                                <option value="">Chưa phân công</option>
                                <c:forEach var="manager" items="${managers}">
                                    <option value="${manager.id}"${manager.id.toString() == form.managerUserId ? ' selected' : ''}><c:out value="${manager.name}"/></option>
                                </c:forEach>
                                <%-- Người phụ trách hiện tại không còn trong danh sách (đổi vai trò, bị khoá) vẫn giữ được khi lưu --%>
                                <c:if test="${not empty currentManager}">
                                    <option value="${currentManager.id}"${currentManager.id.toString() == form.managerUserId ? ' selected' : ''}><c:out value="${currentManager.name}"/></option>
                                </c:if>
                            </select>
                            <c:if test="${not empty errors.managerUserId}"><p class="inv-field__error"><c:out value="${errors.managerUserId}"/></p></c:if>
                        </div>
                        <div class="inv-field${not empty errors.description ? ' inv-field--invalid' : ''}">
                            <label class="inv-field__label" for="warehouse-description">Mô tả ngắn</label>
                            <input class="inv-control" type="text" id="warehouse-description" name="description" maxlength="200"
                                   value="<c:out value='${form.description}'/>" placeholder="VD: Kho chính, Kho khu vực">
                            <c:if test="${not empty errors.description}"><p class="inv-field__error"><c:out value="${errors.description}"/></p></c:if>
                        </div>
                    </div>

                    <div class="inv-field${not empty errors.address ? ' inv-field--invalid' : ''}">
                        <label class="inv-field__label" for="warehouse-address">Địa chỉ<span class="inv-field__required">*</span></label>
                        <textarea class="inv-control inv-control--textarea" id="warehouse-address" name="address" maxlength="300" required
                                  placeholder="Nhập địa chỉ kho"><c:out value="${form.address}"/></textarea>
                        <c:if test="${not empty errors.address}"><p class="inv-field__error"><c:out value="${errors.address}"/></p></c:if>
                    </div>

                    <fieldset class="inv-field warehouse-form__types${not empty errors.type ? ' inv-field--invalid' : ''}">
                        <legend class="inv-field__label">Loại kho<span class="inv-field__required">*</span></legend>
                        <div class="warehouse-type-options">
                            <c:forEach var="type" items="${typeLabels}">
                                <label class="warehouse-type-option">
                                    <input class="warehouse-type-option__input" type="radio" name="type" value="${type.key}"${type.key == form.type ? ' checked' : ''}>
                                    <span>${type.value}</span>
                                </label>
                            </c:forEach>
                        </div>
                        <p class="inv-field__hint">Chọn loại kho phù hợp với mục đích lưu trữ hàng hóa.</p>
                        <c:if test="${not empty errors.type}"><p class="inv-field__error"><c:out value="${errors.type}"/></p></c:if>
                    </fieldset>

                    <div class="inv-field${not empty errors.status ? ' inv-field--invalid' : ''}">
                        <label class="inv-field__label" for="warehouse-status">Trạng thái<span class="inv-field__required">*</span></label>
                        <select class="inv-control" id="warehouse-status" name="status">
                            <c:forEach var="status" items="${statusLabels}">
                                <option value="${status.key}"${status.key == form.status ? ' selected' : ''}>${status.value}</option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty errors.status}"><p class="inv-field__error"><c:out value="${errors.status}"/></p></c:if>
                    </div>

                    <div class="warehouse-form__actions">
                        <a class="inv-button" id="warehouse-cancel" href="<c:url value='/warehouses'/>">Hủy bỏ</a>
                        <button class="inv-button inv-button--primary" type="submit" id="warehouse-save">
                            <span class="inv-icon inv-icon--save" aria-hidden="true"></span>Lưu thông tin kho
                        </button>
                    </div>
                </form>

                <aside class="inv-card warehouse-guide" aria-labelledby="warehouse-guide-title">
                    <h2 class="warehouse-guide__title" id="warehouse-guide-title">Hướng dẫn nhập thông tin</h2>
                    <ul class="warehouse-guide__list">
                        <li class="warehouse-guide__item">
                            <span class="warehouse-guide__icon"><span class="inv-icon inv-icon--file-text" aria-hidden="true"></span></span>
                            <div>
                                <p class="warehouse-guide__name">Mã kho</p>
                                <p class="warehouse-guide__desc">Sử dụng mã dễ nhận biết, ví dụ KHO001. Mỗi kho nên có một mã riêng.</p>
                            </div>
                        </li>
                        <li class="warehouse-guide__item">
                            <span class="warehouse-guide__icon"><span class="inv-icon inv-icon--map-pin" aria-hidden="true"></span></span>
                            <div>
                                <p class="warehouse-guide__name">Địa chỉ kho</p>
                                <p class="warehouse-guide__desc">Ghi địa chỉ đủ rõ ràng để thuận tiện cho việc xác định địa điểm kho.</p>
                            </div>
                        </li>
                        <li class="warehouse-guide__item">
                            <span class="warehouse-guide__icon"><span class="inv-icon inv-icon--warehouse" aria-hidden="true"></span></span>
                            <div>
                                <p class="warehouse-guide__name">Loại kho</p>
                                <p class="warehouse-guide__desc">Phân biệt kho hàng bán được và kho lưu trữ hàng lỗi.</p>
                            </div>
                        </li>
                        <li class="warehouse-guide__item">
                            <span class="warehouse-guide__icon"><span class="inv-icon inv-icon--user" aria-hidden="true"></span></span>
                            <div>
                                <p class="warehouse-guide__name">Người phụ trách</p>
                                <p class="warehouse-guide__desc">Quản lý kho hoặc nhân viên kho chịu trách nhiệm chính về hàng trong kho.</p>
                            </div>
                        </li>
                    </ul>
                    <div class="inv-note warehouse-guide__note">
                        <span class="inv-icon inv-icon--info" aria-hidden="true"></span>
                        <p>Kiểm tra thông tin trước khi lưu để đảm bảo dữ liệu kho chính xác.</p>
                    </div>
                </aside>
            </div>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
