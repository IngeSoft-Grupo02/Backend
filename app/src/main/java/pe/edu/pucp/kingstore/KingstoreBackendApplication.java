package pe.edu.pucp.kingstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "pe.edu.pucp.kingstore")
@EnableScheduling
public class KingstoreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KingstoreBackendApplication.class, args);
	}

}
