package com.seibel.distanthorizons.core.jar.wrapperInterfaces.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import com.seibel.distanthorizons.core.jar.JarUtils;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public class LangWrapper implements ILangWrapper
{
	public static final LangWrapper INSTANCE = new LangWrapper();
	private static final Config jsonObject = Config.inMemory();
	private static final Logger logger = LogManager.getLogger(LangWrapper.class.getSimpleName());
	
	public static void init()
	{
		try
		{
//            System.out.println(JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/"+ Locale.getDefault().toString().toLowerCase()+".json")).replaceAll(":\\n.+?(?=\")",":"));
			// FIXME: Is there something in the config that the parser cant read?
			JsonFormat.fancyInstance().createParser().parse(
					JarUtils.convertInputStreamToString(JarUtils.accessFile("assets/lod/lang/" + Locale.getDefault().toString().toLowerCase() + ".json")),
					jsonObject, ParsingMode.REPLACE
			);
		}
		catch (Exception e)
		{
			logger.error("Failed to read lang file");
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean langExists(String str)
	{
		return jsonObject.get(str) != null;
	}
	
	@Override
	public String getLang(String str)
	{
		if (jsonObject.get(str) != null)
			return (String) jsonObject.get(str);
		else
			return str;
	}
	
}
