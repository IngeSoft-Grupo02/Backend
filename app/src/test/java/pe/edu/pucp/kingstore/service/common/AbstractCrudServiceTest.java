package pe.edu.pucp.kingstore.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre todos los branches de AbstractCrudService.
 *
 * Usa una entidad mínima (TestEntity) y una implementación concreta mínima
 * (TestService) para no depender de ningún servicio de dominio real.
 * Los servicios hijos (ProductService, StoreService, etc.) NO repiten estos
 * tests — solo cubren sus propios métodos y su validateForSave.
 */
@ExtendWith(MockitoExtension.class)
class AbstractCrudServiceTest {

    // -------------------------------------------------------------------------
    // Entidad y servicio mínimos solo para este test
    // -------------------------------------------------------------------------

    static class TestEntity extends BaseEntity {}

    static class TestService extends AbstractCrudService<TestEntity> {
        TestService(JpaRepository<TestEntity, Integer> repository) {
            super(repository, "TestEntity");
        }
    }

    @Mock
    private JpaRepository<TestEntity, Integer> repository;

    private TestService service;

    @BeforeEach
    void setUp() {
        service = new TestService(repository);
    }

    private TestEntity entity() {
        return new TestEntity();
    }

    // =========================================================================
    // requireId
    // =========================================================================

    @Test
    void requireId_null_lanzaBusinessRuleException() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("id must be a positive number");
    }

    @Test
    void requireId_cero_lanzaBusinessRuleException() {
        assertThatThrownBy(() -> service.findById(0))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("id must be a positive number");
    }

    @Test
    void requireId_negativo_lanzaBusinessRuleException() {
        assertThatThrownBy(() -> service.findById(-1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("id must be a positive number");
    }

    // =========================================================================
    // requireEntity
    // =========================================================================

    @Test
    void requireEntity_null_lanzaBusinessRuleException() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be null");
    }

    // =========================================================================
    // requireText — ejercitado via subclase que lo invoca
    // =========================================================================

    @Test
    void requireText_null_lanzaBusinessRuleException() {
        // Subclase que llama a requireText para exponer el método protegido
        class TextService extends AbstractCrudService<TestEntity> {
            TextService() { super(repository, "TestEntity"); }
            void checkText(String value) { requireText(value, "campo"); }
        }
        TextService svc = new TextService();

        assertThatThrownBy(() -> svc.checkText(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("campo is required");
    }

    @Test
    void requireText_blank_lanzaBusinessRuleException() {
        class TextService extends AbstractCrudService<TestEntity> {
            TextService() { super(repository, "TestEntity"); }
            void checkText(String value) { requireText(value, "campo"); }
        }
        TextService svc = new TextService();

        assertThatThrownBy(() -> svc.checkText("   "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("campo is required");
    }

    // =========================================================================
    // create
    // =========================================================================

    @Test
    void create_activeNull_seteaTrue() {
        TestEntity e = entity();
        e.setActive(null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestEntity result = service.create(e);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void create_activeExplicito_seRespeta() {
        TestEntity e = entity();
        e.setActive(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestEntity result = service.create(e);

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void create_resetaIdANull() {
        TestEntity e = entity();
        e.setId(99);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(e);

        assertThat(e.getId()).isNull();
    }

    // =========================================================================
    // update
    // =========================================================================

    @Test
    void update_activeNull_heredaActivoDelExistente() {
        TestEntity existing = entity();
        existing.setId(5);
        existing.setActive(false);
        when(repository.findById(5)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestEntity incoming = entity();
        incoming.setActive(null);

        TestEntity result = service.update(5, incoming);

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void update_activeExplicito_seRespeta() {
        TestEntity existing = entity();
        existing.setId(5);
        existing.setActive(false);
        when(repository.findById(5)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestEntity incoming = entity();
        incoming.setActive(true);

        TestEntity result = service.update(5, incoming);

        assertThat(result.getActive()).isTrue();
    }

    // =========================================================================
    // getById
    // =========================================================================

    @Test
    void getById_noEncontrado_lanzaResourceNotFoundException() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_encontrado_retornaEntidad() {
        TestEntity e = entity();
        e.setId(5);
        when(repository.findById(5)).thenReturn(Optional.of(e));

        assertThat(service.getById(5)).isSameAs(e);
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Test
    void delete_idInexistente_lanzaResourceNotFoundException() {
        when(repository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_idExistente_invocaDeleteById() {
        when(repository.existsById(5)).thenReturn(true);

        service.delete(5);

        verify(repository).deleteById(5);
    }

    // =========================================================================
    // findAll / findActive
    // =========================================================================

    @Test
    void findAll_retornaListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(entity(), entity()));

        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    void findActive_filtraSoloActivos() {
        TestEntity activo = entity();
        activo.setActive(true);
        TestEntity inactivo = entity();
        inactivo.setActive(false);
        when(repository.findAll()).thenReturn(List.of(activo, inactivo));

        assertThat(service.findActive()).containsExactly(activo);
    }

    @Test
    void findActive_listaVacia_retornaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findActive()).isEmpty();
    }

    // =========================================================================
    // deactivate / reactivate
    // =========================================================================

    @Test
    void deactivate_setaActiveFalse() {
        TestEntity e = entity();
        e.setId(5);
        when(repository.findById(5)).thenReturn(Optional.of(e));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.deactivate(5).getActive()).isFalse();
    }

    @Test
    void reactivate_setaActiveTrue() {
        TestEntity e = entity();
        e.setId(5);
        e.setActive(false);
        when(repository.findById(5)).thenReturn(Optional.of(e));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.reactivate(5).getActive()).isTrue();
    }
}