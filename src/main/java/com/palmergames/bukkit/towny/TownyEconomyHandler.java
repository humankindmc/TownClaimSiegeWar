package com.palmergames.bukkit.towny;

import java.text.NumberFormat;

public final class TownyEconomyHandler {
    private TownyEconomyHandler() {
    }

    public static boolean isActive() {
        // ponytail: TownClaim's treasury locator is still a no-op; enable its bridge when real treasuries land.
        return false;
    }

    public static String getFormattedBalance(double amount) {
        return NumberFormat.getNumberInstance().format(amount);
    }
}
