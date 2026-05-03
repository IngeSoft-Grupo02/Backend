package pe.edu.pucp.kingstore.domain.model.user;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@PrimaryKeyJoinColumn(name = "person_id")
public class SystemAdministrator extends Person {

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(nullable = false, length = 100)
    private String position;
}