package com.oms.controller;

import com.oms.model.AccountDetail;
import com.oms.model.AccountStatus;
import com.oms.model.SelectOption;
import com.oms.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// Trang Chi tiết tài khoản và hai hộp thoại mở trên trang đó (render từ server):
//   /accounts/view   : chỉ xem
//   /accounts/lock   : mở sẵn hộp thoại Khóa tài khoản (S1-10A); POST để khóa
//   /accounts/unlock : mở sẵn hộp thoại Mở khóa tài khoản (S1-10B); POST để mở khóa
@WebServlet({"/accounts/view", "/accounts/lock", "/accounts/unlock"})
public class AccountDetailServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/accounts/account-detail.jsp";
    private static final String LOCK_PATH = "/accounts/lock";
    private static final String UNLOCK_PATH = "/accounts/unlock";

    private final AccountService accountService = new AccountService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            AccountDetail account = findAccount(request);
            if (account == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String path = request.getServletPath();
            // Hộp thoại không hợp với trạng thái hiện tại (vd khóa tài khoản đã khóa) thì về trang xem
            if (!canOpenDialog(path, account)) {
                redirectToView(request, response, account.getId());
                return;
            }
            Flash.moveToRequest(request, AccountEditServlet.FLASH_MESSAGE, "flashMessage");
            showPage(request, response, account, path, Map.of(), null, null);
        } catch (SQLException e) {
            log("Không tải được chi tiết tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!LOCK_PATH.equals(path) && !UNLOCK_PATH.equals(path)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            AccountDetail account = findAccount(request);
            if (account == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!canOpenDialog(path, account)) {
                redirectToView(request, response, account.getId());
                return;
            }

            String message;
            if (UNLOCK_PATH.equals(path)) {
                message = accountService.unlock(account, request.getRemoteAddr())
                        ? "Đã mở khóa tài khoản " + account.getUsername() + "."
                        : "Tài khoản " + account.getUsername() + " không còn bị khóa.";
            } else {
                String reason = normalize(request.getParameter("lockReason"));
                Long handoverUserId = AccountFormParser.parseId(request.getParameter("handoverUserId"));
                List<SelectOption> candidates = accountService.getHandoverCandidates(account.getId());
                Map<String, String> errors = accountService.validateLock(account, reason, handoverUserId, candidates);
                if (!errors.isEmpty()) {
                    showPage(request, response, account, path, errors, reason, handoverUserId);
                    return;
                }
                message = accountService.lock(account, reason, handoverUserId, request.getRemoteAddr())
                        ? "Đã khóa tài khoản " + account.getUsername() + "."
                        : "Tài khoản " + account.getUsername() + " đã bị khóa trước đó.";
            }
            request.getSession().setAttribute(AccountEditServlet.FLASH_MESSAGE, message);
            redirectToView(request, response, account.getId());
        } catch (SQLException e) {
            log("Không khóa/mở khóa được tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Khóa: tài khoản chưa bị khóa hẳn. Mở khóa: đang khóa hẳn hoặc khóa tạm (nhập sai mật khẩu).
    private static boolean canOpenDialog(String path, AccountDetail account) {
        if (LOCK_PATH.equals(path)) {
            return account.getStatus() != AccountStatus.LOCKED;
        }
        if (UNLOCK_PATH.equals(path)) {
            return account.getStatus().isLocked();
        }
        return true;
    }

    private AccountDetail findAccount(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : accountService.getDetail(id);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, AccountDetail account,
                          String path, Map<String, String> errors, String lockReason, Long handoverUserId)
            throws ServletException, IOException, SQLException {
        boolean lockMode = LOCK_PATH.equals(path);
        boolean unlockMode = UNLOCK_PATH.equals(path);
        request.setAttribute("account", account);
        request.setAttribute("lockMode", lockMode);
        request.setAttribute("unlockMode", unlockMode);
        if (lockMode) {
            request.setAttribute("handoverCandidates", accountService.getHandoverCandidates(account.getId()));
            request.setAttribute("lockReasonMinLength", AccountService.LOCK_REASON_MIN_LENGTH);
            request.setAttribute("errors", errors);
            request.setAttribute("lockReason", lockReason);
            request.setAttribute("handoverUserId", handoverUserId);
        }
        if (unlockMode) {
            request.setAttribute("handoverOnLastLock", accountService.lastLockHadHandover(account.getId()));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static void redirectToView(HttpServletRequest request, HttpServletResponse response, long id)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/accounts/view?id=" + id);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
