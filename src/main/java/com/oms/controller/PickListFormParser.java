package com.oms.controller;

import com.oms.model.PickListDetail;
import com.oms.model.PickListFilter;
import com.oms.service.PickListService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Đọc bộ lọc màn phiếu soạn và form ghi nhận soạn hàng (S6-03). Mỗi dòng soạn gửi các ô picked_<id>,
// qty_<id>, reason_<id> theo id dòng soạn.
final class PickListFormParser {

    private PickListFormParser() {
    }

    static PickListFilter parseFilter(HttpServletRequest request) {
        return new PickListFilter(request.getParameter("keyword"),
                AccountFormParser.parseId(request.getParameter("warehouse")), request.getParameter("date"),
                request.getParameter("status"), request.getParameter("due"));
    }

    static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // Đơn được tick ở bảng đơn chờ soạn / đơn còn nợ hàng
    static List<Long> parseOrderIds(HttpServletRequest request) {
        List<Long> ids = new ArrayList<>();
        String[] values = request.getParameterValues("orderId");
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    static Map<Long, PickListService.LineInput> parseLines(HttpServletRequest request, PickListDetail pickList) {
        Map<Long, PickListService.LineInput> inputs = new LinkedHashMap<>();
        for (PickListDetail.Line line : pickList.getLines()) {
            String key = Long.toString(line.getId());
            inputs.put(line.getId(), new PickListService.LineInput(request.getParameter("picked_" + key) != null,
                    trim(request.getParameter("qty_" + key)), trim(request.getParameter("reason_" + key))));
        }
        return inputs;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
