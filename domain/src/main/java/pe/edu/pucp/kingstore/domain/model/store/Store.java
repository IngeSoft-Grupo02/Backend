package pe.edu.pucp.kingstore.domain.model.store;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.store.enums.ColorPalette;
import pe.edu.pucp.kingstore.domain.model.store.enums.CustomerGender;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table (name = "store")
public class Store extends BaseEntity {
    @Column (nullable = false, length = 100)
    private String storeName;

    @Column (nullable = false, unique = true,length = 100)
    private String slug;

    @Column (length = 255)
    private String description;

    @Column (length = 255)
    private String logoUrl;

    @Column (length = 7)
    private String primaryColor;

    @Column (length = 7)
    private String secondaryColor;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "store_category", joinColumns = @JoinColumn(name = "store_id")
    )
    @Column (name = "category")
    private List<StoreCategory> categories;


    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "store_gender", joinColumns = @JoinColumn(name = "store_id")
    )
    @Column(name = "gender")
    private List<CustomerGender> genders;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorPalette colorPalette;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus storeStatus;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;  // un comerciante por tienda

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
