package pe.edu.pucp.kingstore.repository.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;


import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository
    extends JpaRepository<Quotation, Integer> {

    Optional<Quotation> findByShoppingCartId(Integer shoppingCartId);
    List<Quotation> findByStatus (QuotationStatus status);

}
