package pe.edu.pucp.kingstore.repository.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationDesign;

import java.util.List;

@Repository
public interface QuotationDesignRepository extends JpaRepository<QuotationDesign, Integer> {
    List<QuotationDesign> findByQuotationId(Integer quotationId);
}
