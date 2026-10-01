<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeMenu" value="suppliers"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Nhà cung cấp"/>
<%-- Dùng chung cho Thêm và Sửa; SupplierFormServlet gán editing và form (Supplier, null khi mở form thêm) --%>
<c:set var="pageTitle" value="${editing ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'}"/>
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
    <link rel="stylesheet" href="<c:url value='/assets/css/suppliers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content accounts-page">
            <header class="page-header">
                <h1 class="page-header__title">${pageTitle}</h1>
                <p class="page-header__subtitle">Thông tin nhà cung cấp dùng khi lập phiếu nhập kho.</p>
            </header>

            <%-- novalidate: kiểm tra bằng supplier-form.js để hiện lỗi dưới từng ô; server luôn kiểm tra lại --%>
            <form class="account-form" id="supplier-form" method="post" novalidate
                  action="<c:url value='${editing ? "/suppliers/edit" : "/suppliers/new"}'/>">
                <c:if test="${editing}">
                    <input type="hidden" name="id" value="${form.id}">
                </c:if>
                <section class="account-form__section" aria-labelledby="supplier-info-title">
                    <h2 class="account-form__section-title" id="supplier-info-title">Thông tin nhà cung cấp</h2>
                    <p class="account-form__section-desc">Mã nhà cung cấp là duy nhất, tự chuyển thành chữ in hoa.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.code ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="code">Mã nhà cung cấp *</label>
                            <input class="form-group__control" type="text" id="code" name="code"
                                   value="<c:out value='${form.code}'/>" placeholder="Vd NCC-001"
                                   maxlength="30" autocomplete="off" required
                                   aria-describedby="code-error" aria-invalid="${not empty errors.code}">
                            <p class="form-group__error" id="code-error"${empty errors.code ? ' hidden' : ''}><c:out value="${errors.code}"/></p>
                        </div>

                        <div class="form-group${not empty errors.name ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="name">Tên nhà cung cấp *</label>
                            <input class="form-group__control" type="text" id="name" name="name"
                                   value="<c:out value='${form.name}'/>" placeholder="Nhập tên nhà cung cấp"
                                   maxlength="200" autocomplete="off" required
                                   aria-describedby="name-error" aria-invalid="${not empty errors.name}">
                            <p class="form-group__error" id="name-error"${empty errors.name ? ' hidden' : ''}><c:out value="${errors.name}"/></p>
                        </div>

                        <div class="form-group${not empty errors.taxCode ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="tax-code">Mã số thuế</label>
                            <input class="form-group__control" type="text" id="tax-code" name="taxCode"
                                   value="<c:out value='${form.taxCode}'/>" placeholder="Vd 0101234567"
                                   maxlength="14" inputmode="numeric" autocomplete="off"
                                   aria-describedby="tax-code-hint tax-code-error" aria-invalid="${not empty errors.taxCode}">
                            <p class="form-group__hint" id="tax-code-hint">10 số, hoặc 10 số kèm mã đơn vị phụ thuộc (0101234567-001).</p>
                            <p class="form-group__error" id="tax-code-error"${empty errors.taxCode ? ' hidden' : ''}><c:out value="${errors.taxCode}"/></p>
                        </div>

                        <div class="form-group supplier-form__wide${not empty errors.paymentTerms ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="payment-terms">Điều khoản thanh toán</label>
                            <textarea class="form-group__control supplier-form__textarea" id="payment-terms" name="paymentTerms"
                                      maxlength="200" placeholder="Vd thanh toán trong 30 ngày kể từ ngày nhận hàng"
                                      aria-describedby="payment-terms-error" aria-invalid="${not empty errors.paymentTerms}"><c:out value="${form.paymentTerms}"/></textarea>
                            <p class="form-group__error" id="payment-terms-error"${empty errors.paymentTerms ? ' hidden' : ''}><c:out value="${errors.paymentTerms}"/></p>
                        </div>
                    </div>
                    <c:if test="${editing}">
                        <p class="supplier-form__status">
                            Trạng thái: <strong>${form.active ? 'Đang giao dịch' : 'Ngừng giao dịch'}</strong>
                            (đổi ở danh sách nhà cung cấp)<c:if test="${form.hasReceipts}"> · Đã có phiếu nhập kho nên không xoá được</c:if>.
                        </p>
                    </c:if>
                </section>

                <section class="account-form__section account-form__section--divided" aria-labelledby="supplier-contact-title">
                    <h2 class="account-form__section-title" id="supplier-contact-title">Liên hệ</h2>
                    <p class="account-form__section-desc">Người liên hệ khi đặt hàng hoặc xử lý lô lỗi.</p>

                    <div class="account-form__grid">
                        <div class="form-group${not empty errors.contactName ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="contact-name">Người liên hệ</label>
                            <input class="form-group__control" type="text" id="contact-name" name="contactName"
                                   value="<c:out value='${form.contactName}'/>" placeholder="Nhập họ tên người liên hệ"
                                   maxlength="150" autocomplete="off"
                                   aria-describedby="contact-name-error" aria-invalid="${not empty errors.contactName}">
                            <p class="form-group__error" id="contact-name-error"${empty errors.contactName ? ' hidden' : ''}><c:out value="${errors.contactName}"/></p>
                        </div>

                        <div class="form-group${not empty errors.phone ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="phone">Số điện thoại</label>
                            <input class="form-group__control" type="tel" id="phone" name="phone"
                                   value="<c:out value='${form.phone}'/>" placeholder="Vd 024 3456 7890"
                                   maxlength="20" autocomplete="off"
                                   aria-describedby="phone-error" aria-invalid="${not empty errors.phone}">
                            <p class="form-group__error" id="phone-error"${empty errors.phone ? ' hidden' : ''}><c:out value="${errors.phone}"/></p>
                        </div>

                        <div class="form-group${not empty errors.email ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="email">Email</label>
                            <input class="form-group__control" type="email" id="email" name="email"
                                   value="<c:out value='${form.email}'/>" placeholder="lienhe@nhacungcap.vn"
                                   maxlength="150" autocomplete="off"
                                   aria-describedby="email-error" aria-invalid="${not empty errors.email}">
                            <p class="form-group__error" id="email-error"${empty errors.email ? ' hidden' : ''}><c:out value="${errors.email}"/></p>
                        </div>

                        <div class="form-group supplier-form__wide${not empty errors.address ? ' form-group--invalid' : ''}">
                            <label class="form-group__label" for="address">Địa chỉ</label>
                            <input class="form-group__control" type="text" id="address" name="address"
                                   value="<c:out value='${form.address}'/>" placeholder="Số nhà, đường, phường/xã, tỉnh/thành"
                                   maxlength="300" autocomplete="off"
                                   aria-describedby="address-error" aria-invalid="${not empty errors.address}">
                            <p class="form-group__error" id="address-error"${empty errors.address ? ' hidden' : ''}><c:out value="${errors.address}"/></p>
                        </div>
                    </div>
                </section>

                <footer class="account-form__footer">
                    <a class="button button--secondary" id="cancel-supplier-form" href="<c:url value='/suppliers'/>">Hủy</a>
                    <button class="button button--primary" type="submit" id="save-supplier-submit">${editing ? 'Lưu thay đổi' : 'Thêm nhà cung cấp'}</button>
                </footer>
            </form>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/supplier-form.js'/>"></script>
</body>
</html>
