<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="transfers"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Chuyển kho"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuyển kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/transfers.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Chuyển kho nội bộ</h1>
                    <p class="inv-header__subtitle">Điều hàng từ kho thừa sang kho thiếu. Hàng đang chuyển chưa cộng vào kho đến cho tới khi kho đến xác nhận nhận.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="transfer-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="transfer-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${canManage}">
                <section class="inv-card" aria-labelledby="transfer-new-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="transfer-new-title">Tạo phiếu chuyển kho</h2>
                            <p class="inv-card__subtitle">Chọn kho đi và kho đến, sau đó thêm các lô hàng cần chuyển ở trang phiếu.</p>
                        </div>
                    </div>
                    <form class="transfer-new" id="transfer-new-form" method="post" action="<c:url value='/transfers/new'/>">
                        <div class="inv-field">
                            <label class="stock-filters__label" for="transfer-from">Kho đi</label>
                            <select class="inv-control" id="transfer-from" name="from" required>
                                <option value="">Chọn kho đi</option>
                                <c:forEach var="warehouse" items="${warehouses}">
                                    <option value="${warehouse.id}"><c:out value="${warehouse.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="stock-filters__label" for="transfer-to">Kho đến</label>
                            <select class="inv-control" id="transfer-to" name="to" required>
                                <option value="">Chọn kho đến</option>
                                <c:forEach var="warehouse" items="${warehouses}">
                                    <option value="${warehouse.id}"><c:out value="${warehouse.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field transfer-new__note">
                            <label class="stock-filters__label" for="transfer-note">Ghi chú</label>
                            <input class="inv-control" type="text" id="transfer-note" name="note" maxlength="1000" placeholder="Tuỳ chọn">
                        </div>
                        <button class="inv-button inv-button--primary" type="submit" id="transfer-create">Tạo phiếu</button>
                    </form>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="transfer-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="transfer-table-title">Phiếu chuyển kho</h2>
                        <p class="inv-card__subtitle">Phiếu mới nhất ở trên cùng.</p>
                    </div>
                    <p class="inv-card__meta">Tổng cộng <strong>${transfers.size()} phiếu</strong></p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="transfer-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã phiếu</th>
                                <th scope="col">Kho đi</th>
                                <th scope="col">Kho đến</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col">Xuất kho lúc</th>
                                <th scope="col">Nhận lúc</th>
                                <th scope="col">Người tạo</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${transfers}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/transfers/view'><c:param name='id' value='${row.id}'/></c:url>"><c:out value="${row.transferNo}"/></a></td>
                                    <td><c:out value="${row.fromWarehouseName}"/></td>
                                    <td><c:out value="${row.toWarehouseName}"/></td>
                                    <td><span class="transfer-status transfer-status--${row.statusTone}"><c:out value="${row.statusLabel}"/></span></td>
                                    <td><c:out value="${empty row.shippedAtText ? '—' : row.shippedAtText}"/></td>
                                    <td><c:out value="${empty row.receivedAtText ? '—' : row.receivedAtText}"/></td>
                                    <td><c:out value="${row.createdByName}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty transfers}">
                                <tr><td class="inv-table__empty" colspan="7">Chưa có phiếu chuyển kho nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
</body>
</html>
