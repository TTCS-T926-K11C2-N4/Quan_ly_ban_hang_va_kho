<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/>
<c:set var="activeMenu" value="warehouse"/>
<c:set var="activeSubmenu" value="stocktakes"/>
<c:set var="breadcrumbSection" value="Kho hàng"/>
<c:set var="breadcrumbPage" value="Phiếu kiểm kê"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu <c:out value="${stocktake.stocktakeNo}"/> | Hệ thống quản lý bán hàng &amp; kho</title>
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
                    <h1 class="inv-header__title">Phiếu kiểm kê <c:out value="${stocktake.stocktakeNo}"/></h1>
                    <p class="inv-header__subtitle"><c:out value="${stocktake.warehouseName}"/> · <c:out value="${stocktake.scopeText}"/>
                        · Số tồn sổ chụp lúc <c:out value="${stocktake.snapshotAtText}"/></p>
                </div>
                <div class="inv-header__actions">
                    <a class="inv-button" href="<c:url value='/stocktakes'/>">← Danh sách phiếu</a>
                    <span class="stocktake-status stocktake-status--${stocktake.statusTone}" id="stocktake-status"><c:out value="${stocktake.statusLabel}"/></span>
                </div>
            </header>

            <c:if test="${not empty flashMessage}">
                <div class="account-flash inv-flash" id="stocktake-message" role="status"><c:out value="${flashMessage}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="account-flash account-flash--error inv-flash inv-flash--error" id="stocktake-error" role="alert"><c:out value="${flashError}"/></div>
            </c:if>

            <c:if test="${stocktake.status == 'COMPLETED'}">
                <div class="stocktake-note" id="stocktake-completed-info">
                    <strong>Đã chốt lúc <c:out value="${stocktake.finalizedAtText}"/> bởi <c:out value="${stocktake.finalizedByName}"/>.</strong>
                    Lý do điều chỉnh: <c:out value="${stocktake.reason}"/>
                </div>
            </c:if>

            <section class="inv-card" aria-labelledby="stocktake-items-title">
                <div class="inv-card__head">
                    <div>
                        <h2 class="inv-card__title" id="stocktake-items-title">Dòng kiểm kê</h2>
                        <p class="inv-card__subtitle">Đơn vị: đơn vị cơ sở của SKU. Chênh lệch = số đếm − số sổ.</p>
                    </div>
                    <p class="inv-card__meta" id="stocktake-progress">Đã đếm <strong>${stocktake.countedCount} / ${stocktake.itemCount}</strong> dòng · lệch <strong>${stocktake.varianceCount}</strong></p>
                </div>

                <form id="stocktake-count-form" method="post" action="<c:url value='/stocktakes/count'/>">
                    <input type="hidden" name="id" value="${stocktake.id}">
                    <div class="inv-table-scroll">
                        <table class="inv-table stocktake-items" id="stocktake-items">
                            <thead>
                                <tr>
                                    <th scope="col">Sản phẩm / SKU</th>
                                    <th scope="col">Lô</th>
                                    <th scope="col" class="is-number">Số sổ</th>
                                    <th scope="col" class="is-number">Số đếm thực tế</th>
                                    <th scope="col" class="is-number">Chênh lệch</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="item" items="${stocktake.items}">
                                    <tr class="${item.differs ? 'stocktake-items__row--variance' : ''}">
                                        <td>
                                            <span class="inv-table__main"><c:out value="${item.productName}"/></span>
                                            <span class="inv-table__sub">SKU: <c:out value="${item.sku}"/></span>
                                        </td>
                                        <td><c:out value="${item.lotNo}"/></td>
                                        <td class="is-number"><fmt:formatNumber value="${item.systemQty}" maxFractionDigits="3"/> <c:out value="${item.unitName}"/></td>
                                        <td class="is-number">
                                            <c:choose>
                                                <c:when test="${canCount}">
                                                    <input type="hidden" name="itemId" value="${item.id}">
                                                    <input class="inv-control stocktake-items__count" type="text" name="counted" inputmode="decimal"
                                                           maxlength="17" autocomplete="off" placeholder="Nhập số đếm"
                                                           data-system="${item.systemQty.stripTrailingZeros().toPlainString()}"
                                                           value="${item.counted ? item.countedQty.stripTrailingZeros().toPlainString() : ''}"
                                                           aria-label="Số đếm <c:out value='${item.sku}'/> lô <c:out value='${item.lotNo}'/>">
                                                </c:when>
                                                <c:otherwise>
                                                    <c:choose>
                                                        <c:when test="${item.counted}"><fmt:formatNumber value="${item.countedQty}" maxFractionDigits="3"/></c:when>
                                                        <c:otherwise>—</c:otherwise>
                                                    </c:choose>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="is-number stocktake-items__variance" data-variance>
                                            <c:choose>
                                                <c:when test="${not item.counted}">—</c:when>
                                                <c:when test="${item.differs}">${item.variance.signum() > 0 ? '+' : ''}<fmt:formatNumber value="${item.variance}" maxFractionDigits="3"/></c:when>
                                                <c:otherwise>0</c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    <c:if test="${canCount}">
                        <div class="stocktake-actions">
                            <button class="inv-button inv-button--primary" type="submit" id="stocktake-save">Lưu số đếm</button>
                        </div>
                    </c:if>
                </form>
            </section>

            <c:if test="${canFinalize}">
                <section class="inv-card" aria-labelledby="stocktake-finalize-title">
                    <div class="inv-card__head">
                        <div>
                            <h2 class="inv-card__title" id="stocktake-finalize-title">Chốt kiểm kê</h2>
                            <p class="inv-card__subtitle">Chốt phiếu sẽ đưa tồn các dòng lệch về đúng số đếm và ghi vào thẻ kho. Không hoàn tác được; cần lý do điều chỉnh.</p>
                        </div>
                    </div>
                    <form class="stocktake-finalize" id="stocktake-finalize-form" method="post" action="<c:url value='/stocktakes/finalize'/>" novalidate>
                        <input type="hidden" name="id" value="${stocktake.id}">
                        <div class="inv-field">
                            <label class="stock-filters__label" for="stocktake-reason">Lý do điều chỉnh tồn<span class="stocktake-required">*</span></label>
                            <textarea class="inv-control stocktake-reason" id="stocktake-reason" name="reason" rows="2" maxlength="${reasonMaxLength}"
                                      placeholder="Vd: Kiểm kê định kỳ cuối tháng, chênh lệch do hao hụt…" required></textarea>
                            <p class="stocktake-error" id="stocktake-reason-error" role="alert" hidden>Vui lòng nhập lý do điều chỉnh tồn.</p>
                        </div>
                        <button class="inv-button inv-button--primary" type="submit" id="stocktake-finalize">Chốt kiểm kê và điều chỉnh tồn</button>
                    </form>
                </section>
            </c:if>

            <c:if test="${canCount}">
                <form method="post" action="<c:url value='/stocktakes/cancel'/>" class="stocktake-cancel">
                    <input type="hidden" name="id" value="${stocktake.id}">
                    <button class="inv-button" type="submit" id="stocktake-cancel">Huỷ phiếu kiểm kê</button>
                </form>
            </c:if>
        </main>
    </div>

    <script src="<c:url value='/assets/js/app.js'/>"></script>
    <script src="<c:url value='/assets/js/stocktakes.js'/>"></script>
</body>
</html>
