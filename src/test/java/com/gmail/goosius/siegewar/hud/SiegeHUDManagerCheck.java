package com.gmail.goosius.siegewar.hud;

import com.gmail.goosius.siegewar.SiegeController;
import com.gmail.goosius.siegewar.enums.SiegeStatus;
import com.gmail.goosius.siegewar.objects.Siege;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SiegeHUDManagerCheck {
	public static void main(String[] args) throws Exception {
		Field config = TownySettings.class.getDeclaredField("config");
		config.setAccessible(true);
		config.set(null, new CommentedConfiguration(Files.createTempFile("siegewar-hud", ".yml")));
		Field translations = Translation.class.getDeclaredField("translations");
		translations.setAccessible(true);
		translations.set(null, Map.of("en_US", new AbstractMap<String, String>() {
			@Override public String get(Object key) { return key.toString(); }
			@Override public Set<Entry<String, String>> entrySet() { return Set.of(); }
		}));

		Town town = new Town("HudCheck", UUID.randomUUID());
		Siege siege = new Siege(town);
		siege.setStatus(SiegeStatus.IN_PROGRESS);

		assert !SiegeHUDManager.isDisplayable(siege);
		SiegeController.putTownInSiegeMap(town, siege);
		assert SiegeHUDManager.isDisplayable(siege);
		siege.setStatus(SiegeStatus.UNKNOWN);
		assert !SiegeHUDManager.isDisplayable(siege);

		SiegeController.clearSieges();
	}
}
