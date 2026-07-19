package com.gmail.goosius.siegewar.integration.townclaim;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class TownClaimMetadataStore {
    private static File file;
    private static YamlConfiguration data;

    private TownClaimMetadataStore() {
    }

    static void load(File dataFolder) {
        file = new File(dataFolder, "metadata.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    static void hydrate(String type, UUID id, TownyObject object) {
        ConfigurationSection section = data.getConfigurationSection(type + "." + id);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String path = key + ".";
            CustomDataField<?> field = field(key, section.getString(path + "type"), section.getString(path + "value", ""));
            if (field != null) {
                object.addMetaData(field, false);
            }
        }
    }

    static void save(String type, UUID id, TownyObject object) {
        String root = type + "." + id;
        data.set(root, null);
        for (CustomDataField<?> field : object.getMetadata()) {
            data.set(root + "." + field.getKey() + ".type", field.getClass().getSimpleName());
            data.set(root + "." + field.getKey() + ".value", String.valueOf(field.getValue()));
        }
        try {
            data.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save SiegeWar metadata", exception);
        }
    }

    static CustomDataField<?> field(String key, String type, String value) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "BooleanDataField" -> new BooleanDataField(key, Boolean.parseBoolean(value));
            case "DecimalDataField" -> new DecimalDataField(key, Double.parseDouble(value));
            case "IntegerDataField" -> new IntegerDataField(key, Integer.parseInt(value));
            case "LongDataField" -> new LongDataField(key, Long.parseLong(value));
            case "StringDataField" -> new StringDataField(key, value);
            default -> null;
        };
    }
}
