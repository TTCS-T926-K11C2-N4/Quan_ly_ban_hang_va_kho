package com.oms.controller;

import com.oms.model.ProductForm;
import com.oms.util.AvatarImages;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Đọc form thêm/sửa sản phẩm (multipart vì có ảnh); ô trống thành null, SKU đổi sang chữ hoa
final class ProductFormParser {

    private ProductFormParser() {
    }

    // canEditCost = false thì bỏ qua ô giá vốn kể cả khi request có gửi lên (không tin form phía trình duyệt)
    static ProductForm read(HttpServletRequest request, boolean canEditCost) {
        String sku = normalize(request.getParameter("sku"));
        return new ProductForm(
                sku == null ? null : sku.toUpperCase(Locale.ROOT),
                normalize(request.getParameter("name")),
                AccountFormParser.parseId(request.getParameter("categoryId")),
                AccountFormParser.parseId(request.getParameter("baseUnitId")),
                normalize(request.getParameter("packagingSpec")),
                canEditCost ? normalize(request.getParameter("costPrice")) : null,
                normalize(request.getParameter("status")),
                normalize(request.getParameter("description")),
                AccountFormParser.parseId(request.getParameter("version")),
                readConversions(request));
    }

    // Dòng quy đổi gửi lên dạng mảng conversionUnitId[], conversionFactor[] cùng thứ tự; giữ cả dòng trống
    // để báo lỗi (người dùng phải nhập đủ hoặc xoá dòng)
    private static List<ProductForm.Conversion> readConversions(HttpServletRequest request) {
        String[] units = request.getParameterValues("conversionUnitId");
        String[] factors = request.getParameterValues("conversionFactor");
        int count = Math.max(units == null ? 0 : units.length, factors == null ? 0 : factors.length);
        List<ProductForm.Conversion> conversions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String unit = units != null && i < units.length ? normalize(units[i]) : null;
            String factor = factors != null && i < factors.length ? normalize(factors[i]) : null;
            conversions.add(new ProductForm.Conversion(AccountFormParser.parseId(unit), factor));
        }
        return conversions;
    }

    // Nội dung ô ảnh "image"; mảng rỗng nếu không chọn ảnh
    static byte[] readImage(HttpServletRequest request) throws AvatarImages.InvalidImageException, IOException,
            ServletException {
        Part part;
        try {
            part = request.getPart("image");
        } catch (IllegalStateException e) {
            // Tomcat báo vượt maxFileSize bằng IllegalStateException
            throw new AvatarImages.InvalidImageException("Ảnh vượt quá dung lượng tối đa 2MB.");
        }
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    // Chỉ giữ tên tệp (một số trình duyệt gửi cả đường dẫn trên máy), cắt cho vừa cột original_name
    static String imageName(HttpServletRequest request) throws IOException, ServletException {
        Part part = request.getPart("image");
        String submitted = part == null ? null : part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) {
            return "product";
        }
        String name = submitted.substring(Math.max(submitted.lastIndexOf('/'), submitted.lastIndexOf('\\')) + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
