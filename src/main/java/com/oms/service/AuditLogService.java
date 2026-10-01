package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.model.AuditLogFilter;
import com.oms.model.AuditLogRecord;
import com.oms.model.PageResult;
import com.oms.model.SelectOption;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Xem nhật ký thao tác trên tồn kho, giá, hạn mức công nợ, hoá đơn và tài khoản (S2-04)
public class AuditLogService {

    public static final int PAGE_SIZE = 20;

    private final AuditLogDao auditLogDao = new AuditLogDao();

    public PageResult<AuditLogRecord> search(AuditLogFilter filter, int requestedPage) throws SQLException {
        long total = auditLogDao.count(filter);
        int totalPages = Math.max(1, (int) ((total + PAGE_SIZE - 1) / PAGE_SIZE));
        // Trang vượt quá (vd sau khi lọc bớt kết quả) thì đưa về trang cuối
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        List<AuditLogRecord> items = auditLogDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE);
        return new PageResult<>(items, page, PAGE_SIZE, total);
    }

    public List<SelectOption> getActors() throws SQLException {
        return auditLogDao.findActors();
    }

    // Trả về lỗi theo tên ô lọc; rỗng nghĩa là hợp lệ
    public Map<String, String> validate(AuditLogFilter filter) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (filter.getFromDate() != null && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            errors.put("toDate", "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        return errors;
    }
}
