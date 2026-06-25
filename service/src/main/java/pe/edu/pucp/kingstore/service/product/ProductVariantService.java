package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.repository.product.ProductVariantRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import java.util.Optional;

@Service
public class ProductVariantService extends AbstractCrudService<ProductVariant> {

    private final ProductVariantRepository productVariantRepository;

    public ProductVariantService(ProductVariantRepository productVariantRepository) {
        super(productVariantRepository, "Product variant");
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    protected void validateForSave(ProductVariant productVariant) {
        if (productVariant.getStock() < 0) {
            throw new BusinessRuleException("Product variant stock cannot be negative");
        }
    }
    public ProductVariant getByIdWithProduct(Integer id) {
        requireId(id);
        return productVariantRepository.findWithProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", id));
    }
}
