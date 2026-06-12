package pe.edu.pucp.kingstore.domain.dto.store;

import java.util.List;

public record DashboardResponse(
        long pendingOrders,
        long pendingQuotes,
        long drafts,
        List<RecentOrderResponse> recentOrders
) {}