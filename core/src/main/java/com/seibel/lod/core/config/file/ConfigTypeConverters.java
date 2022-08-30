package com.seibel.lod.core.config.file;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

public class ConfigTypeConverters {
    // Once you've made a converter add it to here where the first value is the type you want to convert and the 2nd value is the converter
    public static final Map<Class, ConverterBase> convertObjects = new HashMap<Class, ConverterBase>() {{
        put(Map.class, new MapConverter());
    }};

    public static String convertToString(Class clazz, Object value) {
        try {
            return convertObjects.get(clazz).convertToString(value);
        } catch (Exception e) {
            System.out.println("Type [" + clazz.toString() + "] isnt a convertible value in the config file handler");
            return null;
        }
    }
    public static Object convertFromString(Class clazz, String value) {
        try {
            return convertObjects.get(clazz).convertFromString(value);
        } catch (Exception e) {
            System.out.println("Type [" + clazz.toString() + "] isnt a convertible value in the config file handler");
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


    @SuppressWarnings("unchecked")
    public static class MapConverter extends ConverterBase {
        @Override
        public String convertToString(Object item) {
            Map<String, Object> mapObject = (Map<String, Object>) item;
            JSONObject jsonObject = new JSONObject();

            for (int i = 0; i < mapObject.size(); i++) {
                jsonObject.put(mapObject.keySet().toArray()[i], mapObject.get(mapObject.keySet().toArray()[i]));
            }

            return jsonObject.toJSONString();
        }

        @Override
        public Map<String, Object> convertFromString(String s) {
            Map<String, Object> map = new HashMap<>();

            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) new JSONParser().parse(s);
            } catch (ParseException p) {
                p.printStackTrace();
            }

            for (int i = 0; i < jsonObject.keySet().toArray().length; i++) {
                map.put((String) jsonObject.keySet().toArray()[i], jsonObject.get(jsonObject.keySet().toArray()[i]));
            }
            return map;
        }
    }
}
