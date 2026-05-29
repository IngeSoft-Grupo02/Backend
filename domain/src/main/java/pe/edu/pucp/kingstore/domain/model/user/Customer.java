package pe.edu.pucp.kingstore.domain.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.store.Store;

@Getter
@Setter
@Entity
@PrimaryKeyJoinColumn(name = "person_id")
public class Customer extends Person {

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

}