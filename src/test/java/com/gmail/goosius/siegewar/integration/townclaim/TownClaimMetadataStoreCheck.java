package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.config.CommentedConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
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
        UUID claimId = UUID.randomUUID();
        assert TownClaimBridge.territoryId(claimId).equals(TownClaimBridge.territoryId(claimId));
        assert !TownClaimBridge.territoryId(claimId).equals(TownClaimBridge.territoryId(UUID.randomUUID()));

        CommentedConfiguration config = new CommentedConfiguration(Files.createTempFile("townclaim-siegewar", ".yml"));
        assert config.load();
        TownClaimBridge.configureTownySettings(config);
        assert TownySettings.getTownLevel(0) != null;
        assert TownySettings.getNationLevel(0) != null;

        // Configuration runs during both startup and reload and must remain safe to repeat.
        TownClaimBridge.configureTownySettings(config);
        assert TownySettings.getTownLevel(0) != null;
        assert TownySettings.getNationLevel(0) != null;

        assertNationFormattingUsesInitializedLevels();

        TownyUniverse universe = TownyUniverse.getInstance();
        Town town = new Town("CompatibilityCheck", UUID.randomUUID());
        universe.registerTown(town);
        assert TownyAPI.getInstance().getTown(town.getUUID()) == town;
        universe.unregisterTown(town);
    }

    private static void assertNationFormattingUsesInitializedLevels() throws Exception {
        PluginManager pluginManager = (PluginManager) Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> method.getName().equals("getPluginManager")
                        ? pluginManager
                        : defaultValue(method.getReturnType()));

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Object previousServer = serverField.get(null);
        serverField.set(null, server);
        try {
            Nation nation = new Nation("Compatibility_Nation", UUID.randomUUID());
            assert nation.getFormattedName().contains("Compatibility");
        } finally {
            serverField.set(null, previousServer);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
