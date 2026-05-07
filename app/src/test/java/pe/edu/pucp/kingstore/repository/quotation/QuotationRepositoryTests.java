package pe.edu.pucp.kingstore.repository.quotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
public class QuotationRepositoryTests {
    @Autowired
    QuotationRepository underTest;

}
