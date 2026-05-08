package pe.edu.pucp.kingstore.service.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

import java.util.List;
import java.util.Optional;

public abstract class AbstractCrudService<T extends BaseEntity> implements CrudService<T> {

    private final JpaRepository<T, Integer> repository;
    private final String resourceName;

    protected AbstractCrudService(JpaRepository<T, Integer> repository, String resourceName) {
        this.repository = repository;
        this.resourceName = resourceName;
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findActive() {
        return repository.findAll().stream()
                .filter(entity -> Boolean.TRUE.equals(entity.getActive()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(Integer id) {
        requireId(id);
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public T getById(Integer id) {
        requireId(id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, id));
    }

    @Override
    @Transactional
    public T create(T entity) {
        requireEntity(entity);
        entity.setId(null);
        if (entity.getActive() == null) {
            entity.setActive(true);
        }
        validateForSave(entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T update(Integer id, T entity) {
        requireId(id);
        requireEntity(entity);
        T existing = getById(id);
        entity.setId(id);
        if (entity.getActive() == null) {
            entity.setActive(existing.getActive());
        }
        validateForSave(entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T deactivate(Integer id) {
        T entity = getById(id);
        entity.setActive(false);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T reactivate(Integer id) {
        T entity = getById(id);
        entity.setActive(true);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireId(id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(resourceName, id);
        }
        repository.deleteById(id);
    }

    protected void validateForSave(T entity) {
    }

    protected void requireId(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessRuleException(resourceName + " id must be a positive number");
        }
    }

    protected void requireEntity(T entity) {
        if (entity == null) {
            throw new BusinessRuleException(resourceName + " cannot be null");
        }
    }

    protected void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
    }
}
