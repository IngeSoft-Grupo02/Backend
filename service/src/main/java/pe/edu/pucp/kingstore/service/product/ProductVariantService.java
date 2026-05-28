package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.repository.product.ProductVariantRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

@Service
public class ProductVariantService extends AbstractCrudService<ProductVariant> {

    public ProductVariantService(ProductVariantRepository productVariantRepository) {
        super(productVariantRepository, "Product variant");
    }

    @Override
    protected void validateForSave(ProductVariant productVariant) {
        if (productVariant.getStock() < 0) {
            throw new BusinessRuleException("Product variant stock cannot be negative");
        }
    }
}
