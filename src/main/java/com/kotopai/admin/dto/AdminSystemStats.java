package com.kotopai.admin.dto;

import java.math.BigDecimal;

/**
 * High-level system-wide metrics for administrator oversight.
 */
public record AdminSystemStats(
        long totalUsers,
        long totalOutings,
        long totalFriendships,
        long totalNotifications,
        BigDecimal totalSettledVolume
) {
    public String getFormattedSettledVolume() {
        BigDecimal volume = totalSettledVolume != null ? totalSettledVolume : BigDecimal.ZERO;
        return "৳" + String.format("%,.2f", volume);
    }
}

