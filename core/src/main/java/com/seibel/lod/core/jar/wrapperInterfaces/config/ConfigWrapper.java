package com.seibel.lod.core.jar.wrapperInterfaces.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.wrapperInterfaces.config.IConfigWrapper;

import java.util.Locale;

public class ConfigWrapper implements IConfigWrapper {
    public static final ConfigWrapper INSTANCE = new ConfigWrapper();
    private static Config jsonObject = Config.inMemory();

    public static void init() {
        try {
//            System.out.println(JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/"+ Locale.getDefault().toString().toLowerCase()+".json")).replaceAll(":\\n.+?(?=\")",":"));
            // FIXME: Is there something in the config that the parser cant read?
            JsonFormat.fancyInstance().createParser().parse(
                    JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/"+ Locale.getDefault().toString().toLowerCase()+".json")),
                    jsonObject, ParsingMode.REPLACE
            );
        } catch (Exception e) { e.printStackTrace(); }
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
