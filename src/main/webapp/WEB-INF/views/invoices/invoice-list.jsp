<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="invoices"/>
<c:set var="breadcrumbSection" value="Hoá đơn &amp; công nợ"/>
<c:set var="breadcrumbPage" value="Hoá đơn bán hàng"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hoá đơn bán hàng | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/invoices.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Hoá đơn bán hàng</h1>
                    <p class="inv-header__subtitle">Hoá đơn lập từ số lượng thực giao của phiếu xuất đã giao. Đã phát hành thì không sửa, không xoá; điều chỉnh bằng chứng từ điều chỉnh.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="invoice-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="invoice-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${canManage}">
                <section class="inv-card" aria-labelledby="invoice-candidates-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="invoice-candidates-title">Phiếu xuất đã giao, chờ lập hoá đơn</h2>
                            <p class="inv-card__subtitle">Tick các phiếu của cùng một đại lý để gộp thành một hoá đơn.</p>
                        </div>
                        <p class="inv-card__meta"><strong>${candidates.size()}</strong> phiếu</p>
                    </div>
                    <form id="invoice-issue-form" action="<c:url value='/invoices/issue'/>" method="post">
                        <div class="inv-table-scroll">
                            <table class="inv-table" id="invoice-candidates">
                                <thead>
                                    <tr>
                                        <th scope="col"><span class="visually-hidden">Chọn</span></th>
                                        <th scope="col">Đại lý</th>
                                        <th scope="col">Phiếu xuất</th>
                                        <th scope="col">Đơn hàng</th>
                                        <th scope="col">Giao lúc</th>
                                        <th scope="col" class="is-number">Giá trị thực giao</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="note" items="${candidates}">
                                        <tr>
                                            <td><input type="checkbox" name="note_id" value="${note.noteId}" data-customer="${note.customerId}" aria-label="Chọn phiếu ${note.deliveryNo}"></td>
                                            <td><span class="inv-table__main"><c:out value="${note.customerName}"/></span><span class="inv-table__sub"><c:out value="${note.customerCode}"/></span></td>
                                            <td><c:out value="${note.deliveryNo}"/></td>
                                            <td><c:out value="${note.orderNo}"/></td>
                                            <td><c:out value="${note.deliveredAtText}"/></td>
                                            <td class="is-number"><fmt:formatNumber value="${note.value}" maxFractionDigits="0"/> ₫</td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty candidates}">
                                        <tr><td class="inv-table__empty" colspan="6">Không có phiếu xuất nào đang chờ lập hoá đơn.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                        <c:if test="${not empty candidates}">
                            <div class="invoice-issue">
                                <input class="inv-control" type="text" name="note" maxlength="1000" placeholder="Ghi chú hoá đơn (tuỳ chọn)" aria-label="Ghi chú hoá đơn">
                                <button class="inv-button inv-button--primary" type="submit" id="invoice-issue">Phát hành hoá đơn</button>
                            </div>
                        </c:if>
                    </form>
                </section>
            </c:if>

            <c:if test="${canManage}">
                <form class="invoice-reminder" action="<c:url value='/invoices/reminders'/>" method="post">
                    <button class="inv-button" type="submit" id="invoice-reminders">Gửi nhắc hạn thanh toán ngay</button>
                    <span class="inv-card__subtitle">Hệ thống tự nhắc mỗi ngày lúc 08:00: trước hạn 3 ngày và khi quá hạn.</span>
                </form>
            </c:if>

            <section class="inv-card" aria-labelledby="invoice-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="invoice-table-title">Hoá đơn đã phát hành</h2>
                    <p class="inv-card__meta"><strong>${invoices.size()}</strong> hoá đơn</p>
                </div>
                <form class="invoice-filters" action="<c:url value='/invoices'/>" method="get">
                    <div class="inv-field">
                        <label class="stock-filters__label" for="invoice-status">Trạng thái</label>
                        <select class="inv-control" id="invoice-status" name="status">
                            <option value="">Tất cả</option>
                            <option value="ISSUED"${status == 'ISSUED' ? ' selected' : ''}>Chưa thanh toán</option>
                            <option value="PARTIALLY_PAID"${status == 'PARTIALLY_PAID' ? ' selected' : ''}>Thanh toán một phần</option>
                            <option value="PAID"${status == 'PAID' ? ' selected' : ''}>Đã thanh toán</option>
                        </select>
                    </div>
                    <label class="invoice-filters__check"><input type="checkbox" name="overdue" value="1"${overdue ? ' checked' : ''}> Chỉ hoá đơn quá hạn</label>
                    <button class="inv-button" type="submit" id="invoice-filter">Lọc</button>
                </form>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="invoice-table">
                        <thead>
                            <tr>
                                <th scope="col">Số hoá đơn</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Ngày lập</th>
                                <th scope="col">Hạn thanh toán</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Tổng tiền</th>
                                <th scope="col" class="is-number">Còn nợ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="invoice" items="${invoices}">
                                <tr class="${invoice.overdue ? 'invoice-row--overdue' : ''}">
                                    <td><a class="inv-table__main" href="<c:url value='/invoices/view'><c:param name='id' value='${invoice.id}'/></c:url>"><c:out value="${invoice.invoiceNo}"/></a></td>
                                    <td><c:out value="${invoice.customerName}"/></td>
                                    <td><c:out value="${invoice.issueDateText}"/></td>
                                    <td><c:out value="${invoice.dueDateText}"/><c:if test="${invoice.overdue}"> <span class="invoice-pill invoice-pill--danger">Quá hạn</span></c:if></td>
                                    <td><span class="invoice-pill invoice-pill--${invoice.statusTone}"><c:out value="${invoice.statusLabel}"/></span></td>
                                    <td class="is-number"><fmt:formatNumber value="${invoice.total}" maxFractionDigits="0"/> ₫</td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${invoice.outstanding}" maxFractionDigits="0"/> ₫</strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty invoices}">
                                <tr><td class="inv-table__empty" colspan="7">Chưa có hoá đơn nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/invoices.js'/>"></script>
</body>
</html>
