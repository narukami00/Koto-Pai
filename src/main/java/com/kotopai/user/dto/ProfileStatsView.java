package com.kotopai.user.dto;

import java.math.BigDecimal;

/**
 * View model projection for user profile activity statistics.
 */
public record ProfileStatsView(
        int outingsCount,
        int friendsCount,
        BigDecimal settledAmount
) {
    public String getFormattedSettledAmount() {
        BigDecimal amount = settledAmount != null ? settledAmount : BigDecimal.ZERO;
        return "৳" + String.format("%,.2f", amount);
    }
}

