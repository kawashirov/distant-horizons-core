package com.seibel.lod.core.jar;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JarUtils {
    public static final File jarFile = new File(JarUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath());



    /**
     * Get a file within the mods resources
     * @param resource Location of the file you want to get
     * @return InputStream
     */
    public static InputStream accessFile(String resource) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // this is the path within the jar file
        InputStream input = loader.getResourceAsStream(resource);
        if (input == null) {
            // this is how we load file within editor
            input = loader.getResourceAsStream(resource);
        }

        return input;
    }

    /** Convert inputStream to String. Useful for reading .txt or .json that are inside the jar file */
    public static String convertInputStreamToString(InputStream inputStream) {
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();

        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result.toString();
    }
}
