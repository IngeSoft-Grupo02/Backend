package pe.edu.pucp.kingstore.domain.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.store.Store;

@Getter
@Setter
@Entity
@Table(name = "customer", uniqueConstraints = @UniqueConstraint(
        name = "uk_customer_account_store", columnNames = {"user_account_id", "store_id"}))
@PrimaryKeyJoinColumn(name = "person_id")
public class Customer extends Person {

    // Un mismo UserAccount (correo global único) puede tener varios Customer: uno por
    // tienda (membresías por tienda). La unicidad por tienda la da uk_customer_account_store.
    @ManyToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

}