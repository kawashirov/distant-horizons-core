package com.seibel.lod.core.jar.installer;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Does something simmilar to wget
 * It allows you to download a file from a link
 *
 * @author coolGi
 */
public class WebDownloader {
    public static boolean netIsAvailable() {
        try {
            final URL url = new URL("https://gitlab.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static void downloadAsFile(URL url, File file) throws Exception {
//        URL url = new URL(urlS);

        HttpsURLConnection connection = (HttpsURLConnection) url
                .openConnection();
        long filesize = connection.getContentLengthLong();
        if (filesize == -1) {
            throw new Exception("Content length must not be -1 (unknown)!");
        }
        long totalDataRead = 0;
        try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(
                connection.getInputStream())) {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            try (java.io.BufferedOutputStream bout = new BufferedOutputStream(
                    fos, 1024)) {
                byte[] data = new byte[1024];
                int i;
                while ((i = in.read(data, 0, 1024)) >= 0) {
                    totalDataRead = totalDataRead + i;
                    bout.write(data, 0, i);
//                        int percent = (int) ((totalDataRead * 100) / filesize);
//                        System.out.println(percent);
                }
            }
        }
    }

    public static String downloadAsString(URL url) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
//        URL url = new URL(urlS);

        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(1000);
        urlConnection.setReadTimeout(1000);
        BufferedReader bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String line;
        while ((line = bReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return (stringBuilder.toString());
    }
}
