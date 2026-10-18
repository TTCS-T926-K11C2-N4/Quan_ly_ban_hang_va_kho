<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="stocktakes"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Kiểm kê kho"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Kiểm kê kho | Hệ thống quản lý bán hàng &amp; kho</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
    <link rel="stylesheet" href="<c:url value='/assets/css/app.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/inventory.css'/>">
    <link rel="stylesheet" href="<c:url value='/assets/css/stocktakes.css'/>">
</head>
<body class="app-page">
    <%@ include file="/WEB-INF/views/layout/sidebar.jspf" %>

    <div class="app-shell">
        <%@ include file="/WEB-INF/views/layout/topbar.jspf" %>

        <main class="app-content inv-page">
            <header class="inv-header">
                <div>
                    <h1 class="inv-header__title">Kiểm kê kho</h1>
                    <p class="inv-header__subtitle">Đối chiếu số tồn sổ sách với số đếm thực tế, ghi nhận chênh lệch và điều chỉnh tồn về đúng thực tế.</p>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="stocktake-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="stocktake-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${canManage}">
                <section class="inv-card" aria-labelledby="stocktake-new-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="stocktake-new-title">Tạo phiếu kiểm kê</h2>
                            <p class="inv-card__subtitle">Số tồn sổ được chốt tại thời điểm tạo phiếu, các lần nhập kho sau đó không làm đổi số chụp.</p>
                        </div>
                    </div>
                    <form class="stocktake-new" id="stocktake-new-form" method="post" action="<c:url value='/stocktakes/new'/>">
                        <div class="inv-field">
                            <label class="inv-field__label" for="stocktake-warehouse">Kho cần kiểm kê</label>
                            <select class="inv-control" id="stocktake-warehouse" name="warehouse" required>
                                <option value="">Chọn kho</option>
                                <c:forEach var="warehouse" items="${warehouses}">
                                    <option value="${warehouse.id}"><c:out value="${warehouse.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="inv-field">
                            <label class="inv-field__label" for="stocktake-scope">Phạm vi</label>
                            <select class="inv-control" id="stocktake-scope" name="scope">
                                <option value="WAREHOUSE">Toàn kho</option>
                                <option value="CATEGORY">Theo nhóm hàng</option>
                            </select>
                        </div>
                        <div class="inv-field" id="stocktake-category-field" hidden>
                            <label class="inv-field__label" for="stocktake-category">Nhóm hàng</label>
                            <select class="inv-control" id="stocktake-category" name="category">
                                <option value="">Chọn nhóm hàng</option>
                                <c:forEach var="category" items="${categories}">
                                    <option value="${category.id}"><c:out value="${category.name}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <button class="inv-button inv-button--primary" type="submit" id="stocktake-create">Tạo phiếu kiểm kê</button>
                    </form>
                </section>
            </c:if>

            <section class="inv-card" aria-labelledby="stocktake-table-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="stocktake-table-title">Phiếu kiểm kê</h2>
                        <p class="inv-card__subtitle">Phiếu mới nhất ở trên cùng.</p>
                    </div>
                    <p class="inv-card__meta">Tổng cộng <strong>${stocktakes.size()} phiếu</strong></p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="stocktake-table">
                        <thead>
                            <tr>
                                <th scope="col">Mã phiếu</th>
                                <th scope="col">Kho / phạm vi</th>
                                <th scope="col">Trạng thái</th>
                                <th scope="col" class="is-number">Đã đếm</th>
                                <th scope="col" class="is-number">Dòng lệch</th>
                                <th scope="col">Chụp tồn lúc</th>
                                <th scope="col">Người tạo</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="row" items="${stocktakes}">
                                <tr>
                                    <td>
                                        <a class="inv-table__main" href="<c:url value='/stocktakes/view'><c:param name='id' value='${row.id}'/></c:url>"><c:out value="${row.stocktakeNo}"/></a>
                                    </td>
                                    <td>
                                        <span class="inv-table__main"><c:out value="${row.warehouseName}"/></span>
                                        <span class="inv-table__sub"><c:out value="${row.scopeText}"/></span>
                                    </td>
                                    <td><span class="stocktake-status stocktake-status--${row.statusTone}"><c:out value="${row.statusLabel}"/></span></td>
                                    <td class="is-number">${row.countedCount} / ${row.itemCount}</td>
                                    <td class="is-number${row.varianceCount > 0 ? ' stocktake-variance' : ''}">${row.varianceCount}</td>
                                    <td><c:out value="${row.snapshotAtText}"/></td>
                                    <td><c:out value="${row.createdByName}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty stocktakes}">
                                <tr><td class="inv-table__empty" colspan="7">Chưa có phiếu kiểm kê nào.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/stocktakes.js'/>"></script>
</body>
</html>
