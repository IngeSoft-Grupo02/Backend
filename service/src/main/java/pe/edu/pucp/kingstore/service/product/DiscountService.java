package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil;

import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
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
    @Transactional(readOnly = true)
    public List<Discount> findByStoreId(Integer storeId) {
        requireId(storeId);
        return discountRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public Discount findInStore(Integer discountId, Integer storeId) {
        requireId(discountId);
        requireId(storeId);
        Discount discount = getById(discountId);
        if (isDeleted(discount)) {
            throw new ResourceNotFoundException("Discount", discountId);
        }
        Integer discountStoreId = discount.getStore() != null
                ? discount.getStore().getId()
                : discount.getProduct() != null && discount.getProduct().getStore() != null
                  ? discount.getProduct().getStore().getId()
                  : null;
        if (!Objects.equals(discountStoreId, storeId)) {
            throw new ResourceNotFoundException("Discount", discountId);
        }
        return discount;
    }

    @Transactional
    public Discount createForStore(Store store, Product product, DiscountRequestDTO request) {
        enforceDiscountLimit(store.getId(), null);
        Discount discount = new Discount();
        discount.setStore(store);
        discount.setProduct(product);
        applyRequest(discount, request);
        discount.setActive(resolveActive(request));
        return create(discount);
    }

    @Transactional
    public Discount updateForStore(Discount discount, Store store, Product product,
                                   DiscountRequestDTO request) {
        discount.setStore(store);
        discount.setProduct(product);
        applyRequest(discount, request);
        discount.setActive(resolveActive(request));
        return discountRepository.save(discount);
    }
    // OBSERVACION /////////////
    @Override
    @Transactional
    public Discount deactivate(Integer discountId) {
        Discount discount = getById(discountId);
        discount.setActive(false);
        return discountRepository.save(discount);
    }

    @Transactional
    public Discount markDeleted(Integer discountId) {
        Discount discount = getById(discountId);
        discount.setDeleted(true);
        discount.setDeletedAt(LocalDateTime.now());
        return discountRepository.save(discount);
    }

    public DiscountResponseDTO toResponseDTO(Discount discount) {
        Product product = discount.getProduct();
        Integer storeId = discount.getStore() != null
                ? discount.getStore().getId()
                : product != null && product.getStore() != null
                  ? product.getStore().getId()
                  : null;
        return new DiscountResponseDTO(
                discount.getId(),
                product != null ? product.getId() : null,
                discount.getVolumeType(),
                discount.getMinQuantity(),
                discount.getMaxQuantity(),
                discount.getDiscountPercentage(),
                discount.getActive(),
                Boolean.TRUE.equals(discount.getActive()) ? "Activa" : "Pausada",
                storeId,
                product != null ? product.getName() : null,
                discount.getName(),
                discount.getDiscountType(),
                discount.getDiscountPercentage(),
                discount.getMinQuantity(),
                discount.getUsageCount(), //!= null ? discount.getUsageCount() : 0, //  OBSERVACION
                discount.getAppliesTo()
        );
    }

    private void enforceDiscountLimit(Integer storeId, Integer currentDiscountId) {
        long count = discountRepository.findByStoreId(storeId).stream()
                .filter(d -> !isDeleted(d))
                .filter(d -> currentDiscountId == null
                        || !Objects.equals(d.getId(), currentDiscountId))
                .count();
        if (count >= 5) {
            throw new BusinessRuleException("A store can only have up to 5 discounts");
        }
    }

    private void applyRequest(Discount discount, DiscountRequestDTO request) {
        if (request == null) throw new BusinessRuleException("Discount request is required");
        Integer minQuantity = request.getMinQuantity();
        Integer maxQuantity = request.getMaxQuantity() == null
                ? minQuantity : request.getMaxQuantity();
        Double value = request.getDiscountPercentage();
        if (minQuantity == null || minQuantity <= 0) {
            throw new BusinessRuleException("Minimum quantity must be positive");
        }
        if (maxQuantity == null || maxQuantity < minQuantity) {
            throw new BusinessRuleException("Maximum quantity must be >= minimum quantity");
        }
        if (value == null || value < 0) {
            throw new BusinessRuleException("Discount value must be zero or greater");
        }
        if (isPercentage(request) && value > 100) {
            throw new BusinessRuleException("Discount percentage must be between 0 and 100");
        }
        String appliesTo = MerchantStringUtil.blankToNull(request.getAppliesTo()) == null
                ? "Todo el catalogo" : request.getAppliesTo().trim();
        String normalized = normalize(appliesTo);
        if (normalized.contains("categoria")) {
            throw new BusinessRuleException("Category discounts are not supported");
        }
        appliesTo = normalized.contains("producto") ? "Producto especifico" : "Todo el catalogo";
        if (normalized.contains("producto") && discount.getProduct() == null) {
            throw new BusinessRuleException("Product is required for product discounts");
        }
        discount.setName(MerchantStringUtil.blankToNull(request.getName()));
        discount.setDiscountType(MerchantStringUtil.blankToNull(request.getType()) == null
                ? "Porcentaje" : request.getType().trim());
        discount.setAppliesTo(appliesTo);
        discount.setUsageCount(request.getUsageCount() == null
                ? discount.getUsageCount() : request.getUsageCount());
        discount.setVolumeType(request.getVolumeType() == null
                ? VolumeType.UNIT : request.getVolumeType());
        discount.setMinQuantity(minQuantity);
        discount.setMaxQuantity(maxQuantity);
        discount.setDiscountPercentage(value);
    }

    private Boolean resolveActive(DiscountRequestDTO request) {
        if (request.getActive() != null) return request.getActive();
        if (request.getStatus() == null || request.getStatus().isBlank()) return true;
        String normalized = request.getStatus().trim().toLowerCase();
        return normalized.equals("activa") || normalized.equals("activo")
                || normalized.equals("active");
    }

    private boolean isPercentage(DiscountRequestDTO request) {
        return request.getType() == null || request.getType().isBlank()
                || request.getType().equalsIgnoreCase("Porcentaje")
                || request.getType().equalsIgnoreCase("PERCENTAGE");
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean isDeleted(Discount discount) {
        return Boolean.TRUE.equals(discount.getDeleted());
    }

    @Override
    protected void validateForSave(Discount discount) {
        boolean hasStore = discount.getStore() != null && discount.getStore().getId() != null;
        boolean hasProduct = discount.getProduct() != null && discount.getProduct().getId() != null;
        if (!hasStore && !hasProduct) {
            throw new BusinessRuleException("Discount must belong to a store or product");
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
