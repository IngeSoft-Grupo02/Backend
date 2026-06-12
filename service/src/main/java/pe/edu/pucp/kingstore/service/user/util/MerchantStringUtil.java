package pe.edu.pucp.kingstore.service.user.util;

import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class MerchantStringUtil {

    private MerchantStringUtil() {}

    public static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String normalizeEmail(String email) {
        requireText(email, "Email");
        return email.trim().toLowerCase();
    }

    public static String slugify(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    public static Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "NEGRO", "BLACK" -> Color.BLACK;
            case "BLANCO", "WHITE" -> Color.WHITE;
            case "ROJO", "RED"    -> Color.RED;
            case "AZUL", "BLUE"   -> Color.BLUE;
            case "VERDE", "GREEN" -> Color.GREEN;
            default -> null;
        };
    }

    public static List<String> imageNames(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .limit(5)
                .toList();
    }

    public static String[] splitCsv(String line) {
        if (line == null) return new String[0];
        List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    public static String get(String[] cols, Map<String, Integer> index, String key) {
        if (index == null || cols == null) return null;
        Integer i = index.get(key);
        return i == null || i >= cols.length ? null : cols[i].trim();
    }

    public static QuotationStatus parseQuotationStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PENDING",  "PENDIENTE" -> QuotationStatus.PENDING;
            case "APPROVED", "APROBADA"  -> QuotationStatus.APPROVED;
            case "REJECTED", "RECHAZADA" -> QuotationStatus.REJECTED;
            default -> throw new BusinessRuleException("Invalid quotation status: " + value);
        };
    }

    public static OrderStatus parseOrderStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PAYMENT_CONFIRMED", "PAGADO",    "APROBADO"  -> OrderStatus.PAYMENT_CONFIRMED;
            case "IN_PREPARATION",    "EN_PROCESO"              -> OrderStatus.IN_PREPARATION;
            case "IN_TRANSIT",        "ENVIADO"                 -> OrderStatus.IN_TRANSIT;
            case "DELIVERED",         "ENTREGADO"               -> OrderStatus.DELIVERED;
            case "CANCELLED",         "CANCELADO"               -> OrderStatus.CANCELLED;
            default -> throw new BusinessRuleException("Invalid order status: " + value);
        };
    }

    public static ProductStatus parseProductStatus(String value) {
        String normalized = value.trim().toLowerCase().replace(' ', '_');
        return switch (normalized) {
            case "draft",         "borrador"      -> ProductStatus.DRAFT;
            case "out_of_stock",  "fuera_de_stock" -> ProductStatus.OUT_OF_STOCK;
            case "active",        "activo"         -> ProductStatus.ACTIVE;
            case "inactive",      "inactivo"       -> ProductStatus.INACTIVE;
            default -> throw new BusinessRuleException("Invalid product status: " + value);
        };
    }

    public static StoreStatus parseStoreStatus(String value) {
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "ACTIVE",    "ACTIVA"      -> StoreStatus.ACTIVE;
            case "INACTIVE",  "INACTIVA"    -> StoreStatus.INACTIVE;
            case "SUSPENDED", "SUSPENDIDA"  -> StoreStatus.SUSPENDED;
            default -> throw new BusinessRuleException("Invalid store status: " + value);
        };
    }
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
