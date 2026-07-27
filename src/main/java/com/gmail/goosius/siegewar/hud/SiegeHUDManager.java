package com.gmail.goosius.siegewar.hud;

import java.util.HashMap;
import java.util.Map;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.SiegeWar;
import com.gmail.goosius.siegewar.events.SiegeEndEvent;
import com.gmail.goosius.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.huds.providers.FoliaHUD;
import com.palmergames.bukkit.towny.huds.providers.HUD;
import com.palmergames.bukkit.towny.huds.providers.PaperHUD;
import com.palmergames.bukkit.towny.huds.providers.ServerHUD;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SiegeHUDManager implements Listener {

	private static final String SIEGE_WAR_HUD_NAME = "siegeWarHUD";
	private static final String SIEGE_WAR_HUD_OBJ = "SIEGE_HUD_OBJ";
	static Map<Player, Siege> warHudUsers;

    public SiegeHUDManager() {
		boolean isFolia = SiegeWar.isFoliaClassPresent();

		HUD siegeWarHUD = new HUD(SIEGE_WAR_HUD_NAME, SIEGE_WAR_HUD_OBJ, (p) -> SiegeWarHud.updateHUD(p), (p, siege) -> SiegeWarHud.updateHUD(p, (Siege) siege));
		SiegeWarHud siegeHUD = new SiegeWarHud(siegeWarHUD);
		HUDManager.addHUD(SIEGE_WAR_HUD_NAME, isFolia ? new FoliaHUD(siegeHUD) : new PaperHUD(siegeHUD));
        warHudUsers = new HashMap<>();
    }

    public void toggleWarHud(Player player, Siege siege) {
		ServerHUD hud = HUDManager.getHUD(SIEGE_WAR_HUD_NAME);
		if (hud == null)
			return;

        if (!warHudUsers.containsKey(player)) {
            warHudUsers.put(player, siege);
			hud.toggleOn(player);
			SiegeWarHud.updateHUD(player, siege);
        } else if (warHudUsers.get(player) != siege) {
            warHudUsers.replace(player, siege);
			hud.toggleOn(player);
			SiegeWarHud.updateHUD(player, siege);
        } else
            toggleOff(player);
    }

    public static void toggleOff(Player player) {
        warHudUsers.remove(player);
        ServerHUD hud = HUDManager.getHUD(SIEGE_WAR_HUD_NAME);
        if (hud != null)
            hud.toggleOff(player);
    }

	public static void updateHUDs() {
		ServerHUD hud = HUDManager.getHUD(SIEGE_WAR_HUD_NAME);
		if (hud == null)
			return;

		for (Player player : hud.getPlayers()) {
			if (!hud.isActive(player)) {
				hud.removePlayer(player);
				warHudUsers.remove(player);
			} else if (!isDisplayable(warHudUsers.get(player))) {
				toggleOff(player);
			} else
				SiegeWarHud.updateHUD(player, warHudUsers.get(player));
		}
	}

	static boolean isDisplayable(Siege siege) {
		return siege != null && siege.getStatus().isActive() && SiegeController.hasSiege(siege.getTown());
	}

	@EventHandler
	public void onSiegeEnd(SiegeEndEvent event) {
		for (Map.Entry<Player, Siege> entry : Map.copyOf(warHudUsers).entrySet()) {
			if (entry.getValue() == event.getSiege())
				toggleOff(entry.getKey());
		}
	}

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        warHudUsers.remove(event.getPlayer());
    }

    public static String checkLength(String string) {
        return string.length() > 32 ? string.substring(0, 32) + "..." : string;
    }
}
