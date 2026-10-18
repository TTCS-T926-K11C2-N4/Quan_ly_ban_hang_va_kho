(function () {
    var toggleButton = document.getElementById("sidebar-toggle");
    var backdrop = document.getElementById("app-sidebar-backdrop");
    var OPEN_CLASS = "app-page--sidebar-open";

    function setSidebarOpen(open) {
        document.body.classList.toggle(OPEN_CLASS, open);
        toggleButton.setAttribute("aria-expanded", String(open));
        toggleButton.setAttribute("aria-label", open ? "Đóng menu" : "Mở menu");
        backdrop.hidden = !open;
    }

    toggleButton.addEventListener("click", function () {
        setSidebarOpen(!document.body.classList.contains(OPEN_CLASS));
    });

    backdrop.addEventListener("click", function () {
        setSidebarOpen(false);
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && document.body.classList.contains(OPEN_CLASS)) {
            setSidebarOpen(false);
            toggleButton.focus();
        }
    });
})();

// Giữ vị trí cuộn của sidebar khi chuyển trang hoặc F5: mỗi trang do server dựng lại nên sidebar luôn bắt đầu ở
// đầu. Lưu scrollTop vào sessionStorage lúc rời trang, khôi phục lúc nạp. Nhóm menu đang mở có thể khác giữa các
// trang làm vị trí lệch, nên nếu mục đang chọn nằm ngoài vùng nhìn thấy thì cuộn vừa đủ để nó hiện ra.
(function () {
    var sidebar = document.getElementById("app-sidebar");
    var KEY = "oms.sidebarScrollTop";
    if (!sidebar) {
        return;
    }

    function save() {
        try {
            sessionStorage.setItem(KEY, String(sidebar.scrollTop));
        } catch (e) {
            // Trình duyệt chặn sessionStorage (chế độ riêng tư): bỏ qua, sidebar về đầu như trước
        }
    }

    var saved = null;
    try {
        saved = sessionStorage.getItem(KEY);
    } catch (e) {
        saved = null;
    }
    if (saved !== null && !isNaN(saved)) {
        sidebar.scrollTop = Number(saved);
    }

    var active = sidebar.querySelector(".app-submenu__link--active, .app-menu__link--active, .app-menu__toggle--current");
    if (active) {
        var box = sidebar.getBoundingClientRect();
        var item = active.getBoundingClientRect();
        if (item.top < box.top) {
            sidebar.scrollTop -= box.top - item.top + 12;
        } else if (item.bottom > box.bottom) {
            sidebar.scrollTop += item.bottom - box.bottom + 12;
        }
    }

    sidebar.addEventListener("click", function (event) {
        if (event.target.closest("a")) {
            save();
        }
    });
    window.addEventListener("pagehide", save);
})();

// Thông báo dạng toast (góc trên phải): chuyển các hộp .account-flash / .sales-flash server gửi về thành toast màu
// đặc có icon, tiêu đề, nút đóng. Thành công / thông tin tự ẩn (có thanh đếm giờ, rê chuột thì dừng); cảnh báo / lỗi
// phải tự đóng. Hộp gốc chỉ bị ẩn, tắt JavaScript thì vẫn hiện như cũ. Hộp cố định (data-toast="off") để nguyên.
(function () {
    var TONES = {
        success: { title: "Thành công", duration: 5000,
            icon: '<path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>' },
        info: { title: "Thông tin", duration: 6000,
            icon: '<circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2.2"/><path d="M12 11v6M12 7.5v.5" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>' },
        warning: { title: "Lưu ý", duration: 0,
            icon: '<path d="M12 3.5l9.5 16.5h-19z" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round"/><path d="M12 10v4.5M12 17.5v.3" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>' },
        error: { title: "Có lỗi", duration: 0,
            icon: '<path d="M12 3l8 3v5.5c0 4.6-3.3 8.4-8 9.5-4.7-1.1-8-4.9-8-9.5V6z" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round"/><path d="M12 8.5v4.5M12 16v.3" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/>' }
    };
    var boxes = document.querySelectorAll(".account-flash, .sales-flash");
    if (!boxes.length) {
        return;
    }
    var stack = document.createElement("div");
    stack.className = "toast-stack";
    document.body.appendChild(stack);

    function toneOf(box) {
        var tones = ["error", "warning", "info"];
        for (var i = 0; i < tones.length; i++) {
            if (box.classList.contains("account-flash--" + tones[i]) || box.classList.contains("sales-flash--" + tones[i])) {
                return tones[i];
            }
        }
        return "success";
    }

    // Tiêu đề: đoạn đầu (nếu hộp có nhiều đoạn) hoặc câu đầu; một câu thì dùng tiêu đề mặc định của loại thông báo
    function split(box, tone) {
        var parts = Array.prototype.map.call(box.querySelectorAll("p"), function (p) { return p.textContent.trim(); })
            .filter(function (text) { return text; });
        var text = box.textContent.replace(/\s+/g, " ").trim();
        if (parts.length > 1) {
            return { title: parts[0], body: parts.slice(1).join(" ").replace(/\s+/g, " ") };
        }
        var cut = text.search(/[.!?]\s/);
        if (cut > 0 && cut < text.length - 2) {
            return { title: text.slice(0, cut + 1), body: text.slice(cut + 2) };
        }
        return { title: TONES[tone].title, body: text };
    }

    function show(box) {
        var tone = toneOf(box);
        var content = split(box, tone);
        var toast = document.createElement("div");
        toast.className = "toast toast--" + tone;
        toast.setAttribute("role", tone === "error" || tone === "warning" ? "alert" : "status");
        toast.innerHTML = '<svg class="toast__icon" viewBox="0 0 24 24" aria-hidden="true">' + TONES[tone].icon + "</svg>"
            + '<div class="toast__text"><p class="toast__title"></p><p class="toast__body"></p></div>'
            + '<button class="toast__close" type="button" aria-label="Đóng thông báo">×</button>';
        toast.querySelector(".toast__title").textContent = content.title;
        toast.querySelector(".toast__body").textContent = content.body;
        toast.querySelector(".toast__close").addEventListener("click", function () { close(toast); });
        var duration = TONES[tone].duration;
        if (duration) {
            // Thông báo dài hiện lâu hơn để kịp đọc; thanh đếm giờ chạy hết thì đóng, rê chuột vào thì dừng (CSS)
            duration = Math.max(duration, (content.title.length + content.body.length) * 60);
            var bar = document.createElement("span");
            bar.className = "toast__progress";
            bar.style.animationDuration = duration + "ms";
            bar.addEventListener("animationend", function () { close(toast); });
            toast.appendChild(bar);
        }
        stack.appendChild(toast);
        box.hidden = true;
    }

    function close(toast) {
        toast.classList.add("toast--leaving");
        setTimeout(function () { toast.remove(); }, 200);
    }

    Array.prototype.forEach.call(boxes, function (box) {
        if (!box.hidden && box.getAttribute("data-toast") !== "off" && box.textContent.trim()) {
            show(box);
        }
    });
})();
