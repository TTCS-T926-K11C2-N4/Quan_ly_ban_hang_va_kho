package com.oms.service;

import com.oms.model.CategoryRow;
import com.oms.model.PageResult;
import com.oms.model.ProductCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// S2-06: thứ tự cây, tìm kiếm giữ nhóm cha, đánh STT, giới hạn số cấp
class ProductCategoryServiceTest {

    // Đồ uống(1) > Bia(2) > Bia lon(4); Đồ uống > Nước ngọt(3, sort 0, xếp trước Bia sort 1); Bánh kẹo(5, ngừng)
    private static List<ProductCategory> sample() {
        List<ProductCategory> categories = new ArrayList<>();
        categories.add(category(4, 2L, "BIA-LON", "Bia lon", 3, "/1/2/4/", 0, true));
        categories.add(category(5, null, "BANH-KEO", "Bánh kẹo", 1, "/5/", 1, false));
        categories.add(category(2, 1L, "BIA", "Bia", 2, "/1/2/", 1, true));
        categories.add(category(1, null, "DO-UONG", "Đồ uống", 1, "/1/", 0, true));
        categories.add(category(3, 1L, "NUOC-NGOT", "Nước ngọt", 2, "/1/3/", 0, true));
        return categories;
    }

    private static ProductCategory category(long id, Long parentId, String code, String name, int level, String path,
                                            int sortOrder, boolean active) {
        return new ProductCategory(id, parentId, code, name, null, level, path, sortOrder, active, 0, 0, 0);
    }

    private static List<String> names(List<CategoryRow> rows) {
        return rows.stream().map(row -> row.getNumber() + " " + row.getCategory().getName()).toList();
    }

    @Test
    void treeOrderPutsParentsFirstAndSortsSiblings() {
        List<String> order = ProductCategoryService.toTreeOrder(sample()).stream().map(ProductCategory::getName).toList();
        assertEquals(List.of("Đồ uống", "Nước ngọt", "Bia", "Bia lon", "Bánh kẹo"), order);
    }

    @Test
    void searchWithoutFilterNumbersEveryLevel() {
        PageResult<CategoryRow> page = ProductCategoryService.searchTree(
                ProductCategoryService.toTreeOrder(sample()), "", null, 1);
        assertEquals(List.of("1 Đồ uống", "1.1 Nước ngọt", "1.2 Bia", "1.2.1 Bia lon", "2 Bánh kẹo"),
                names(page.getItems()));
        assertEquals(2, page.getTotalItems(), "đếm theo nhóm gốc");
    }

    @Test
    void searchKeepsAncestorsOfMatchedGroup() {
        PageResult<CategoryRow> page = ProductCategoryService.searchTree(
                ProductCategoryService.toTreeOrder(sample()), "bia lon", null, 1);
        assertEquals(List.of("1 Đồ uống", "1.1 Bia", "1.1.1 Bia lon"), names(page.getItems()));
        assertFalse(page.getItems().get(0).isMatched(), "Đồ uống chỉ hiện để giữ ngữ cảnh");
        assertTrue(page.getItems().get(2).isMatched());
    }

    @Test
    void statusFilterShowsOnlyInactiveBranches() {
        PageResult<CategoryRow> page = ProductCategoryService.searchTree(
                ProductCategoryService.toTreeOrder(sample()), "", false, 1);
        assertEquals(List.of("1 Bánh kẹo"), names(page.getItems()));
    }

    @Test
    void parentCannotExceedMaxLevelOrBeInsideEditedBranch() {
        ProductCategory levelFour = category(9, 8L, "C4", "Cấp 4", 4, "/6/7/8/9/", 0, true);
        ProductCategory levelFive = category(10, 9L, "C5", "Cấp 5", 5, "/6/7/8/9/10/", 0, true);
        assertNull(ProductCategoryService.parentError(levelFour, null, 1), "nhóm mới ở cấp 5 vẫn được");
        assertNotNull(ProductCategoryService.parentError(levelFive, null, 1), "cấp 6 thì vượt quá");

        ProductCategory bia = category(2, 1L, "BIA", "Bia", 2, "/1/2/", 0, true);
        ProductCategory biaLon = category(4, 2L, "BIA-LON", "Bia lon", 3, "/1/2/4/", 0, true);
        assertNotNull(ProductCategoryService.parentError(biaLon, bia, 2), "không chuyển Bia vào nhóm con của nó");
        assertNotNull(ProductCategoryService.parentError(bia, bia, 2), "không chọn chính nó");
    }

    @Test
    void pathAndSortOrderHelpers() {
        assertEquals("/7/", ProductCategoryService.childPath(null, 7));
        assertEquals("/1/2/7/", ProductCategoryService.childPath("/1/2/", 7));
        assertEquals(0, ProductCategoryService.parseSortOrder(null));
        assertEquals(15, ProductCategoryService.parseSortOrder("15"));
        assertNull(ProductCategoryService.parseSortOrder("-1"));
        assertNull(ProductCategoryService.parseSortOrder("abc"));
    }
}
