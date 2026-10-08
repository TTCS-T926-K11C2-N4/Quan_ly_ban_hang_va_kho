package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerAssignmentDao;
import com.oms.dao.CustomerDao;
import com.oms.model.AssignmentHistoryEntry;
import com.oms.model.Customer;
import com.oms.model.SelectOption;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Phân công nhân viên kinh doanh phụ trách đại lý (S3-06). Mỗi đại lý một người phụ trách chính
// (customers.sales_rep_id); Nhân viên kinh doanh chỉ thấy đại lý mình phụ trách (data_scope ASSIGNED, CustomerService).
// Mỗi lần đổi người phụ trách ghi customer_assignment_history và nhật ký thao tác trong cùng transaction;
// chuyển giao hàng loạt khi nhân viên nghỉ chuyển cả đại lý lẫn địa bàn (user_regions), lọc được theo khu vực.
public class CustomerAssignmentService {

    public static final int REASON_MAX_LENGTH = 500;
    public static final int MAX_SELECTED = 200;
    private static final String CUSTOMER_ENTITY = "CUSTOMER";
    private static final String USER_ENTITY = "USER";

    // Kết quả phân công các đại lý đã chọn: assigned đã đổi, unchanged đã đúng người đó, conflict người khác vừa đổi
    public record AssignResult(int assigned, int unchanged, int conflict) {
    }

    // Xem trước chuyển giao: đại lý và địa bàn sẽ chuyển
    public record TransferPreview(List<SelectOption> customers, List<String> regions) {
    }

    private final CustomerAssignmentDao assignmentDao = new CustomerAssignmentDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    public List<SelectOption> getActiveSalesReps() throws SQLException {
        return assignmentDao.findActiveSalesReps();
    }

    public List<AssignmentHistoryEntry> getHistory(long customerId) throws SQLException {
        return assignmentDao.findHistory(customerId);
    }

