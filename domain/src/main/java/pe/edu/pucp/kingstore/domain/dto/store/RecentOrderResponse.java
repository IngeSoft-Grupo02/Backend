package pe.edu.pucp.kingstore.domain.dto.store;

import java.time.LocalDateTime;

public record RecentOrderResponse(
        Integer id,
        String customer,
        String status,
        Double total,
        LocalDateTime createdAt,
        Integer storeId
) {}