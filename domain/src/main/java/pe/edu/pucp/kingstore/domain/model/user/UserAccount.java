package pe.edu.pucp.kingstore.domain.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;
}