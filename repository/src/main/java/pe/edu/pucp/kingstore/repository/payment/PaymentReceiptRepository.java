package pe.edu.pucp.kingstore.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;

import java.util.Optional;

@Repository
public interface PaymentReceiptRepository
    extends JpaRepository<PaymentReceipt, Integer> {

    Optional<PaymentReceipt> findByOrderId(Integer orderId);
}
