package com.oms.dao;

import com.oms.model.DebtAgingRow;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Sổ công nợ theo đại lý với phân tích tuổi nợ (S7-03), tính từ hoá đơn chưa huỷ
public class DebtLedgerDao {

    // Tuổi nợ = số ngày từ ngày lập hoá đơn đến ngày xem; chỉ phần còn nợ được chia nhóm
    private static final String SQL = "SELECT c.id, c.code, c.name, SUM(v.total_amount),"
            + " SUM(v.amount_paid + v.amount_credited), SUM(v.outstanding_amount),"
            + " SUM(IF(DATEDIFF(?, v.issue_date) <= 30, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.issue_date) BETWEEN 31 AND 60, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.issue_date) BETWEEN 61 AND 90, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.issue_date) > 90, v.outstanding_amount, 0)),"
            + " SUM(IF(v.due_date < ?, v.outstanding_amount, 0))"
            + " FROM invoices v JOIN customers c ON c.id = v.customer_id WHERE v.status <> 'CANCELLED'"
            + " GROUP BY c.id, c.code, c.name%s ORDER BY SUM(v.outstanding_amount) DESC, c.name";

    // overdueOnly: chỉ đại lý có khoản quá hạn (hạn thanh toán trước ngày xem và còn nợ)
    public List<DebtAgingRow> findRows(LocalDate today, boolean overdueOnly) throws SQLException {
        String sql = String.format(SQL, overdueOnly ? " HAVING SUM(IF(v.due_date < ?, v.outstanding_amount, 0)) > 0" : "");
        List<DebtAgingRow> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Date date = Date.valueOf(today);
            for (int i = 1; i <= 5; i++) {
                statement.setDate(i, date);
            }
            if (overdueOnly) {
                statement.setDate(6, date);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new DebtAgingRow(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3),
                            resultSet.getBigDecimal(4), resultSet.getBigDecimal(5), resultSet.getBigDecimal(6),
                            resultSet.getBigDecimal(7), resultSet.getBigDecimal(8), resultSet.getBigDecimal(9),
                            resultSet.getBigDecimal(10), resultSet.getBigDecimal(11)));
                }
            }
        }
        return rows;
    }
}
