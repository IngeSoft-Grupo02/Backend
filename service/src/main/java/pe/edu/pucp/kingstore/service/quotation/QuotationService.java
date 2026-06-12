package pe.edu.pucp.kingstore.service.quotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationItemResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseDTO;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class QuotationService extends AbstractCrudService<Quotation> {

    private final QuotationRepository quotationRepository;

    public QuotationService(QuotationRepository quotationRepository) {
        super(quotationRepository, "Quotation");
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Quotation> findByShoppingCart(Integer shoppingCartId) {
        requireId(shoppingCartId);
        return quotationRepository.findByShoppingCartId(shoppingCartId);
    }

    @Transactional(readOnly = true)
    public List<Quotation> findByStatus(QuotationStatus status) {
        if (status == null) {
            throw new BusinessRuleException("Quotation status is required");
        }
        return quotationRepository.findByStatus(status);
    }

    @Transactional
    public Quotation respond(Integer id, QuotationStatus status, String observations) {
        if (status == null || status == QuotationStatus.PENDING) {
            throw new BusinessRuleException("Quotation response must approve or reject the quotation");
        }
        if (status == QuotationStatus.REJECTED && (observations == null || observations.isBlank())) {
            throw new BusinessRuleException("Observations are required when rejecting a quotation");
        }
        Quotation quotation = getById(id);
        quotation.setStatus(status);
        quotation.setObservations(observations);
        quotation.setResponseAt(LocalDateTime.now());
        return quotationRepository.save(quotation);
    }
    @Transactional(readOnly = true)
    public List<Quotation> findByStoreId(Integer storeId) {
        requireId(storeId);
        return quotationRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Quotation> findByStoreIdAndStatus(Integer storeId, QuotationStatus status) {
        requireId(storeId);
        if (status == null) {
            throw new BusinessRuleException("Quotation status is required");
        }
        return quotationRepository.findByStoreIdAndStatus(storeId, status);
    }

    @Transactional(readOnly = true)
    public Quotation findInStore(Integer quotationId, Integer storeId) {
        requireId(quotationId);
        requireId(storeId);
        return findByStoreId(storeId).stream()
                .filter(q -> Objects.equals(q.getId(), quotationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
    }
    @Override
    protected void validateForSave(Quotation quotation) {
        if (quotation.getShoppingCart() == null || quotation.getShoppingCart().getId() == null) {
            throw new BusinessRuleException("Quotation must belong to a shopping cart");
        }
        if (quotation.getStatus() == null) {
            quotation.setStatus(QuotationStatus.PENDING);
        }
        recalculateTotals(quotation);
    }

    private void recalculateTotals(Quotation quotation) {
        double subTotal = 0;
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Quotation item quantity must be positive");
                }
                if (item.getPrice() < 0) {
                    throw new BusinessRuleException("Quotation item price cannot be negative");
                }
                item.setSubTotal(item.getPrice() * item.getQuantity());
                subTotal += item.getSubTotal();
            }
        }

        if (quotation.getDiscount() < 0 || quotation.getDiscount() > subTotal) {
            throw new BusinessRuleException("Quotation discount must be between zero and subtotal");
        }
        quotation.setSubTotal(subTotal);
        quotation.setTotalAmount(subTotal - quotation.getDiscount());
    }

    @Transactional(readOnly = true)
    public QuotationResponseDTO toResponseDTO(Quotation quotation, Integer storeId) {
        var customer = quotation.getShoppingCart() != null
                ? quotation.getShoppingCart().getCustomer()
                : null;

        List<QuotationItemResponseDTO> items = quotation.getItems() == null
                ? List.of()
                : quotation.getItems().stream()
                  .map(item -> new QuotationItemResponseDTO(
                          "Producto",
                          item.getProductVariant() != null
                          ? item.getProductVariant().getSize() + " / "
                            + item.getProductVariant().getColor().name()
                          : null,
                          item.getQuantity(),
                          item.getPrice(),
                          item.getSubTotal()
                  ))
                  .toList();

        return new QuotationResponseDTO(
                quotation.getId(),
                MerchantCustomerUtil.customerName(customer),
                quotation.getStatus(),
                switch (quotation.getStatus()) {
                    case PENDING  -> "Pendiente";
                    case APPROVED -> "Aprobada";
                    case REJECTED -> "Rechazada";
                },
                quotation.getSubTotal(),
                quotation.getDiscount(),
                quotation.getTotalAmount(),
                quotation.getRequestedAt(),
                quotation.getDescription(),
                quotation.getObservations(),
                storeId,
                items
        );
    }
}
