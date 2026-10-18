<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="finance"/>
<c:set var="activeSubmenu" value="credit-notes"/>
<c:set var="breadcrumbSection" value="Hoá đơn & công nợ"/>
<c:set var="breadcrumbPage" value="Chứng từ giảm trừ"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chứng từ giảm trừ công nợ | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Chứng từ giảm trừ công nợ</h1>
                    <p class="inv-header__subtitle">Lập từ phiếu trả hàng đã nhận, giảm trừ theo giá đã bán trên hoá đơn gốc. Hoá đơn gốc giữ nguyên.</p>
                </div>
            </header>

            <section class="inv-card" aria-labelledby="credit-note-table-title">
                <div class="inv-card__head">
                    <h2 class="inv-card__title" id="credit-note-table-title">Chứng từ đã lập</h2>
                    <p class="inv-card__meta"><strong>${creditNotes.size()}</strong> chứng từ</p>
                </div>
                <div class="inv-table-scroll">
                    <table class="inv-table" id="credit-note-table">
                        <thead>
                            <tr>
                                <th scope="col">Số chứng từ</th>
                                <th scope="col">Hoá đơn gốc</th>
                                <th scope="col">Phiếu trả</th>
                                <th scope="col">Đại lý</th>
                                <th scope="col">Lập lúc</th>
                                <th scope="col" class="is-number">Giảm trừ</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${creditNotes}">
                                <tr>
                                    <td><a class="inv-table__main" href="<c:url value='/credit-notes/view'><c:param name='id' value='${item.id}'/></c:url>"><c:out value="${item.creditNoteNo}"/></a></td>
                                    <td><c:out value="${item.invoiceNo}"/></td>
                                    <td><c:out value="${item.returnNo}"/></td>
                                    <td><c:out value="${item.customerName}"/></td>
                                    <td><c:out value="${item.issuedAtText}"/></td>
                                    <td class="is-number"><strong><fmt:formatNumber value="${item.total}" maxFractionDigits="0"/> ₫</strong></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty creditNotes}">
                                <tr><td class="inv-table__empty" colspan="6">Chưa có chứng từ giảm trừ nào.</td></tr>
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
