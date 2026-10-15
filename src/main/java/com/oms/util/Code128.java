package com.oms.util;

// Mã vạch Code 128 (bộ ký tự B, ASCII 32–126) dựng thành SVG để in trên phiếu đơn hàng (S4-08), không cần thư viện ngoài.
public final class Code128 {

    // Độ rộng vạch/khoảng trống xen kẽ của giá trị 0..106 (106 = ký tự dừng, 7 phần tử)
    private static final String[] PATTERNS = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };
    private static final int START_B = 104;
    private static final int STOP = 106;
    private static final int QUIET_MODULES = 10;

    private Code128() {
    }

    // Các giá trị ký hiệu của mã vạch: bắt đầu, từng ký tự, số kiểm tra, dừng
    static int[] encode(String text) {
        int[] values = new int[text.length() + 3];
        values[0] = START_B;
        int checksum = START_B;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) {
                throw new IllegalArgumentException("Ký tự không mã hoá được bằng Code 128B: " + (int) c);
            }
            values[i + 1] = c - 32;
            checksum += (c - 32) * (i + 1);
        }
        values[text.length() + 1] = checksum % 103;
        values[text.length() + 2] = STOP;
        return values;
    }

    // Số module (đơn vị độ rộng nhỏ nhất) của cả mã vạch, chưa kể vùng trống hai đầu
    static int modules(int[] values) {
        int total = 0;
        for (int value : values) {
            for (char width : PATTERNS[value].toCharArray()) {
                total += width - '0';
            }
        }
        return total;
    }

    // SVG co theo chiều rộng khung chứa (width 100%), cao height px; text chỉ gồm ký tự in được của ASCII
    public static String svg(String text, int height) {
        int[] values = encode(text);
        int width = modules(values) + 2 * QUIET_MODULES;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"barcode\" viewBox=\"0 0 ").append(width)
                .append(' ').append(height).append("\" preserveAspectRatio=\"none\" role=\"img\" aria-label=\"Mã vạch ")
                .append(text.replace("&", "").replace("<", "").replace("\"", "")).append("\">");
        int x = QUIET_MODULES;
        for (int value : values) {
            String pattern = PATTERNS[value];
            for (int i = 0; i < pattern.length(); i++) {
                int w = pattern.charAt(i) - '0';
                if (i % 2 == 0) {
                    svg.append("<rect x=\"").append(x).append("\" y=\"0\" width=\"").append(w).append("\" height=\"")
                            .append(height).append("\"/>");
                }
                x += w;
            }
        }
        return svg.append("</svg>").toString();
    }
}
