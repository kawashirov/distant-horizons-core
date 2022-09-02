package com.seibel.lod.core.config.file;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.internal.a7.ClientApi;
import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.types.AbstractConfigType;
import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles reading and writing config files.
 *
 * @author coolGi
 * @version 2022-5-26
 */
public class ConfigFileHandling {
    public static final Path ConfigPath = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class)
            .getInstallationDirectory().toPath().resolve("config").resolve(ModInfo.NAME+".toml");

    /** Saves the config to the file */
    public static void saveToFile() {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        if (!Files.exists(ConfigPath)) // Try to check if the config exists
            try {
                Files.createFile(ConfigPath);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        loadConfig(config);

        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass())) {
                createComment((ConfigEntry<?>) entry, config);
                saveEntry((ConfigEntry<?>) entry, config);
            }
        }

        config.save();
        config.close();
    }
    /** Loads the config from the file */
    public static void loadFromFile() {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        // Attempt to load the file and if it fails then save config to file
        try {
            if (Files.exists(ConfigPath))
                config.load();
            else {
                saveToFile();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            saveToFile();
            return;
        }

        // Load all the entries
        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass())) {
                createComment((ConfigEntry<?>) entry, config);
                loadEntry((ConfigEntry<?>) entry, config);
            }
        }

        config.save();
        config.close();
    }




    // Save an entry when only given the entry
    public static void saveEntry(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).build();
        loadConfig(config);
        saveEntry(entry, config);
        config.save();
        config.close();
    }
    // Save an entry
    @SuppressWarnings("unchecked")
    public static void saveEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile) return;

        if (ConfigTypeConverters.convertObjects.containsKey(entry.getType())) {
            workConfig.set(entry.getNameWCategory(), ConfigTypeConverters.convertToString(entry.getType(), entry.getTrueValue()));
        } else {
            workConfig.set(entry.getNameWCategory(), entry.getTrueValue());
        }
    }

    // Loads an entry when only given the entry
    public static void loadEntry(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).autosave().build();
        loadConfig(config);
        loadEntry(entry, config);
        config.close();

    }
    // Loads an entry
    @SuppressWarnings("unchecked") // Suppress due to its always safe
	public static <T> void loadEntry(ConfigEntry<T> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile) return;

        if (workConfig.contains(entry.getNameWCategory())) {
            try {
                if (entry.getType().isEnum()) {
                    entry.setWithoutSaving((T) ( workConfig.getEnum(entry.getNameWCategory(), (Class<? extends Enum>) entry.getType()) ));
                } else if (ConfigTypeConverters.convertObjects.containsKey(entry.getType())) {
                    entry.setWithoutSaving((T) ConfigTypeConverters.convertFromString(entry.getType(), workConfig.get(entry.getNameWCategory())));
                } else {
                    if (entry.getType() == workConfig.get(entry.getNameWCategory()).getClass()) { // If the types are the same
                        entry.setWithoutSaving((T) workConfig.get(entry.getNameWCategory()));

                        if (entry.isValid() == 0) return;
                        else if (entry.isValid() == -1) entry.setWithoutSaving(entry.getMin());
                        else if (entry.isValid() == 1) entry.setWithoutSaving(entry.getMax());
                    } else {
                        ClientApi.LOGGER.warn("Entry ["+ entry.getNameWCategory() +"] is invalid. Expected " + entry.getType() + " but got " + workConfig.get(entry.getNameWCategory()).getClass() + ". Using default value.");
                        saveEntry(entry, workConfig);
                    }
                }
            } catch (Exception e) {
//                e.printStackTrace();
                ClientApi.LOGGER.warn("Entry ["+entry.getNameWCategory()+"] had an invalid value when loading the config. Using default value.");
                saveEntry(entry, workConfig);
            }
        } else {
            saveEntry(entry, workConfig);
        }
    }

    // Creates the comment for an entry when only given the entry
    public static void createComment(ConfigEntry<?> entry) {
        CommentedFileConfig config = CommentedFileConfig.builder(ConfigPath.toFile()).autosave().build();
        loadConfig(config);
        createComment(entry, config);
        config.close();
    }
    // Creates a comment for an entry
    public static void createComment(ConfigEntry<?> entry, CommentedFileConfig workConfig) {
        if (!entry.getAppearance().showInFile)
            return;
        if (entry.getComment() != null) {
            workConfig.setComment(entry.getNameWCategory(), " " + entry.getComment().replaceAll("\n", "\n ") + "\n ");
        }
    }




    /** Does config.load(); but with error checking */
    public static void loadConfig(CommentedFileConfig config) {
        try {
            config.load();
        } catch (Exception e) {
            System.out.println("Loading file failed because of this expectation:\n"+e);
            try { // Now try remaking the file and loading it
                if (!ConfigPath.getParent().toFile().exists())
                    Files.createDirectory(ConfigPath.getParent());
                else
                    Files.deleteIfExists(ConfigPath);
                Files.createFile(ConfigPath);
                config.load();
            } catch (IOException ex) {
                System.out.println("Creating file failed");
                ex.printStackTrace();
                SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).crashMinecraft("Loading file and resetting config file failed at path ["+ConfigPath+"]. Please check the file is ok and you have the permissions", ex);
            }
        }
    }


    // ========== API (server) STUFF ========== //
    @SuppressWarnings("unchecked")
    /** ALWAYS CLEAR WHEN NOT ON SERVER!!!! */
    public static void clearApiValues() {
        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass()) && ((ConfigEntry) entry).allowApiOverride) {
                ((ConfigEntry) entry).setApiValue(null);
            }
        }
    }
    @SuppressWarnings("unchecked")
    public static String exportApiValues() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("configVersion", ConfigBase.configVersion);
        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass()) && ((ConfigEntry) entry).allowApiOverride) {
                if (ConfigTypeConverters.convertObjects.containsKey(entry.getType())) {
                    jsonObject.put(entry.getNameWCategory(), ConfigTypeConverters.convertToString(entry.getType(), ((ConfigEntry<?>) entry).getTrueValue()));
                } else {
                    jsonObject.put(entry.getNameWCategory(), ((ConfigEntry<?>) entry).getTrueValue());
                }
            }
        }
        return jsonObject.toJSONString();
    }
    @SuppressWarnings("unchecked") // Suppress due to its always safe
    public static void importApiValues(String values) {
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(values);
        } catch (ParseException p) {
            p.printStackTrace();
        }

        // Importing code
        for (AbstractConfigType<?, ?> entry : ConfigBase.entries) {
            if (ConfigEntry.class.isAssignableFrom(entry.getClass()) && ((ConfigEntry) entry).allowApiOverride) {
                Object jsonItem = jsonObject.get(entry.getNameWCategory());
                if (entry.getType().isEnum()) {
                    ((ConfigEntry) entry).setApiValue(Enum.valueOf((Class<? extends Enum>) entry.getType(), jsonItem.toString()));
                } else if (ConfigTypeConverters.convertObjects.containsKey(entry.getType())) {
                    ((ConfigEntry) entry).setApiValue(ConfigTypeConverters.convertFromString(entry.getType(), jsonItem.toString()));
                } else {
                    ((ConfigEntry) entry).setApiValue(jsonItem);
                }
            }
        }
    }
}
