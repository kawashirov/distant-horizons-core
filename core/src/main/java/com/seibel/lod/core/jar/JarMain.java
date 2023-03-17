package com.seibel.lod.core.jar;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.jar.DarkModeDetector;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.gui.BaseJFrame;
import com.seibel.lod.core.jar.gui.cusomJObject.JBox;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
import com.seibel.lod.core.jar.installer.WebDownloader;
import com.seibel.lod.core.jar.JarDependencySetup;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
// Once built it would be in core/build/libs/DistantHorizons-<Version>-dev-all.jar
public class JarMain extends Application {
    public static final Logger logger = LogManager.getLogger(JarMain.class.getSimpleName());
    public static List<String> programArgs;
    public static final boolean isDarkTheme = DarkModeDetector.isDarkMode();
    public static boolean isOffline = WebDownloader.netIsAvailable();

    @Override
    public void start(Stage stage) {
        logger.debug("JavaFX version "+System.getProperty("javafx.version"));

        Label l = new Label("Hello, JavaFX ");
        Scene scene = new Scene(new StackPane(l), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        programArgs = Arrays.asList(args);

        if (!programArgs.contains("--no-custom-logger")) {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            try {
                context.setConfigLocation(JarUtils.accessFileURI("/log4jConfig.xml"));
            } catch (Exception e) {
                logger.error("Failed to set log4j config. Try running with the \"--no-custom-logger\" argument");
                e.printStackTrace();
            }
        }

        logger.debug("Running "+ModInfo.READABLE_NAME+" standalone jar");
        logger.warn("The standalone jar is still a massive WIP, expect bugs");
        logger.debug("Java version "+System.getProperty("java.version"));
//        logger.debug(programArgs);

        // Sets up the local
        if (JarUtils.accessFile("assets/lod/lang/" + Locale.getDefault().toString().toLowerCase() + ".json") == null) {
            logger.warn("The language setting [" + Locale.getDefault().toString().toLowerCase() + "] isn't allowed yet. Defaulting to [" + Locale.US.toString().toLowerCase() + "].");
            Locale.setDefault(Locale.US);
        }
        JarDependencySetup.createInitialBindings();

        if (args.length == 0 || Arrays.asList(args).contains("--gui")) {
            launch(args);
            return;
        }
    }




    public enum OperatingSystem {WINDOWS, MACOS, LINUX, NONE} // Easy to use enum for the 3 main os's
    public static OperatingSystem getOperatingSystem() { // Get the os and turn it into that enum
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.NONE;
        }
    }
}
