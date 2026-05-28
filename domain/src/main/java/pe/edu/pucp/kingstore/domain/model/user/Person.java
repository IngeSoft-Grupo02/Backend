    package pe.edu.pucp.kingstore.domain.model.user;

    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.Setter;
    import pe.edu.pucp.kingstore.domain.model.BaseEntity;
    import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
    import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
    import java.time.LocalDate;

    @Getter
    @Setter
    @Entity
    @Table(name = "person")
    @Inheritance(strategy = InheritanceType.JOINED)
    public class Person extends BaseEntity {

        @Column(nullable = false, length = 20)
        private String documentNumber;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private DocumentType documentType;

        @Column(nullable = false, length = 100)
        private String firstName;

        @Column(nullable = false, length = 100)
        private String paternalSurname;

        @Column(nullable = false, length = 100)
        private String maternalSurname;

        @Column(nullable = false)
        private LocalDate birthDate;

        @Column(length = 15)
        private String phone;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Gender gender;
    }