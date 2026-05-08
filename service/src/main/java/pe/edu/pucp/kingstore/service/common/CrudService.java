package pe.edu.pucp.kingstore.service.common;

import java.util.List;
import java.util.Optional;

public interface CrudService<T> {

    List<T> findAll();

    List<T> findActive();

    Optional<T> findById(Integer id);

    T getById(Integer id);

    T create(T entity);

    T update(Integer id, T entity);

    T deactivate(Integer id);

    T reactivate(Integer id);

    void delete(Integer id);
}
