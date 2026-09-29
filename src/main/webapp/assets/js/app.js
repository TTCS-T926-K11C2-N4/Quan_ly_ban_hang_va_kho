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