    // Lỗi theo tên ô; rỗng nghĩa là hợp lệ
    public Map<String, String> validateAssign(List<Long> customerIds, Long toSalesRepId, String reason,
                                              List<SelectOption> activeReps) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (customerIds.isEmpty()) {
            errors.put("customerIds", "Chọn ít nhất một đại lý để phân công.");
        } else if (customerIds.size() > MAX_SELECTED) {
            errors.put("customerIds", "Mỗi lần phân công tối đa " + MAX_SELECTED + " đại lý.");
        }
        if (toSalesRepId == null) {
            errors.put("toSalesRepId", "Chọn nhân viên kinh doanh phụ trách.");
        } else if (find(activeReps, toSalesRepId) == null) {
            errors.put("toSalesRepId", "Nhân viên này không còn là nhân viên kinh doanh đang hoạt động.");
        }
        if (reason != null && reason.length() > REASON_MAX_LENGTH) {
            errors.put("reason", "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    // Gọi validateAssign trước
    public AssignResult assign(List<Long> customerIds, long toSalesRepId, String reason, long actorUserId,
                               String ipAddress) throws SQLException {
        Map<Long, String> repNames = repNames();
        int assigned = 0;
        int unchanged = 0;
        int conflict = 0;
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Long customerId : customerIds) {
                    Customer customer = customerDao.findById(customerId);
                    if (customer == null) {
                        conflict++;
                    } else if (Long.valueOf(toSalesRepId).equals(customer.getSalesRepId())) {
                        unchanged++;
                    } else if (assignmentDao.updateSalesRep(connection, customerId, customer.getSalesRepId(),
                            toSalesRepId, actorUserId)) {
                        recordChange(connection, customer.getId(), customer.getCode(), customer.getSalesRepId(),
                                toSalesRepId, reason, repNames, actorUserId, ipAddress);
                        assigned++;
                    } else {
                        conflict++;
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
        return new AssignResult(assigned, unchanged, conflict);
    }

    public Map<String, String> validateTransfer(Long fromSalesRepId, Long toSalesRepId, String reason,
                                                List<SelectOption> activeReps) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (fromSalesRepId == null) {
            errors.put("fromSalesRepId", "Chọn nhân viên bàn giao.");
        }
        if (toSalesRepId == null) {
            errors.put("toSalesRepId", "Chọn nhân viên nhận bàn giao.");
        } else if (find(activeReps, toSalesRepId) == null) {
            errors.put("toSalesRepId", "Nhân viên nhận phải là nhân viên kinh doanh đang hoạt động.");
        } else if (toSalesRepId.equals(fromSalesRepId)) {
            errors.put("toSalesRepId", "Nhân viên nhận phải khác nhân viên bàn giao.");
        }
        if (reason == null) {
            errors.put("reason", "Vui lòng nhập lý do chuyển giao.");
        } else if (reason.length() > REASON_MAX_LENGTH) {
            errors.put("reason", "Lý do tối đa " + REASON_MAX_LENGTH + " ký tự.");
        }
        return errors;
    }

    public TransferPreview previewTransfer(long fromSalesRepId, Long regionId) throws SQLException {
        return new TransferPreview(assignmentDao.findCustomersOf(fromSalesRepId, regionId),
                assignmentDao.findRegionNames(fromSalesRepId, regionId));
    }

    // Gọi validateTransfer trước. Trả về số đại lý đã chuyển.
    public int transfer(long fromSalesRepId, long toSalesRepId, Long regionId, String reason, long actorUserId,
                        String ipAddress) throws SQLException {
        Map<Long, String> repNames = repNames();
        List<String> regions = assignmentDao.findRegionNames(fromSalesRepId, regionId);
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int moved = 0;
                for (SelectOption customer : assignmentDao.findCustomersOf(connection, fromSalesRepId, regionId)) {
                    if (assignmentDao.updateSalesRep(connection, customer.getId(), fromSalesRepId, toSalesRepId,
                            actorUserId)) {
                        recordChange(connection, customer.getId(), customer.getCode(), fromSalesRepId, toSalesRepId,
                                reason, repNames, actorUserId, ipAddress);
                        moved++;
                    }
                }
                if (!regions.isEmpty()) {
                    assignmentDao.transferRegions(connection, fromSalesRepId, toSalesRepId, regionId);
                    Map<String, Object> before = new LinkedHashMap<>();
                    before.put("salesRep", repNames.get(fromSalesRepId));
                    before.put("regions", regions);
                    Map<String, Object> after = new LinkedHashMap<>(before);
                    after.put("salesRep", repNames.get(toSalesRepId));
                    auditLogDao.insert(connection, actorUserId, "SALES_REGION_TRANSFER", USER_ENTITY, fromSalesRepId,
                            JsonUtil.object(before), JsonUtil.object(after), reason, ipAddress);
                }
                connection.commit();
                return moved;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void recordChange(Connection connection, long customerId, String customerCode, Long fromSalesRepId,
                              long toSalesRepId, String reason, Map<Long, String> repNames, long actorUserId,
                              String ipAddress) throws SQLException {
        assignmentDao.insertHistory(connection, customerId, fromSalesRepId, toSalesRepId, reason, actorUserId);
        auditLogDao.insert(connection, actorUserId, "CUSTOMER_ASSIGN", CUSTOMER_ENTITY, customerId,
                JsonUtil.object(values(customerCode, fromSalesRepId == null ? null : repNames.get(fromSalesRepId))),
                JsonUtil.object(values(customerCode, repNames.get(toSalesRepId))), reason, ipAddress);
    }

    // Tên mọi người từng / đang phụ trách đại lý (kể cả đã nghỉ) để ghi nhật ký bằng tên thay vì mã
    private Map<Long, String> repNames() throws SQLException {
        return customerDao.findSalesRepOptions().stream()
                .collect(Collectors.toMap(SelectOption::getId, SelectOption::getName, (a, b) -> a));
    }

    static Map<String, Object> values(String customerCode, String salesRepName) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", customerCode);
        values.put("salesRep", salesRepName);
        return values;
    }

    public static SelectOption find(List<SelectOption> options, long id) {
        return options.stream().filter(option -> option.getId() == id).findFirst().orElse(null);
    }
}
