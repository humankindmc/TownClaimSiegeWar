package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import org.bukkit.entity.Player;

final class TownClaimPermissionSource extends TownyPermissionSource {
    @Override
    public String getPrefixSuffix(Resident resident, String node) {
        return "";
    }

    @Override
    public int getGroupPermissionIntNode(String group, String node) {
        return 0;
    }

    @Override
    public int getPlayerPermissionIntNode(String player, String node) {
        return 0;
    }

    @Override
    public String getPlayerGroup(Player player) {
        return "";
    }

    @Override
    public String getPlayerPermissionStringNode(String player, String node) {
        return "";
    }
}
