package pe.edu.pucp.kingstore.api.util;

public final class MerchantFileUtil {

    private MerchantFileUtil() {}

    public static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    public static String contentType(String filename) {
        String ext = extension(filename);
        return "image/" + (ext.equals("jpg") ? "jpeg" : ext);
    }
}