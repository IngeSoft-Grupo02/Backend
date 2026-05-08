package pe.edu.pucp.kingstore.domain.dto.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.order.OrderDTO;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentReceiptDTO extends BaseEntityDTO {
    private Integer orderId;
    private LocalDate issueDate;
    private String ruc;
    private Double totalWithoutTax;
    private Double taxes;
    private Double subTotal;
    private Double finalTotal;
    private PaymentMethod paymentMethod;
}
