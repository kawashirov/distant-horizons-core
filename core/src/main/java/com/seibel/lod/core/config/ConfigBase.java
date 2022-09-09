package com.seibel.lod.core.config;

import com.seibel.lod.core.config.file.ConfigFileHandling;
import com.seibel.lod.core.config.types.AbstractConfigType;
import com.seibel.lod.core.config.types.ConfigCategory;
import com.seibel.lod.core.config.types.ConfigEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indexes and sets everything up for the file handling and gui
 *
 * @author coolGi
 * @author Ran
 */
// Init the config after singletons have been blinded
public class ConfigBase 
{
    public static ConfigBase INSTANCE;
    public ConfigFileHandling configFileINSTANCE;

	public static final Logger LOGGER = LogManager.getLogger(ConfigBase.class.getSimpleName());
    public final String modID;
    public final String modName;
    public final int configVersion;
	
    /*
            What the config works with

        Enum
        Boolean
        Byte
        Integer
        Double
        Long
        // Float (to be tested)
        String
        Map<String, ?> // The ? should be another value from above
     */
    public static final List<Class<?>> acceptableInputs = new ArrayList<>();
    private static void addAcceptableInputs() {
        acceptableInputs.add(Boolean.class);
        acceptableInputs.add(Byte.class);
        acceptableInputs.add(Short.class);
        acceptableInputs.add(Integer.class);
        acceptableInputs.add(Double.class);
        acceptableInputs.add(Long.class);
//        acceptableInputs.add(Float.class);
        acceptableInputs.add(String.class);
        acceptableInputs.add(Map.class); // TODO[CONFIG]: This is handled separately to check the first input is String and the second input is valid
    }

    /** Disables the minimum and maximum of a variable */
    public boolean disableMinMax = false; // Very fun to use
    public final List<AbstractConfigType<?, ?>> entries = new ArrayList<>();


    public ConfigBase(String modID, String modName, Class<?> config, int configVersion) {
        this.modID = modID;
        this.modName = modName;
        this.configVersion = configVersion;

        addAcceptableInputs(); // Add all the acceptable stuff to the acceptableInputs list
        initNestedClass(config, ""); // Init root category

        // File handling (load from file)
        this.configFileINSTANCE = new ConfigFileHandling(this);
        this.configFileINSTANCE.loadFromFile();
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
                    if (!isAcceptableType(((ConfigEntry<?>) entry).get().getClass())) {
                        LOGGER.error("Invalid variable type at [" + (category.isEmpty() ? "" : category + ".") + field.getName() + "].");
                        LOGGER.error("Type [" + ((ConfigEntry<?>) entry).get().getClass() + "] is not one of these types [" + acceptableInputs.toString() + "]");
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
            if(Clazz == i)
                return true;
        }
        return false;
    }
}
