package pe.edu.pucp.kingstore.service.store;

import jakarta.transaction.TransactionScoped;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.store.StoreCategoryDTO;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;

@Service
public class StoreCategoryService extends AbstractCrudService<StoreCategory> {

    private final StoreCategoryRepository categoryRepository;

    public StoreCategoryService(StoreCategoryRepository categoryRepository) {
        super(categoryRepository, "StoreCategory");
        this.categoryRepository = categoryRepository;
    }

    @Override
    protected void validateForSave(StoreCategory category) {
        if (category.getStoreCategoryName() == null || category.getStoreCategoryName().isBlank())
            throw new BusinessRuleException("Category name is required");

        String normalizedName = category.getStoreCategoryName().trim();
        category.setStoreCategoryName(normalizedName);

        categoryRepository.findByStoreCategoryNameIgnoreCase(normalizedName)
                .filter(existing -> category.getId() == null || !existing.getId().equals(category.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Category already exists: " + normalizedName);
                });
    }

    @Transactional(readOnly = true)
    public List<StoreCategory> search(String term) {
        if (term == null || term.isBlank()) return categoryRepository.findAll();
        return categoryRepository.findAll().stream()
                .filter(c -> c.getStoreCategoryName().toLowerCase().contains(term.toLowerCase()))
                .toList();
    }

    @Transactional
    public StoreCategory createFromDTO(StoreCategoryDTO dto) {
        if (dto == null || dto.getStoreCategoryName() == null || dto.getStoreCategoryName().isBlank()) {
            throw new BusinessRuleException("Category name is required");
        }
        StoreCategory cat = new StoreCategory();
        cat.setStoreCategoryName(dto.getStoreCategoryName());
        return create(cat);
    }

    @Transactional
    public StoreCategory updateFromDTO(Integer id, StoreCategoryDTO dto) {
        if (dto == null || dto.getStoreCategoryName() == null || dto.getStoreCategoryName().isBlank()) {
            throw new BusinessRuleException("Category name is required");
        }
        StoreCategory cat = getById(id);
        cat.setStoreCategoryName(dto.getStoreCategoryName());
        return update(id, cat);
    }
    @Override
    @TransactionScoped
    public List<StoreCategory> findActive(){
        return super.findActive().stream()
                .map(StoreCategory.class::cast)
                .toList();
    }
}
