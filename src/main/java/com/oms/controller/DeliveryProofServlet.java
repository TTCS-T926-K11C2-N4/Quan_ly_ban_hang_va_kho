package com.oms.controller;

import com.oms.dao.DeliveryDao;
import com.oms.dao.PermissionDao;
import com.oms.service.DeliveryScope;
import com.oms.util.FileStorage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// S6-08: ảnh biên bản giao hàng (?id=<id delivery_proofs>); chỉ người xem được đơn đó mới tải được
@WebServlet("/deliveries/proof")
public class DeliveryProofServlet extends HttpServlet {

    private final DeliveryDao deliveryDao = new DeliveryDao();
    private final PermissionDao permissionDao = new PermissionDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        try {
            DeliveryDao.ProofFile proof = id == null ? null : deliveryDao.findProof(id);
            DeliveryScope scope = DeliveryScope.of(permissionDao, CurrentUser.get(request).getId());
            if (proof == null || !scope.allowed()
                    || (scope.salesRepId() != null && !scope.salesRepId().equals(proof.salesRepId()))) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path file = FileStorage.resolve(proof.storageKey());
            if (!Files.isRegularFile(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setCharacterEncoding(null);
            response.setContentType(proof.contentType());
            response.setContentLengthLong(Files.size(file));
            response.setHeader("Cache-Control", "private, max-age=86400");
            response.setHeader("X-Content-Type-Options", "nosniff");
            Files.copy(file, response.getOutputStream());
        } catch (SQLException e) {
            log("Không tải được ảnh biên bản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
