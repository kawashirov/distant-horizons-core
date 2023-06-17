package com.seibel.distanthorizons.core.config.types;


import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.enums.EConfigEntryAppearance;
import com.seibel.distanthorizons.core.config.types.enums.EConfigEntryPerformance;
import com.seibel.distanthorizons.core.config.types.enums.*;
import com.seibel.distanthorizons.coreapi.interfaces.config.IConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Use for making the config variables
 * for types that are not supported by it look in ConfigBase
 *
 * @author coolGi
 * @version 2022-5-26
 */
public class ConfigEntry<T> extends AbstractConfigType<T, ConfigEntry<T>> implements IConfigEntry<T>
{
    private final T defaultValue;
    private String comment;
    private T min;
    private T max;
    private final ArrayList<IConfigListener> listenerList;

    // API control //
    /**
     * If true this config can be controlled by the API <br>
     * and any get() method calls will return the apiValue if it is set.
     */
    public final boolean allowApiOverride;
    private T apiValue;

    private final EConfigEntryPerformance performance;


    /** Creates the entry */
    private ConfigEntry(EConfigEntryAppearance appearance, T value, String comment, T min, T max, boolean allowApiOverride, EConfigEntryPerformance performance, ArrayList<IConfigListener> listenerList)
	{
        super(appearance, value);
        this.defaultValue = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
        this.allowApiOverride = allowApiOverride;
        this.performance = performance;
        this.listenerList = listenerList;
    }
	
	
	
    /** Gets the default value of the option */
	@Override
	public T getDefaultValue() { return this.defaultValue; }
	
	@Override
    public void setApiValue(T newApiValue) { this.apiValue = newApiValue; }
	@Override
	public T getApiValue() { return this.apiValue; }
	@Override 
	public boolean getAllowApiOverride() { return this.allowApiOverride; }
    @Override
    public void set(T newValue)
	{
        super.set(newValue);
        this.save();
        this.listenerList.forEach(IConfigListener::onConfigValueSet);
    }
    public void uiSet(T newValue)
	{
        this.set(newValue);
        this.listenerList.forEach(IConfigListener::onUiModify);
    }
    @Override
    public T get()
	{
        if (allowApiOverride && apiValue != null)
		{
			return apiValue;
		}
		
        return super.get();
    }
	@Override
    public T getTrueValue() {
        return super.get();
    }

    /** Sets the value without saving */
	@Override
    public void setWithoutSaving(T newValue)
	{
		super.set(newValue);
		this.listenerList.forEach(IConfigListener::onConfigValueSet);
	}

    /** Gets the min value */
	@Override
    public T getMin() { return this.min; }
    /** Sets the min value */
	@Override
    public void setMin(T newMin) { this.min = newMin; }
    /** Gets the max value */
	@Override
    public T getMax() { return this.max; }
    /** Sets the max value */
	@Override
    public void setMax(T newMax) { this.max = newMax; }
    /** Sets the min and max in 1 setter */
	@Override
    public void setMinMax(T newMin, T newMax)
	{
        this.max = newMin;
        this.min = newMax;
    }

	@Override
    public String getComment() { return this.comment; }
	@Override
    public void setComment(String newComment) { this.comment = newComment; }

    /** Gets the performance impact of an option */
    public EConfigEntryPerformance getPerformance() { return this.performance; }

    public void addListener(IConfigListener newListener) { this.listenerList.add(newListener); }
    public void removeListener(IConfigListener oldListener) { this.listenerList.remove(oldListener); }
	
    public void clearListeners() { this.listenerList.clear(); }
    public ArrayList<IConfigListener> getListeners() { return this.listenerList; }
    /** Replaces the listener list */
    public void setListeners(ArrayList<IConfigListener> newListeners)
	{
        this.listenerList.clear();
        this.listenerList.addAll(newListeners);
    }
    public void setListeners(IConfigListener... newListeners) { this.listenerList.addAll(Arrays.asList(newListeners)); }


