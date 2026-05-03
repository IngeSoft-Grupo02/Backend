package pe.edu.pucp.kingstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "pe.edu.pucp.kingstore")
public class KingstoreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KingstoreBackendApplication.class, args);
	}

}
