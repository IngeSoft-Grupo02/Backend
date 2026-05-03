package pe.edu.pucp.kingstore.domain.model.payment;


import jakarta.persistence.*;
        import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.order.Order;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "payment_receipt")
public class PaymentReceipt extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false, length = 20)
    private String ruc;

    @Column(nullable = false)
    private Double totalWithoutTax;

    @Column(nullable = false)
    private Double taxes;

    @Column(nullable = false)
    private Double subTotal;

    @Column(nullable = false)
    private Double finalTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @PrePersist
    protected void onCreate() {
        this.issueDate = LocalDate.now();
    }
}