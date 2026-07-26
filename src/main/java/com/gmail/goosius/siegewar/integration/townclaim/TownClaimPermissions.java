package com.gmail.goosius.siegewar.integration.townclaim;

import com.humankindmc.claims.nation.NationService;
import com.humankindmc.claims.town.TownService;
import com.humankindmc.claims.town.permission.TownPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class TownClaimPermissions {
    private TownClaimPermissions() {
    }

    public static boolean test(Player player, String node) {
        if (player == null || node == null) {
            return false;
        }
        if (player.hasPermission(node)) {
            return true;
        }
        TownService towns = service(TownService.class);
        if (towns == null) {
            return false;
        }
        if (node.startsWith("siegewar.town.siege.") || node.startsWith("siegewar.command.siegewar.town")) {
            return towns.getTownForPlayer(player.getUniqueId())
                    .map(town -> towns.hasTownPermission(player.getUniqueId(), town.id(), TownPermission.EDIT_TOWN_SETTINGS))
                    .orElse(false);
        }
        if (node.startsWith("siegewar.nation.siege.") || node.startsWith("siegewar.command.siegewar.nation")) {
            NationService nations = service(NationService.class);
            return nations != null && towns.getTownForPlayer(player.getUniqueId())
                    .flatMap(town -> nations.nationForTown(town.id()))
                    .map(nation -> nations.canManageNation(player.getUniqueId(), nation)).orElse(false);
        }
        return false;
    }

    private static <T> T service(Class<T> type) {
        RegisteredServiceProvider<T> registration = Bukkit.getServicesManager().getRegistration(type);
        return registration == null ? null : registration.getProvider();
    }
}
