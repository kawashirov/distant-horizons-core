package com.seibel.lod.core.config.file;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows for custom varuable types to be saved in the config
 * (currently its only used for Map's)
 *
 * @author coolGi
 */
public class ConfigTypeConverters {
    // Once you've made a converter add it to here where the first value is the type you want to convert and the 2nd value is the converter
    public static final Map<Class, ConverterBase> convertObjects = new HashMap<Class, ConverterBase>() {{
        put(Float.class, new FloatConverter());
        put(Byte.class, new ByteConverter());

        put(Map.class, new MapConverter());
        put(HashMap.class, new MapConverter());
    }};

    public static String convertToString(Class clazz, Object value) {
        try {
            return convertObjects.get(clazz).convertToString(value);
        } catch (Exception e) {
            System.out.println("Type [" + clazz.toString() + "] isn't a convertible value in the config file handler");
            return null;
        }
    }
    public static Object convertFromString(Class clazz, String value) {
        try {
            return convertObjects.get(clazz).convertFromString(value);
        } catch (Exception e) {
            System.out.println("Type [" + clazz.toString() + "] isn't a convertible value in the config file handler");
            return null;
        }
    }


    /**
     * The converter should extend this
     */
    public static abstract class ConverterBase {
        public abstract String convertToString(Object value);
        public abstract Object convertFromString(String value);
    }



    // Float and Bytes are a bit wack with the config parser
    // So we just store them as strings
    public static class FloatConverter extends ConverterBase {
        @Override public String convertToString(Object item) { return ((Float) item).toString(); }
        @Override public Float convertFromString(String s) { return Float.valueOf(s); }
    }
    public static class ByteConverter extends ConverterBase {
        @Override public String convertToString(Object item) { return ((Byte) item).toString(); }
        @Override public Byte convertFromString(String s) { return Byte.valueOf(s); }
    }



    @SuppressWarnings("unchecked")
    public static class MapConverter extends ConverterBase {
        @Override
        public String convertToString(Object item) {
            Map<String, Object> mapObject = (Map<String, Object>) item;
            Config jsonObject = Config.inMemory();

            for (int i = 0; i < mapObject.size(); i++) {
                jsonObject.add(mapObject.keySet().toArray()[i].toString(), mapObject.get(mapObject.keySet().toArray()[i]));
            }

            return JsonFormat.fancyInstance().createWriter().writeToString(jsonObject);
        }

        @Override
        public Map<String, Object> convertFromString(String s) {
            Map<String, Object> map = new HashMap<>();

            Config jsonObject = Config.inMemory();
            try {
                JsonFormat.fancyInstance().createParser().parse(s, jsonObject, ParsingMode.REPLACE);
            } catch (Exception e) { e.printStackTrace(); }

            return jsonObject.valueMap();
        }
    }
}
