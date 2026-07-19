package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.config.CommentedConfiguration;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.UUID;

public final class TownClaimMetadataStoreCheck {
    public static void main(String[] args) throws Exception {
        assert Boolean.TRUE.equals(TownClaimMetadataStore.field("flag", "BooleanDataField", "true").getValue());
        assert Double.valueOf(2.5).equals(TownClaimMetadataStore.field("decimal", "DecimalDataField", "2.5").getValue());
        assert Integer.valueOf(4).equals(TownClaimMetadataStore.field("integer", "IntegerDataField", "4").getValue());
        assert Long.valueOf(8).equals(TownClaimMetadataStore.field("long", "LongDataField", "8").getValue());
        assert "value".equals(TownClaimMetadataStore.field("string", "StringDataField", "value").getValue());
        assert TownClaimMetadataStore.field("unknown", "Unknown", "value") == null;

        Field config = TownySettings.class.getDeclaredField("config");
        config.setAccessible(true);
        config.set(null, new CommentedConfiguration(Files.createTempFile("townclaim-siegewar", ".yml")));
        TownyUniverse universe = TownyUniverse.getInstance();
        Town town = new Town("CompatibilityCheck", UUID.randomUUID());
        universe.registerTown(town);
        assert TownyAPI.getInstance().getTown(town.getUUID()) == town;
        universe.unregisterTown(town);
    }
}
