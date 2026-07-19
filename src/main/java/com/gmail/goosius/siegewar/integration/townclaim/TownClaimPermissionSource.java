package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

final class TownClaimPermissionSource extends TownyPermissionSource {
    @Override
    public boolean testPermission(Permissible permissible, String node) {
        return permissible instanceof Player player
                ? TownClaimPermissions.test(player, node)
                : permissible != null && permissible.hasPermission(node);
    }

    @Override
    public boolean isTownyAdmin(Permissible permissible) {
        return permissible != null && (permissible.isOp() || permissible.hasPermission("towny.admin"));
    }

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
