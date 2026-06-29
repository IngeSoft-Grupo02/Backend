package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotationDesignDTO {
    private Integer id;
    private String fileName;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private Integer quotationItemId;
}
