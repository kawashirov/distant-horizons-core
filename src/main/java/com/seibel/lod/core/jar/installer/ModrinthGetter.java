package com.seibel.lod.core.jar.installer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URL;
import java.util.*;

/**
 * Gets the releases available on Modrinth and allows you to perform actions with them
 *
 * @author coolGi
 */
public class ModrinthGetter {
    public static final String ModrinthAPI = "https://api.modrinth.com/v2/project/";
    public static final String projectID = "distanthorizons";
    public static JSONArray projectRelease = new JSONArray();

    public static List<String> releaseID = new ArrayList<>(); // This list contains the release ID's
    public static List<String> mcVersions = new ArrayList<>(); // List of available Minecraft versions in the mod
    /** Arg 1 = Release ID;
     * Arg 2 = Readable name */
    public static Map<String, String> releaseNames = new HashMap<>(); // This list contains the readable names of the ID's to the
    /** Arg 1 = Minecraft version;
     * Arg 2 = Compatible project ID's for that */
    public static Map<String, List<String>> mcVerToReleaseID = new HashMap<>();
    /** Arg 1 = ID;
     *  Arg 2 = Download URL */
    public static Map<String, URL> downloadUrl = new HashMap<>(); // Get the download url
    /** Arg 1 = ID;
     *  Arg 2 = Changelog */
    public static Map<String, String> changeLogs = new HashMap<>();


    public static boolean init() {
        try {
            projectRelease = (JSONArray) new JSONParser().parse(WebDownloader.downloadAsString(new URL(ModrinthAPI+projectID+"/version")));


            for (int i = 0; i < projectRelease.size(); i++) {
                JSONObject currentRelease = (JSONObject) projectRelease.get(i);
                String workingID = currentRelease.get("id").toString();

                releaseID.add(workingID);
                releaseNames.put(workingID, currentRelease.get("name").toString().replaceAll(" - 1\\..*", ""));
                changeLogs.put(workingID, currentRelease.get("changelog").toString());
                try {
                    downloadUrl.put(workingID, new URL(((JSONObject) ((JSONArray) currentRelease.get("files")).get(0)).get("url").toString()));
                } catch (Exception e) { e.printStackTrace(); }

                // Get all the mc versions this mod is available for
                for(String mcVer : (List<String>) currentRelease.get("game_versions")) {
                    if (!mcVersions.contains(mcVer)) {
                        mcVersions.add(mcVer);
                        mcVerToReleaseID.put(mcVer, new ArrayList<>());
                    }
                    mcVerToReleaseID.get(mcVer).add(workingID);
                }
            }
            // Sort them to look better
            Collections.sort(mcVersions);
            Collections.reverse(mcVersions);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getReleaseIDInMcVersion(String mcVersion) {
        List<String> releaseID = new ArrayList<>();

        for (int i = 0; i < projectRelease.size(); i++) {
            JSONObject currentRelease = (JSONObject) projectRelease.get(i);
            if (((List<String>) currentRelease.get("game_versions")).contains(mcVersion))
                releaseID.add(currentRelease.get("id").toString());
        }

        return releaseID;
    }
    public static URL getDownloadFromReleaseID(String ID) {
        for (int i = 0; i < projectRelease.size(); i++) {
            JSONObject currentRelease = (JSONObject) projectRelease.get(i);
            if (currentRelease.get("id").toString().equals(ID))
                try {
                    return new URL(((JSONObject) ((JSONArray) currentRelease.get("files")).get(0)).get("url").toString());
                } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }
}
