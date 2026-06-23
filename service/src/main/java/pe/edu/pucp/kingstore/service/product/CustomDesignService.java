package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.repository.product.CustomDesignRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CustomDesignService extends AbstractCrudService<CustomDesign> {

    private final CustomDesignRepository customDesignRepository;

    public CustomDesignService(CustomDesignRepository customDesignRepository) {
        super(customDesignRepository, "Custom design");
        this.customDesignRepository = customDesignRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomDesign> findByStore(Integer storeId) {
        requireId(storeId);
        return customDesignRepository.findByProduct_Store_Id(storeId);
    }

    @Transactional(readOnly = true)
    public CustomDesign findInStore(Integer designId, Integer storeId) {
        requireId(designId);
        requireId(storeId);
        CustomDesign design = getById(designId);
        if (design.getProduct() == null
                || !Objects.equals(design.getProduct().getStore().getId(), storeId)) {
            throw new ResourceNotFoundException("Custom design", designId);
        }
        return design;
    }

    /**
     * El merchant aprueba un diseño personalizado.
     */
    @Transactional
    public CustomDesign approve(Integer designId, Integer storeId) {
        CustomDesign design = findInStore(designId, storeId);
        if (design.getReviewedAt() != null) {
            throw new BusinessRuleException("Design has already been reviewed");
        }
        design.setObservations("APPROVED");
        design.setReviewedAt(LocalDateTime.now());
        return customDesignRepository.save(design);
    }

    /**
     * El merchant rechaza un diseño con observaciones obligatorias.
     */
    @Transactional
    public CustomDesign reject(Integer designId, Integer storeId, String observations) {
        if (observations == null || observations.isBlank()) {
            throw new BusinessRuleException("Observations are required when rejecting a design");
        }
        CustomDesign design = findInStore(designId, storeId);
        if (design.getReviewedAt() != null) {
            throw new BusinessRuleException("Design has already been reviewed");
        }
        design.setObservations(observations);
        design.setReviewedAt(LocalDateTime.now());
        return customDesignRepository.save(design);
    }

    @Override
    protected void validateForSave(CustomDesign design) {
        if (design.getProduct() == null || design.getProduct().getId() == null) {
            throw new BusinessRuleException("Custom design must belong to a product");
        }
    }
}