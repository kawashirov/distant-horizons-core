package com.seibel.lod.core.config;

import com.seibel.lod.core.config.file.ConfigFileHandling;
import com.seibel.lod.core.config.types.AbstractConfigType;
import com.seibel.lod.core.config.types.ConfigCategory;
import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.config.ILangWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Indexes and sets everything up for the file handling and gui
 *
 * @author coolGi
 * @author Ran
 */
// Init the config after singletons have been blinded
public class ConfigBase 
{
    /** Our own config instance, don't modify */
    public static ConfigBase INSTANCE;
    /** Our own config instance, dont modify */
    public ConfigFileHandling configFileINSTANCE;

	public static final Logger LOGGER = LogManager.getLogger(ConfigBase.class.getSimpleName());
    public final String modID;
    public final String modName;
    public final int configVersion;
	
    /**
            What the config works with

        Enum
        Boolean
        Byte
        Integer
        Double
        Long
        Float
        String

        // Below, "T" should be a value from above
        List<T>
        ArrayList<T>
        Map<String, T>
        HashMap<String, T>
     */
    public static final List<Class<?>> acceptableInputs = new ArrayList<Class<?>>() {{
        add(Boolean.class);
        add(Byte.class);
        add(Short.class);
        add(Integer.class);
        add(Double.class);
        add(Long.class);
        add(Float.class);
        add(String.class);
        add(List.class);
        add(ArrayList.class);
        add(Map.class); // TODO[CONFIG]: This is handled separately to check the first input is String and the second input is valid
        add(HashMap.class);
    }};

    /** Disables the minimum and maximum of a variable */
    public boolean disableMinMax = false; // Very fun to use
    public final List<AbstractConfigType<?, ?>> entries = new ArrayList<>();


    public ConfigBase(String modID, String modName, Class<?> config, int configVersion) {
        LOGGER.info("Initialising config for " + modName);
        this.modID = modID;
        this.modName = modName;
        this.configVersion = configVersion;

        initNestedClass(config, ""); // Init root category

        // File handling (load from file)
        this.configFileINSTANCE = new ConfigFileHandling(this);
        this.configFileINSTANCE.loadFromFile();
        LOGGER.info("Config for " + modName + " initialised");
    }

    private void initNestedClass(Class<?> config, String category) {
        // Put all the entries in entries

        for (Field field : config.getFields()) {
            if (AbstractConfigType.class.isAssignableFrom(field.getType())) {
                try {
                    entries.add((AbstractConfigType<?, ?>) field.get(field.getType()));
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }

                AbstractConfigType<?, ?> entry = entries.get(entries.size() - 1);
                entry.category = category;
                entry.name = field.getName();
                entry.configBase = this;

                if (ConfigEntry.class.isAssignableFrom(field.getType())) { // If item is type ConfigEntry
                    if (!isAcceptableType(((ConfigEntry<?>) entry).getType())) {
                        LOGGER.error("Invalid variable type at [" + (category.isEmpty() ? "" : category + ".") + field.getName() + "].");
                        LOGGER.error("Type [" + ((ConfigEntry<?>) entry).getType() + "] is not one of these types [" + acceptableInputs.toString() + "]");
                        entries.remove(entries.size() -1); // Delete the entry if it is invalid so the game can still run
                    }
                }

                if (ConfigCategory.class.isAssignableFrom(field.getType())) { // If it's a category then init the stuff inside it and put it in the category list
                    if (((ConfigCategory) entry).getDestination() == null)
                        ((ConfigCategory) entry).destination = ((ConfigCategory) entry).getNameWCategory();
                    if (entry.get() != null) {
                        initNestedClass(((ConfigCategory) entry).get(), ((ConfigCategory) entry).getDestination());
                    }
                }
            }
        }
    }

    private static boolean isAcceptableType(Class<?> Clazz) {
        if (Clazz.isEnum())
            return true;
        for(Class<?> i: acceptableInputs) {
            if(i == Clazz)
                return true;
        }
        return false;
    }



    /**
     * Used for checking that all the lang files for the config exist
     *
     * @param onlyShowNew If disabled then it would basically remake the config lang
     * @param checkEnums  Checks if all the lang for the enum's exist
     */
    // This is just to re-format the lang or check if there is something in the lang that is missing
    public String generateLang(boolean onlyShowNew, boolean checkEnums) {
        ILangWrapper langWrapper = SingletonInjector.INSTANCE.get(ILangWrapper.class);
        List<Class<? extends Enum>> enumList = new ArrayList<>();

        String generatedLang = "";

        String starter = "  \"";
        String separator = "\":\n    \"";
        String ending = "\",\n";

        for (AbstractConfigType<?, ?> entry: this.entries) {
            String entryPrefix = "lod.config."+entry.getNameWCategory();

            if (checkEnums && entry.getType().isEnum() && !enumList.contains(entry.getType())) { // Put it in an enum list to work with at the end
                enumList.add((Class<? extends Enum>) entry.getType());
            }
            if (!onlyShowNew || langWrapper.langExists(entryPrefix)) {
                generatedLang += starter
                        + entryPrefix
                        + separator
                        + langWrapper.getLang(entryPrefix)
                        + ending
                ;
                // Adds tooltips
                if (langWrapper.langExists(entryPrefix+".@tooltip")) {
                    generatedLang += starter
                            + entryPrefix+".@tooltip"
                            + separator
                            + langWrapper.getLang(entryPrefix+".@tooltip")
                                    .replaceAll("\n", "\\\\n")
                                    .replaceAll("\"", "\\\\\"")
                            + ending
                    ;
                }
            }
        }
        if (!enumList.isEmpty()) {
            generatedLang += "\n"; // Separate the main lang with the enum's

            for (Class<? extends Enum> anEnum: enumList) {
                for (Object enumStr: new ArrayList<>(EnumSet.allOf(anEnum))) {
                    String enumPrefix = "lod.config.enum."+anEnum.getSimpleName()+"."+enumStr.toString();

                    if (!onlyShowNew || langWrapper.langExists(enumPrefix)) {
                        generatedLang += starter
                                + enumPrefix
                                + separator
                                + langWrapper.getLang(enumPrefix)
                                + ending
                        ;
                    }
                }
            }
        }

        return generatedLang;
    }
}
