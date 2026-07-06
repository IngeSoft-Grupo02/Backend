package pe.edu.pucp.kingstore.domain.model.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.order.enums.District;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "shipping_detail")
public class ShippingDetail extends BaseEntity {

    @Column(length = 500)
    private String description;

    @Column
    private LocalDate estimatedDeliveryDate;

    @Column
    private LocalDate actualDeliveryDate;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private District district;

    @Column(length = 150)
    private String recipientName;

    @Column(length = 20)
    private String phone;
}
