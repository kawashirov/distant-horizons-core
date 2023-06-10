package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.types.enums.EConfigEntryAppearance;

/**
 * Adds a categoty to the config
 * See our config file for more information on how to use it
 *
 * @author coolGi
 */
public class ConfigCategory extends AbstractConfigType<Class, ConfigCategory> {
    public String destination;    // Where the category goes to

    private ConfigCategory(EConfigEntryAppearance appearance, Class value, String destination) {
        super(appearance, value);
        this.destination = destination;
    }

    public String getDestination() {
        return this.destination;
    }

    @Override
    @Deprecated
    /** Use get() instead for category */
    public Class<?> getType() {
        return value;
    }

    public static class Builder extends AbstractConfigType.Builder<Class, Builder> {
        private String tmpDestination = null;

        public Builder setDestination(String newDestination) {
            this.tmpDestination = newDestination;
            return this;
        }

        public Builder setAppearance(EConfigEntryAppearance newAppearance) {
            this.tmpAppearance = newAppearance;
            return this;
        }

        public ConfigCategory build() {
            return new ConfigCategory(tmpAppearance, tmpValue, tmpDestination);
        }
    }
}
