package pe.edu.pucp.kingstore.domain.model.quotation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "quotation")
public class Quotation extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "quotation_id", nullable = false)
    private List<QuotationItem> items;

    @Column(nullable = false)
    private double subTotal;
    @Column(nullable = false)
    private double discount;
    @Column(nullable = false)
    private double totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status;
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    @Column
    private LocalDateTime responseAt;
    @Column(length = 500)
    private String description;
    @Column(length = 500)
    private String observations;
    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
    }
}
