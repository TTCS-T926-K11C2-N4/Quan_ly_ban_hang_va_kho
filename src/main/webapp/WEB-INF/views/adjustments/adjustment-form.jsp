<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="adjustments"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Lập phiếu điều chỉnh"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lập phiếu điều chỉnh tồn kho | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Lập phiếu điều chỉnh tồn kho</h1>
                    <p class="inv-header__subtitle">Chọn kho, nhập số lượng trừ theo từng lô. Giá trị tổn thất tính theo giá vốn và hiện trên phiếu sau khi lập.</p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/adjustments'/>">← Danh sách phiếu</a>
                </div>
            </header>

            <c:if test="${not empty formError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="adjustment-error" role="alert"><c:out value="${formError}"/></div>
            </c:if>

            <c:choose>
                <c:when test="${empty warehouseId}">
                    <section class="inv-card" aria-labelledby="adjustment-pick-title">
                        <h2 class="inv-card__title" id="adjustment-pick-title">Chọn kho</h2>
                        <form class="invoice-filters" action="<c:url value='/adjustments/new'/>" method="get">
                            <div class="inv-field">
                                <label class="inv-field__label" for="adjustment-warehouse">Kho</label>
                                <select class="inv-control" id="adjustment-warehouse" name="warehouse" required>
                                    <c:forEach var="warehouse" items="${warehouses}">
                                        <option value="${warehouse.id}"><c:out value="${warehouse.name}"/> (<c:out value="${warehouse.code}"/>)</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <button class="inv-button inv-button--primary" type="submit" id="adjustment-pick">Tiếp tục</button>
                        </form>
                    </section>
                </c:when>
                <c:otherwise>
                    <form action="<c:url value='/adjustments/new'/>" method="post" id="adjustment-form">
                        <input type="hidden" name="warehouse" value="${warehouseId}">
                        <section class="inv-card" aria-labelledby="adjustment-lots-title">
                            <h2 class="inv-card__title" id="adjustment-lots-title">Lô hàng cần điều chỉnh</h2>
                            <p class="inv-card__subtitle">Nhập số lượng bị trừ (đơn vị cơ sở) ở các lô liên quan, để trống nếu không điều chỉnh.</p>
                            <div class="inv-table-scroll">
                                <table class="inv-table" id="adjustment-lots">
                                    <thead>
                                        <tr>
                                            <th scope="col">Sản phẩm</th>
                                            <th scope="col">Lô</th>
                                            <th scope="col" class="is-number">Tồn lô</th>
                                            <th scope="col">Số lượng trừ</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="lot" items="${lots}">
                                            <tr>
                                                <td><span class="inv-table__main"><c:out value="${lot.productName}"/></span><span class="inv-table__sub"><c:out value="${lot.sku}"/></span></td>
                                                <td><c:out value="${lot.lotNo}"/></td>
                                                <td class="is-number"><fmt:formatNumber value="${lot.qtyOnHand}" maxFractionDigits="3"/></td>
                                                <td>
                                                    <input type="hidden" name="lot" value="${lot.lotId}">
                                                    <input class="inv-control" type="text" inputmode="decimal" name="qty_${lot.lotId}" aria-label="Số lượng trừ ${lot.sku} lô ${lot.lotNo}">
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty lots}">
                                            <tr><td class="inv-table__empty" colspan="4">Kho này không còn lô hàng nào để điều chỉnh.</td></tr>
                                        </c:if>
                                    </tbody>
                                </table>
                            </div>
                        </section>

                        <section class="inv-card" aria-labelledby="adjustment-reason-title">
                            <h2 class="inv-card__title" id="adjustment-reason-title">Lý do điều chỉnh</h2>
                            <div class="invoice-filters">
                                <div class="inv-field">
                                    <label class="inv-field__label" for="adjustment-reason">Lý do</label>
                                    <select class="inv-control" id="adjustment-reason" name="reason" required>
                                        <option value="">— Chọn lý do —</option>
                                        <c:forEach var="entry" items="${reasons}">
                                            <option value="${entry.key}"${reason == entry.key ? ' selected' : ''}><c:out value="${entry.value}"/></option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                            <div class="invoice-issue">
                                <input class="inv-control" type="text" name="note" maxlength="1000" placeholder="Mô tả chi tiết lý do (bắt buộc)" aria-label="Mô tả lý do" required value="<c:out value='${note}'/>">
                                <button class="inv-button inv-button--primary" type="submit" id="adjustment-submit">Gửi duyệt</button>
                            </div>
                        </section>
                    </form>
                </c:otherwise>
            </c:choose>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
