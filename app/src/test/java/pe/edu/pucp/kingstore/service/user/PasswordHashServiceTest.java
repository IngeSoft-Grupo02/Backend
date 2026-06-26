package pe.edu.pucp.kingstore.service.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashServiceTest {

    private final PasswordHashService service = new PasswordHashService();

    @Test
    void hashesRawPasswordAndMatchesIt() {
        String hashed = service.hash("ClaveSegura1*");

        assertThat(hashed)
                .startsWith("$2")
                .isNotEqualTo("ClaveSegura1*");
        assertThat(service.isHashed(hashed)).isTrue();
        assertThat(service.matches("ClaveSegura1*", hashed)).isTrue();
        assertThat(service.matches("OtraClave1*", hashed)).isFalse();
    }

    @Test
    void keepsExistingHashWithoutHashingAgain() {
        String hashed = service.hash("ClaveSegura1*");

        assertThat(service.hash(hashed)).isSameAs(hashed);
    }

    @Test
    void supportsLegacyPlainTextPasswordsDuringMigration() {
        assertThat(service.matches("legacy-pass", "legacy-pass")).isTrue();
        assertThat(service.matches("wrong", "legacy-pass")).isFalse();
        assertThat(service.matches(null, "legacy-pass")).isFalse();
        assertThat(service.matches("anything", null)).isFalse();
        assertThat(service.hash(null)).isNull();
        assertThat(service.isHashed("legacy-pass")).isFalse();
        assertThat(service.isHashed(null)).isFalse();
    }
}
