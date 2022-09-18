package com.seibel.lod.core.jar;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Some general utils for the jar
 * this includes stuff like accessing files inside the jar and checking the checksum of a file
 *
 * @author coolGi
 */
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

    /**
     * Checks the checksum of a file given an algorithm
     *
     * @param digest What algorithem to use <br>
     *               Eg. <br>
     *               MessageDigest.getInstance("MD5") <br>
     *               MessageDigest.getInstance("SHA-256") <br>
     * @param file Location of the file
     * @return Checksum
     */
    // Stolen from https://howtodoinjava.com/java/java-security/sha-md5-file-checksum-hash/
    public static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }
}
