package pe.edu.pucp.kingstore.domain.dto;

import lombok.Data;

@Data
public abstract class BaseEntityDTO {
    private Integer id;
    private Boolean active = true;
}
