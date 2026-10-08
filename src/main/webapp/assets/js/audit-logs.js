// Nhật ký thao tác (S2-04): lọc ngay khi đổi ô lọc; nút ••• mở hộp chi tiết so sánh giá trị trước và sau
(function () {
    var filterForm = document.getElementById("audit-filter-form");
    if (filterForm) {
        filterForm.querySelectorAll("select, input[type='date']").forEach(function (control) {
            control.addEventListener("change", function () {
                filterForm.requestSubmit();
            });
        });
    }

    var dialog = document.getElementById("audit-detail-dialog");
    if (!dialog) {
        return;
    }

    // Tên trường trong old_values/new_values (xem các service ghi audit_logs); trường lạ hiện bằng tên gốc
    var FIELD_LABELS = {
        sku: "Mã SKU", code: "Mã", name: "Tên", username: "Tên đăng nhập", fullName: "Họ và tên",
        email: "Email", phone: "Số điện thoại", roleCodes: "Vai trò", warehouseId: "Kho", regionId: "Địa bàn",
        categoryId: "Nhóm hàng", parentId: "Nhóm cha", baseUnitId: "Đơn vị tính cơ sở",
        packagingSpec: "Quy cách đóng gói", description: "Mô tả", sortOrder: "Thứ tự hiển thị",
        status: "Trạng thái", active: "Đang hoạt động", productIds: "Sản phẩm (mã id)",
        handoverToUserId: "Bàn giao cho", costPriceChanged: "Đổi giá vốn", imageChanged: "Đổi ảnh",
        passwordChanged: "Đổi mật khẩu", avatarChanged: "Đổi ảnh đại diện",
        customerGroupId: "Nhóm khách hàng", validFrom: "Ngày bắt đầu", validTo: "Ngày kết thúc",
        items: "Dòng giá (SKU: giá bán / giá sàn)", conversions: "Đơn vị quy đổi", previousCode: "Từ bảng giá", nextCode: "Phiên bản mới",
        taxCode: "Mã số thuế", contactName: "Người liên hệ", paymentTerms: "Điều khoản thanh toán",
        orderNo: "Mã đơn", customerCode: "Mã đại lý", deliveryAddressId: "Điểm giao (mã id)",
        requestedDate: "Ngày giao mong muốn", lineCount: "Số dòng hàng", subtotal: "Tiền hàng",
        discount: "Chiết khấu", total: "Tổng phải thu",
        label: "Tên điểm giao", address: "Địa chỉ", receiverName: "Người nhận", receiverPhone: "SĐT người nhận",
        routeNote: "Ghi chú đường đi", isDefault: "Điểm mặc định"
    };
    // Trường dùng chung bảng tra tên với trường khác
    var LOOKUP_ALIAS = { parentId: "categoryId" };
    var STATUS_LABELS = {
        ACTIVE: "Hoạt động", LOCKED: "Bị khoá", TEMP_LOCKED: "Khoá tạm", PENDING: "Chờ duyệt",
        DISCONTINUED: "Ngừng kinh doanh", INACTIVE: "Tạm ngưng", DRAFT: "Nháp", PENDING_APPROVAL: "Chờ duyệt"
    };

    var lookups = {};
    try {
        lookups = JSON.parse(document.getElementById("audit-name-lookups").getAttribute("data-json") || "{}");
    } catch (e) {
        lookups = {};
    }

    function parse(json) {
        if (!json) {
            return null;
        }
        try {
            return JSON.parse(json);
        } catch (e) {
            return { "(dữ liệu)": json };
        }
    }

    function format(field, value) {
        if (value === undefined || value === null || value === "") {
            return "—";
        }
        if (typeof value === "boolean") {
            return value ? "Có" : "Không";
        }
        if (Array.isArray(value)) {
            return value.length === 0 ? "—" : value.map(function (item) { return format(field, item); }).join(field === "items" ? "\n" : ", ");
        }
        // Quy đổi ghi dạng "idĐơnVị=hệSố" (ProductService), hiện "Thùng = 24"
        if (field === "conversions" && /^\d+=/.test(String(value))) {
            var parts = String(value).split("=");
            var units = lookups.baseUnitId || {};
            return (units[parts[0]] || "#" + parts[0]) + " = " + parts[1];
        }
        var names = lookups[LOOKUP_ALIAS[field] || field];
        if (names && names[String(value)] !== undefined) {
            return names[String(value)];
        }
        if (field === "status" && STATUS_LABELS[value]) {
            return STATUS_LABELS[value];
        }
        return typeof value === "object" ? JSON.stringify(value) : String(value);
    }

    function fieldOrder(fields) {
        var known = Object.keys(FIELD_LABELS);
        return fields.sort(function (a, b) {
            var ia = known.indexOf(a), ib = known.indexOf(b);
            return (ia < 0 ? known.length : ia) - (ib < 0 ? known.length : ib);
        });
    }

    function cell(row, text, className) {
        var td = document.createElement("td");
        td.textContent = text;
        if (className) {
            td.className = className;
        }
        row.appendChild(td);
    }

    function fill(button) {
        document.getElementById("audit-detail-summary").textContent = button.getAttribute("data-summary");
        document.getElementById("audit-detail-actor").textContent = button.getAttribute("data-actor");
        document.getElementById("audit-detail-time").textContent = button.getAttribute("data-time");
        document.getElementById("audit-detail-ip").textContent = button.getAttribute("data-ip") || "—";
        var reason = button.getAttribute("data-reason");
        document.getElementById("audit-detail-reason").textContent = reason;
        document.getElementById("audit-detail-reason-row").hidden = !reason;

        var before = parse(button.getAttribute("data-old")) || {};
        var after = parse(button.getAttribute("data-new")) || {};
        var fields = fieldOrder(Object.keys(before).concat(Object.keys(after).filter(function (key) {
            return !(key in before);
        })));

        var table = document.getElementById("audit-detail-changes");
        var body = table.tBodies[0];
        body.textContent = "";
        fields.forEach(function (field) {
            var row = document.createElement("tr");
            var oldText = field in before ? format(field, before[field]) : "—";
            var newText = field in after ? format(field, after[field]) : "—";
            // Trường dạng costPriceChanged, passwordChanged chỉ ghi ở giá trị sau: tô màu khi thật sự có đổi
            var changed = /Changed$/.test(field) ? after[field] === true : oldText !== newText;
            if (changed) {
                row.className = "audit-changes__row--changed";
            }
            cell(row, FIELD_LABELS[field] || field, "audit-changes__field");
            cell(row, oldText);
            cell(row, newText);
            body.appendChild(row);
        });
        table.hidden = fields.length === 0;
        document.getElementById("audit-detail-empty").hidden = fields.length > 0;
    }

    document.getElementById("audit-table").addEventListener("click", function (event) {
        var button = event.target.closest(".audit-detail-button");
        if (!button) {
            return;
        }
        fill(button);
        dialog.showModal();
    });
    document.getElementById("audit-detail-close").addEventListener("click", function () {
        dialog.close();
    });
    // Bấm ra ngoài hộp (vùng nền mờ) cũng đóng
    dialog.addEventListener("click", function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });
})();
