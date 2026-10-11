<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeMenu" value="accounts"/>
<c:set var="activeSubmenu" value="customers"/>
<c:set var="breadcrumbSection" value="Đại lý"/>
<c:set var="breadcrumbPage" value="Điểm giao hàng"/>
<c:set var="customerTab" value="delivery"/>
<c:url var="pageUrl" value="/customers/delivery-addresses"><c:param name="id" value="${customer.id}"/></c:url>
<c:set var="activeCount" value="0"/>
<c:set var="inactiveCount" value="0"/>
<c:forEach var="address" items="${addresses}">
    <c:choose>
        <c:when test="${address.active}"><c:set var="activeCount" value="${activeCount + 1}"/></c:when>
        <c:otherwise><c:set var="inactiveCount" value="${inactiveCount + 1}"/></c:otherwise>
    </c:choose>
</c:forEach>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Điểm giao hàng | Hệ thống quản lý bán hàng &amp; kho</title>
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
                <h1 class="page-header__title">Chi tiết đại lý</h1>
                <p class="page-header__subtitle">Điểm giao hàng của đại lý, dùng để chọn nơi giao khi tạo đơn hàng.</p>
            </header>

            <%@ include file="/WEB-INF/views/customers/customer-tabs.jspf" %>

            <c:if test="${not empty flashMessage}">
                <div class="sales-flash" id="delivery-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>

            <section class="sales-card" aria-labelledby="delivery-title">
                <div class="customer-head">
                    <div>
                        <h2 class="customer-head__name"><c:out value="${customer.name}"/></h2>
                        <p class="customer-head__code">Mã đại lý: <c:out value="${customer.code}"/></p>
                    </div>
                </div>

                <section class="sales-section delivery-section" aria-labelledby="delivery-title">
                    <div class="section-head">
                        <div>
                            <h3 class="section-head__title" id="delivery-title">Điểm giao hàng</h3>
                            <p class="section-head__desc">Thêm, sửa hoặc xoá điểm giao hàng của đại lý, gồm địa chỉ, người nhận, số điện thoại và ghi chú đường đi.</p>
                        </div>
                        <c:if test="${canManage and empty form}">
                            <a class="sales-button sales-button--soft" id="add-delivery-address" href="${fn:escapeXml(pageUrl)}&amp;edit=new#delivery-form">
                                <img class="sales-button__icon" src="<c:url value='/assets/img/icons/plus.svg'/>" alt="" width="16" height="16">
                                Thêm điểm giao
                            </a>
                        </c:if>
                    </div>

                    <div class="delivery-list" id="delivery-list">
                        <c:if test="${not empty form and empty form.id}">
                            <c:set var="editingIsDefault" value="${false}"/>
                            <%@ include file="/WEB-INF/views/customers/delivery-address-form.jspf" %>
                        </c:if>

                        <c:forEach var="address" items="${addresses}">
                            <c:if test="${address.active}">
                                <c:choose>
                                    <c:when test="${not empty form and form.id == address.id}">
                                        <c:set var="editingIsDefault" value="${address.defaultAddress}"/>
                                        <%@ include file="/WEB-INF/views/customers/delivery-address-form.jspf" %>
                                    </c:when>
                                    <c:otherwise>
                                        <article class="delivery-card${address.defaultAddress ? ' delivery-card--default' : ''}" id="delivery-${address.id}">
                                            <div class="delivery-card__head">
                                                <div class="delivery-card__tags">
                                                    <c:choose>
                                                        <c:when test="${address.defaultAddress}">
                                                            <span class="delivery-card__badge">Điểm mặc định</span>
                                                        </c:when>
                                                        <c:when test="${canManage}">
                                                            <form method="post" action="<c:url value='/customers/delivery-addresses/action'/>">
                                                                <input type="hidden" name="id" value="${customer.id}">
                                                                <input type="hidden" name="addressId" value="${address.id}">
                                                                <button class="delivery-card__make-default" type="submit" name="action" value="default">Đặt làm mặc định</button>
                                                            </form>
                                                        </c:when>
                                                    </c:choose>
                                                    <c:if test="${not empty address.label}">
                                                        <span class="delivery-card__label"><c:out value="${address.label}"/></span>
                                                    </c:if>
                                                </div>
                                                <c:if test="${canManage}">
                                                    <div class="delivery-card__actions">
                                                        <a class="delivery-card__icon-button delivery-card__icon-button--edit" href="${fn:escapeXml(pageUrl)}&amp;edit=${address.id}#delivery-form"
                                                           aria-label="Sửa điểm giao ${fn:escapeXml(address.address)}" title="Sửa">
                                                            <img src="<c:url value='/assets/img/icons/pencil.svg'/>" alt="" width="16" height="16">
                                                        </a>
                                                        <form method="post" action="<c:url value='/customers/delivery-addresses/action'/>"
                                                              data-confirm="${address.used ? 'Điểm giao này đã có đơn hàng nên không xoá được, sẽ chuyển sang ngừng dùng. Tiếp tục?' : 'Xoá điểm giao này?'}">
                                                            <input type="hidden" name="id" value="${customer.id}">
                                                            <input type="hidden" name="addressId" value="${address.id}">
                                                            <button class="delivery-card__icon-button delivery-card__icon-button--delete" type="submit" name="action" value="remove"
                                                                    aria-label="${address.used ? 'Ngừng dùng' : 'Xoá'} điểm giao ${fn:escapeXml(address.address)}" title="${address.used ? 'Ngừng dùng' : 'Xoá'}">
                                                                <img src="<c:url value='/assets/img/icons/trash.svg'/>" alt="" width="16" height="16">
                                                            </button>
                                                        </form>
                                                    </div>
                                                </c:if>
                                            </div>
                                            <dl class="delivery-card__grid">
                                                <div>
                                                    <dt class="delivery-card__term">Địa chỉ</dt>
                                                    <dd class="delivery-card__value"><c:out value="${address.address}"/></dd>
                                                </div>
                                                <div>
                                                    <dt class="delivery-card__term">Người nhận</dt>
                                                    <dd class="delivery-card__value"><c:out value="${empty address.receiverName ? '—' : address.receiverName}"/></dd>
                                                </div>
                                                <div>
                                                    <dt class="delivery-card__term">Số điện thoại</dt>
                                                    <dd class="delivery-card__value">
                                                        <c:choose>
                                                            <c:when test="${empty address.receiverPhone}">—</c:when>
                                                            <c:otherwise><a class="delivery-card__phone" href="tel:${fn:escapeXml(address.receiverPhone)}"><c:out value="${address.receiverPhone}"/></a></c:otherwise>
                                                        </c:choose>
                                                    </dd>
                                                </div>
                                                <div>
                                                    <dt class="delivery-card__term">Ghi chú đường đi</dt>
                                                    <dd class="delivery-card__value"><c:out value="${empty address.routeNote ? '—' : address.routeNote}"/></dd>
                                                </div>
                                            </dl>
                                        </article>
                                    </c:otherwise>
                                </c:choose>
                            </c:if>
                        </c:forEach>

                        <c:if test="${activeCount == 0 and (empty form or not empty form.id)}">
                            <p class="sales-empty" id="delivery-empty">
                                Đại lý chưa có điểm giao nào đang dùng nên chưa tạo được đơn hàng.<c:if test="${canManage}"> Bấm "Thêm điểm giao" để khai báo.</c:if>
                            </p>
                        </c:if>
                    </div>

                    <c:if test="${inactiveCount > 0}">
                        <details class="delivery-inactive" id="delivery-inactive">
                            <summary class="delivery-inactive__toggle">Đã ngừng dùng (${inactiveCount})</summary>
                            <ul class="delivery-inactive__list">
                                <c:forEach var="address" items="${addresses}">
                                    <c:if test="${not address.active}">
                                        <li class="delivery-inactive__item">
                                            <div>
                                                <p class="delivery-inactive__address"><c:if test="${not empty address.label}"><strong><c:out value="${address.label}"/></strong> · </c:if><c:out value="${address.address}"/></p>
                                                <p class="delivery-inactive__meta"><c:out value="${address.receiverName}"/><c:if test="${not empty address.receiverPhone}"> · <c:out value="${address.receiverPhone}"/></c:if></p>
                                            </div>
                                            <c:if test="${canManage}">
                                                <div class="delivery-inactive__actions">
                                                    <form method="post" action="<c:url value='/customers/delivery-addresses/action'/>">
                                                        <input type="hidden" name="id" value="${customer.id}">
                                                        <input type="hidden" name="addressId" value="${address.id}">
                                                        <button class="sales-button sales-button--small" type="submit" name="action" value="activate">Dùng lại</button>
                                                    </form>
                                                    <c:if test="${not address.used}">
                                                        <form method="post" action="<c:url value='/customers/delivery-addresses/action'/>" data-confirm="Xoá hẳn điểm giao này?">
                                                            <input type="hidden" name="id" value="${customer.id}">
                                                            <input type="hidden" name="addressId" value="${address.id}">
                                                            <button class="sales-button sales-button--small delivery-inactive__delete" type="submit" name="action" value="remove">Xoá</button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </c:if>
                                        </li>
                                    </c:if>
                                </c:forEach>
                            </ul>
                        </details>
                    </c:if>

                    <p class="delivery-note">Chỉ một điểm giao mặc định, được chọn sẵn khi tạo đơn hàng mới cho đại lý.
                        <c:if test="${not canManage}"> Bạn chỉ có quyền xem điểm giao của đại lý này.</c:if></p>
                </section>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/customers.js'/>"></script>
</body>
</html>
