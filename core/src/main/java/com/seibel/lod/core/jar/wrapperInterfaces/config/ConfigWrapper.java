package com.seibel.lod.core.jar.wrapperInterfaces.config;

import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Locale;

public class ConfigWrapper implements IConfigWrapper {
    public static final ConfigWrapper INSTANCE = new ConfigWrapper();
    private static JSONObject jsonObject = new JSONObject();

    public static void init() {
        try {
            jsonObject = (JSONObject) new JSONParser().parse(JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/"+ Locale.getDefault().toString().toLowerCase()+".json")));
        } catch (ParseException e) { e.printStackTrace(); }
    }

    @Override
    public boolean langExists(String str) {
        if (jsonObject.get(str) == null)
            return false;
        else
            return true;
    }

    @Override
    public String getLang(String str) {
        if (jsonObject.get(str) != null)
            return (String) jsonObject.get(str);
        else
            return str;
    }
}
