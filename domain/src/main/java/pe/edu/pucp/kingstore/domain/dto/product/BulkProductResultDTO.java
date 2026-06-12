package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkProductResultDTO {
    private int productsCreated;
    private int variantsProcessed;
    private int imagesUploaded;
    private List<String> errors;
}