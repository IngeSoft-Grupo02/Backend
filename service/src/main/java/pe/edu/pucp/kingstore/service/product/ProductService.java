package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;

@Service
public class ProductService extends AbstractCrudService<Product> {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        super(productRepository, "Product");
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findByStore(Integer storeId) {
        requireId(storeId);
        return productRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveByStore(Integer storeId) {
        requireId(storeId);
        return productRepository.findByStoreIdAndActive(storeId, true);
    }

    @Transactional(readOnly = true)
    public List<Product> searchByNameInStore(String name, Integer storeId) {
        requireText(name, "Product name");
        requireId(storeId);
        return productRepository.findByNameContainingAndStoreId(name.trim(), storeId);
    }

    @Override
    protected void validateForSave(Product product) {
        if (product.getStore() == null || product.getStore().getId() == null) {
            throw new BusinessRuleException("Product must belong to a store");
        }
        requireText(product.getName(), "Product name");
        if (product.getCostPrice() < 0 || product.getBasePrice() < 0) {
            throw new BusinessRuleException("Product prices cannot be negative");
        }
        if (product.getBasePrice() < product.getCostPrice()) {
            throw new BusinessRuleException("Base price cannot be lower than cost price");
        }
        if (product.getVariants() != null) {
            product.getVariants().forEach(variant -> {
                if (variant.getStock() < 0) {
                    throw new BusinessRuleException("Product variant stock cannot be negative");
                }
            });
        }
    }
}
