package pe.edu.pucp.kingstore.domain.model.product;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "custom_design")
public class CustomDesign extends BaseEntity {

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String observations;

    @Column(name = "overlay_x")
    private Double overlayX;

    @Column(name = "overlay_y")
    private Double overlayY;

    @Column(name = "overlay_width")
    private Double overlayWidth;

    @Column(name = "overlay_height")
    private Double overlayHeight;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column
    private LocalDateTime reviewedAt;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}
