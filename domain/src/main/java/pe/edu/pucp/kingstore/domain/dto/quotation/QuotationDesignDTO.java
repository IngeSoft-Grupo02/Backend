package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QuotationDesignDTO {
    private Integer id;
    private String fileName;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private Integer quotationItemId;
    private Double overlayX;
    private Double overlayY;
    private Double overlayWidth;
    private Double overlayHeight;

    public QuotationDesignDTO(Integer id,
                              String fileName,
                              String url,
                              String contentType,
                              Long sizeBytes,
                              Integer quotationItemId) {
        this(id, fileName, url, contentType, sizeBytes, quotationItemId, null, null, null, null);
    }

    public QuotationDesignDTO(Integer id,
                              String fileName,
                              String url,
                              String contentType,
                              Long sizeBytes,
                              Integer quotationItemId,
                              Double overlayX,
                              Double overlayY,
                              Double overlayWidth,
                              Double overlayHeight) {
        this.id = id;
        this.fileName = fileName;
        this.url = url;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.quotationItemId = quotationItemId;
        this.overlayX = overlayX;
        this.overlayY = overlayY;
        this.overlayWidth = overlayWidth;
        this.overlayHeight = overlayHeight;
    }
}
