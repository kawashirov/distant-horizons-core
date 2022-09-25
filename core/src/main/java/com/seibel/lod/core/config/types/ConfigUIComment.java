package com.seibel.lod.core.config.types;

/**
 * Adds something like a ConfigEntry but without a button to change the input
 *
 * @author coolGi
 */
public class ConfigUIComment extends AbstractConfigType<String, ConfigUIComment>{
    public ConfigUIComment(String value) {
        super(ConfigEntryAppearance.ONLY_SHOW, value); //TODO: Is the listener: null right?
    }

    @Override
    public void setAppearance(ConfigEntryAppearance newAppearance) {
        return;
    }

    public static class Builder extends AbstractConfigType.Builder<String, Builder> {
        @Override
        public Builder setAppearance(ConfigEntryAppearance newAppearance) {
            return this;
        }

        public ConfigUIComment build() {
            return new ConfigUIComment(tmpValue);
        }
    }
}
