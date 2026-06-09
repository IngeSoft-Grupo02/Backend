package pe.edu.pucp.kingstore.domain.model.audit;

import jakarta.persistence.*;
        import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 150)
    private String userEmail;

    @Column(length = 50)
    private String role;

    @Column(length = 50)
    private String tenantSlug;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 255)
    private String endpoint;

    private Integer statusCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditLevel level;

    @Column(length = 500)
    private String description;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}