    /**
     * Checks if the option is valid
     *
     * @return      0 == valid
     *         <p>  2 == invalid
     *         <p>  1 == number too high
     *         <p> -1 == number too low
     */
	@Override
    public byte isValid() { return isValid(this.value); }
    /**
     * Checks if a new value is valid
     *
     * @return      0 == valid
     *         <p>  2 == invalid
     *         <p>  1 == number too high
     *         <p> -1 == number too low
     */
	@Override
    public byte isValid(T value) {
        if (this.configBase.disableMinMax)
            return 0;

        if (value.getClass() != this.value.getClass()) // If the 2 variables aren't the same type then it will be invalid
            return 2;
        if (Number.class.isAssignableFrom(value.getClass())) { // Only check min max if it is a number
            if (this.max != null && Float.parseFloat(value.toString()) > Float.parseFloat(max.toString()))
                return 1;
            if (this.min != null && Float.parseFloat(value.toString()) < Float.parseFloat(min.toString()))
                return -1;

            return 0;
        }
        return 0;
    }

    /** This should normally not be called since set() automatically calls this */
    public void save() { configBase.configFileINSTANCE.saveEntry(this); }
    /** This should normally not be called except for special circumstances */
    public void load() { configBase.configFileINSTANCE.loadEntry(this); }
	
	
	@Override
	public boolean equals(IConfigEntry<?> obj) { return obj.getClass() == ConfigEntry.class && equals((ConfigEntry<?>) obj); }
    /** Is the value of this equal to another */
    public boolean equals(ConfigEntry<?> obj) {
        // Can all of this just be "return this.value.equals(obj.value)"?

        if (Number.class.isAssignableFrom(this.value.getClass()))
            return this.value == obj.value;
        else
            return this.value.equals(obj.value);
    }


    public static class Builder<T> extends AbstractConfigType.Builder<T, Builder<T>>
	{
        private String tmpComment = null;
        private T tmpMin;
        private T tmpMax;
        private boolean tmpUseApiOverwrite;
        private EConfigEntryPerformance tmpPerformance = EConfigEntryPerformance.DONT_SHOW;
        protected ArrayList<IConfigListener> tmpIConfigListener = new ArrayList<>();

        public Builder<T> comment(String newComment)
		{
			this.tmpComment = newComment;
			return this;
		}

        /** Allows most values to be set by 1 setter */
        public Builder<T> setMinDefaultMax(T newMin, T newDefault, T newMax)
		{
			this.set(newDefault);
			this.setMinMax(newMin, newMax);
			return this;
		}

        public Builder<T> setMinMax(T newMin, T newMax)
		{
			this.tmpMin = newMin;
			this.tmpMax = newMax;
			return this;
		}

        public Builder<T> setMin(T newMin)
		{
			this.tmpMin = newMin;
			return this;
		}
		
		public Builder<T> setMax(T newMax)
		{
			this.tmpMax = newMax;
			return this;
		}

        public Builder<T> setUseApiOverwrite(boolean newUseApiOverwrite)
		{
			this.tmpUseApiOverwrite = newUseApiOverwrite;
			return this;
		}

        public Builder<T> setPerformance(EConfigEntryPerformance newPerformance)
		{
			this.tmpPerformance = newPerformance;
			return this;
		}

        public Builder<T> replaceListener(ArrayList<IConfigListener> newConfigListener)
		{
			this.tmpIConfigListener = newConfigListener;
			return this;
		}
		
		public Builder<T> addListeners(IConfigListener... newConfigListener)
		{
			this.tmpIConfigListener.addAll(Arrays.asList(newConfigListener));
			return this;
		}
		
		public Builder<T> addListener(IConfigListener newConfigListener)
		{
			this.tmpIConfigListener.add(newConfigListener);
			return this;
		}
		
		public Builder<T> clearListeners()
		{
			this.tmpIConfigListener.clear();
			return this;
		}



        public ConfigEntry<T> build()
		{
			return new ConfigEntry<>(this.tmpAppearance, this.tmpValue, this.tmpComment, this.tmpMin, this.tmpMax, this.tmpUseApiOverwrite, this.tmpPerformance, this.tmpIConfigListener);
        }
		
    }
}
