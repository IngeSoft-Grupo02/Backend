package pe.edu.pucp.kingstore.domain.model.store;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;

import java.time.LocalDateTime;
/* */
@NamedEntityGraph(
        name = "Store.withMerchantAndCategory",
        attributeNodes = {
                @NamedAttributeNode(value = "merchant", subgraph = "merchant-subgraph"),
                @NamedAttributeNode("category")
        },
        subgraphs = {
                @NamedSubgraph(
                name = "merchant-subgraph",
                attributeNodes =  @NamedAttributeNode("userAccount")
                )
        }
)
@Getter
@Setter
@Entity
@Table(name = "store")
public class Store extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String logoUrl;

    @Column(nullable = false)
    private Double designFeePercentage = 10.0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "store_category_id", nullable = false)
    private StoreCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrimaryColor primaryColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecondaryColor secondaryColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TertiaryColor tertiaryColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus storeStatus;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.designFeePercentage == null) {
            this.designFeePercentage = 10.0;
        }
    }
}
