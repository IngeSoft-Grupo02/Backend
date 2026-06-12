package pe.edu.pucp.kingstore.api.controller.merchant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import pe.edu.pucp.kingstore.api.util.MerchantFileUtil;
import pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Clase base para todos los controllers del comerciante.
 * Centraliza helpers compartidos para evitar duplicación de código.
 * Cada controller hijo hereda estos métodos directamente.
 */
public abstract class BaseMerchantController {

    protected final MerchantContext merchantContext;

    protected BaseMerchantController(MerchantContext merchantContext) {
        this.merchantContext = merchantContext;
    }

    // ── Contexto de autenticación ─────────────────────────────────────────

    protected Merchant currentMerchant(Authentication authentication) {
        return merchantContext.merchant(authentication);
    }

    protected List<Store> merchantStores(Authentication authentication) {
        return merchantContext.stores(authentication);
    }

    protected Store currentMerchantStore(Authentication authentication, Integer storeId) {
        return merchantContext.currentStore(authentication, storeId);
    }

    protected Integer currentUserAccountId(Authentication authentication) {
        return merchantContext.userAccountId(authentication);
    }

    protected Store storeInMerchantScope(Authentication authentication, Integer storeId) {
        return merchantContext.storeById(authentication, storeId);
    }

    // ── Manejo de excepciones ─────────────────────────────────────────────

    protected ResponseEntity<?> handle(EndpointCall call) {
        try {
            return call.execute();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "The product cannot be deleted because it is referenced by orders, quotations, carts or discounts"
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Validaciones de texto ─────────────────────────────────────────────

    protected void requireText(String value, String fieldName) {
        MerchantStringUtil.requireText(value, fieldName);
    }

    protected String blankToNull(String value) {
        return MerchantStringUtil.blankToNull(value);
    }

    protected String blankToEmpty(String value) {
        return MerchantStringUtil.blankToEmpty(value);
    }

    protected String safe(String value) {
        return MerchantStringUtil.safe(value);
    }

    protected String normalizeEmail(String email) {
        return MerchantStringUtil.normalizeEmail(email);
    }

    // ── Construcción de nombres ───────────────────────────────────────────

    protected String customerName(Customer customer) {
        return MerchantCustomerUtil.customerName(customer);
    }

    protected String fullName(String firstName, String paternalSurname, String maternalSurname) {
        return MerchantCustomerUtil.fullName(firstName, paternalSurname, maternalSurname);
    }

    // ── Utilidades de archivos e imágenes ─────────────────────────────────

    protected String extension(String filename) {
       return MerchantFileUtil.extension(filename);
    }

    protected String contentType(String filename) {
        return MerchantFileUtil.contentType(filename);
    }

    // ── Utilidades de texto y parseo ──────────────────────────────────────

    protected String slugify(String text) {
       return MerchantStringUtil.slugify(text);
    }

    protected Integer parseInt(String value) {
        return MerchantStringUtil.parseInt(value);
    }

    protected Double parseDouble(String value) {
        return MerchantStringUtil.parseDouble(value);
    }

    protected Color parseColor(String value) {
       return MerchantStringUtil.parseColor(value);
    }

    protected List<String> imageNames(String line) {
       return MerchantStringUtil.imageNames(line);
    }

    protected String[] splitCsv(String line) {
       return MerchantStringUtil.splitCsv(line);
    }

    protected String get(String[] cols, Map<String, Integer> index, String key) {
        return MerchantStringUtil.get(cols,index,key);
    }

    // ── Interfaz funcional para endpoints ────────────────────────────────

    @FunctionalInterface
    protected interface EndpointCall {
        ResponseEntity<?> execute() throws IOException;
    }
}