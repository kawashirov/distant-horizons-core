package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.file.ConfigFileHandling;

/**
 * Use for making the config variables
 *
 * @author coolGi
 * @version 2022-5-26
 */
public class ConfigEntry<T> extends AbstractConfigType<T, ConfigEntry<T>>
{
    private final T defaultValue;
    private String comment;
    private T min;
    private T max;

    // API control //
    /**
     * If true this config can be controlled by the API <br>
     * and any get() method calls will return the apiValue if it is set.
     */
    public final boolean allowApiOverride;
    private T apiValue;
    private Runnable runnable; // What to run when the value gets changed

    private ConfigEntryPerformance performance;


    /** Creates the entry */
    private ConfigEntry(ConfigEntryAppearance appearance, T value, String comment, T min, T max, boolean allowApiOverride, Runnable runnable, ConfigEntryPerformance performance) {
        super(appearance, value);
        this.defaultValue = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
        this.allowApiOverride = allowApiOverride;
        this.runnable = runnable;
        this.performance = performance;
    }


    /** Gets the default value of the option */
    public T getDefaultValue() {
        return this.defaultValue;
    }

    public void setApiValue(T newApiValue) {
        this.apiValue = newApiValue;
    }
    public T getApiValue() {
        return this.apiValue;
    }
    @Override
    public void set(T newValue) {
        super.set(newValue);
        save();
    }
    @Override
    public T get() {
        if (allowApiOverride && apiValue != null)
            return apiValue;
        return super.get();
    }
    public T getTrueValue() {
        return super.get();
    }

    /** Sets the value without saving */
    public void setWithoutSaving(T newValue) {
        super.set(newValue);
    }

    /** Gets the min value */
    public T getMin() {
        return this.min;
    }
    /** Sets the min value */
    public void setMin(T newMin) {
        this.min = newMin;
    }
    /** Gets the max value */
    public T getMax() {
        return this.max;
    }
    /** Sets the max value */
    public void setMax(T newMax) {
        this.max = newMax;
    }
    /** Sets the min and max in 1 setter */
    public void setMinMax(T newMin, T newMax) {
        this.max = newMin;
        this.min = newMax;
    }

    /** Gets the comment */
    public String getComment() {
        return this.comment;
    }
    /** Sets the comment */
    public void setComment(String newComment) {
        this.comment = newComment;
    }

    /** Gets the performance impact of an option */
    public ConfigEntryPerformance getPerformance() {
        return this.performance;
    }

    /**
     * Checks if the option is valid
     *
     * 0 == valid
     * 1 == number too high
     * -1 == number too low
     */
    public byte isValid() {
        return isValid(value);
    }
    /** Checks if a value is valid */
    public byte isValid(T value) {
        if (ConfigBase.disableMinMax)
            return 0;
        if (Number.class.isAssignableFrom(value.getClass())) { // Only check min max if it is a number
            if (this.max != null && Double.valueOf(value.toString()) > Double.valueOf(max.toString()))
                return 1;
            if (this.min != null && Double.valueOf(value.toString()) < Double.valueOf(min.toString()))
                return -1;

            return 0;
        }
        return 0;
    }

    /** This should normally not be called since set() automatically calls this */
    public void save() {
        ConfigFileHandling.saveEntry(this);
    }
    /** This should normally not be called except for special circumstances */
    public void load() {
        ConfigFileHandling.loadEntry(this);
    }

    /** Is the value of this equal to another */
    public boolean equals(ConfigEntry<?> obj) {
        // Can all of this just be "return this.value.equals(obj.value)"?

        if (this.value.getClass() != obj.value.getClass())
            return false;
        if (Number.class.isAssignableFrom(this.value.getClass())) {
            if (this.value == obj.value)
                return true;
            else return false;
        } else {
            if (this.value.equals(obj.value))
                return true;
            else return false;
        }
    }

    public Runnable getRunnable() {
        return this.runnable;
    }
    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }
    public void runRunnable() {
        if (runnable != null)
            runnable.run();
    }

    public static class Builder<T> extends AbstractConfigType.Builder<T, Builder<T>> {
        private String tmpComment = null;
        private T tmpMin;
        private T tmpMax;
        private boolean tmpUseApiOverwrite;
        private Runnable runnable;
        private ConfigEntryPerformance performance = ConfigEntryPerformance.DONT_SHOW;

        public Builder<T> comment(String newComment) {
            this.tmpComment = newComment;
            return this;
        }

        public Builder<T> setMinDefaultMax(T newMin, T newDefault, T newMax) {
            this.set(newDefault);
            this.setMinMax(newMin, newMax);
            return this;
        }

        public Builder<T> setMinMax(T newMin, T newMax) {
            this.tmpMin = newMin;
            this.tmpMax = newMax;
            return this;
        }

        public Builder<T> setMin(T newMin) {
            this.tmpMin = newMin;
            return this;
        }
        public Builder<T> setMax(T newMax) {
            this.tmpMax = newMax;
            return this;
        }

        public Builder<T> setUseApiOverwrite(boolean newUseApiOverwrite) {
            this.tmpUseApiOverwrite = newUseApiOverwrite;
            return this;
        }

        public Builder<T> setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public Builder<T> setPerformance(ConfigEntryPerformance performance) {
            this.performance = performance;
            return this;
        }



        public ConfigEntry<T> build() {
            return new ConfigEntry<T>(tmpAppearance, tmpValue, tmpComment, tmpMin, tmpMax, tmpUseApiOverwrite, runnable, performance);
        }
    }
}
