package pe.edu.pucp.kingstore.service.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class PasswordHashService {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        if (rawPassword == null || isHashed(rawPassword)) {
            return rawPassword;
        }
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            return rawPassword != null && encoder.matches(rawPassword, storedPassword);
        }
        return Objects.equals(storedPassword, rawPassword);
    }

    public boolean isHashed(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }
}
