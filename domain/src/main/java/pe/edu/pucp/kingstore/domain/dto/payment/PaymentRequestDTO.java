package pe.edu.pucp.kingstore.domain.dto.payment;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;

/**
 * DTO de entrada para el pago simulado.
 * El cliente elige el método de pago y proporciona su RUC.
 */
@Data
public class PaymentRequestDTO {
    private String ruc;
    private PaymentMethod paymentMethod;
    private String receiptType;
    // Campos de tarjeta — solo para simulación, no se guardan
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;
}
