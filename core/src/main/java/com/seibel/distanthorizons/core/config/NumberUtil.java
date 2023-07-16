package com.seibel.distanthorizons.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Gets the minimum or maximum values of a given type
 *
 * @author coolGi
 * @version 2023-7-16
 */
// TODO: Should this be moved out of here into somewhere like the util section
public class NumberUtil {
    // Is there no better way of doing this?
    public static Map<Class, Number> minValues = new HashMap<Class, Number>() {{
        put(Byte.class, Byte.MIN_VALUE);
        put(Short.class, Short.MIN_VALUE);
        put(Integer.class, Integer.MIN_VALUE);
        put(Long.class, Long.MIN_VALUE);
        put(Double.class, Double.MIN_VALUE);
        put(Float.class, Float.MIN_VALUE);
    }};
    public static Map<Class, Number> maxValues = new HashMap<Class, Number>() {{
        put(Byte.class, Byte.MAX_VALUE);
        put(Short.class, Short.MAX_VALUE);
        put(Integer.class, Integer.MAX_VALUE);
        put(Long.class, Long.MAX_VALUE);
        put(Double.class, Double.MAX_VALUE);
        put(Float.class, Float.MAX_VALUE);
    }};

    public static Number getMinimum(Class c) {
        return minValues.get(c);
    }
    public static Number getMaximum(Class c) {
        return maxValues.get(c);
    }
}
