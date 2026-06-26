package pe.edu.pucp.kingstore.service.store;

import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.text.Normalizer;
import java.util.Locale;

public final class StoreSlugUtil {

    private StoreSlugUtil() {
    }

    public static String normalizeStoreName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Store name is required");
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    public static String toSlugBase(String normalizedName) {
        String withoutDiacritics = Normalizer.normalize(normalizedName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            throw new BusinessRuleException("Store name must contain letters or numbers to generate a slug");
        }
        return slug;
    }
}
