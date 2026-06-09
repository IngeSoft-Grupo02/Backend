package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.Data;

/**
 * Representa una fila del CSV de carga masiva de comerciantes.
 *
 * Columnas esperadas (exactas, sensibles a mayÃºsculas):
 *   email, password, firstName, paternalSurname, maternalSurname,
 *   documentType (DNI|PASSPORT|FOREIGN_ID_CARD),
 *   documentNumber, birthDate (yyyy-MM-dd), phone, gender (MALE|FEMALE|NOT_SPECIFIED),
 *   ruc, storeId (opcional â€“ id numÃ©rico de tienda ya existente)
 */
@Data
public class BulkMerchantRowDTO {
    private int rowNumber;          // nro de fila en el CSV (para reporte de incidencias)
    private String email;
    private String password;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private String documentType;
    private String documentNumber;
    private String birthDate;       // yyyy-MM-dd
    private String phone;
    private String gender;
    private String ruc;
    private String storeId;         // opcional
}
