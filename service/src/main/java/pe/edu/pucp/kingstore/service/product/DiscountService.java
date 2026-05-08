package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;

@Service
public class DiscountService extends AbstractCrudService<Discount> {

    private final DiscountRepository discountRepository;

    public DiscountService(DiscountRepository discountRepository) {
        super(discountRepository, "Discount");
        this.discountRepository = discountRepository;
    }

    @Transactional(readOnly = true)
    public List<Discount> findByProduct(Integer productId) {
        requireId(productId);
        return discountRepository.findByProductId(productId);
    }

    @Override
    protected void validateForSave(Discount discount) {
        if (discount.getProduct() == null || discount.getProduct().getId() == null) {
            throw new BusinessRuleException("Discount must belong to a product");
        }
        if (discount.getMinQuantity() <= 0 || discount.getMaxQuantity() <= 0) {
            throw new BusinessRuleException("Discount quantities must be positive");
        }
        if (discount.getMinQuantity() > discount.getMaxQuantity()) {
            throw new BusinessRuleException("Minimum quantity cannot exceed maximum quantity");
        }
        if (discount.getDiscountPercentage() < 0 || discount.getDiscountPercentage() > 100) {
            throw new BusinessRuleException("Discount percentage must be between 0 and 100");
        }
    }
}
