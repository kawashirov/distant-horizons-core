package com.seibel.lod.core.jar;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.jar.DarkModeDetector;
import com.seibel.lod.core.jar.JarUtils;
import com.seibel.lod.core.jar.gui.BaseJFrame;
import com.seibel.lod.core.jar.gui.cusomJObject.JBox;
import com.seibel.lod.core.jar.installer.ModrinthGetter;
import com.seibel.lod.core.jar.installer.WebDownloader;
import com.seibel.lod.core.jar.JarDependencySetup;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
// Once built it would be in core/build/libs/DistantHorizons-<Version>-dev-all.jar
public class JarMain {
    public static final boolean isDarkTheme = DarkModeDetector.isDarkMode();
    public static boolean isOffline = WebDownloader.netIsAvailable();

    public static void main(String[] args) {
        // Sets up the local
        if (JarUtils.accessFile("assets/lod/lang/" + Locale.getDefault().toString().toLowerCase() + ".json") == null) {
            System.out.println("The language setting [" + Locale.getDefault().toString().toLowerCase() + "] isn't allowed yet. Defaulting to [" + Locale.US.toString().toLowerCase() + "].");
            Locale.setDefault(Locale.US);
        }
        // Set up the theme
        System.setProperty("apple.awt.application.appearance", "system");
        if (isDarkTheme)
            FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();


        JarDependencySetup.createInitialBindings();
//        GitlabGetter.init();
        ModrinthGetter.init();
        System.out.println("WARNING: The standalone jar still work in progress");

//        JOptionPane.showMessageDialog(null, "The GUI for the standalone jar isn't made yet\nIf you want to use the mod then put it in your mods folder", "Distant Horizons", JOptionPane.WARNING_MESSAGE);

//        if (getOperatingSystem().equals(OperatingSystem.MACOS)) {
//            System.out.println("If you want the installer then please use Linux or Windows for the time being.\nMacOS support/testing will come later on");
//        }

        // Code will be changed later on to allow resizing and work better



        BaseJFrame frame = new BaseJFrame(false, true);
        frame.addExtraButtons(frame.getWidth(), 0, true, false);

        // Buttons which you want to be stacked vertically should be added with this (`frame.add(obj, this);`)
        GridBagConstraints verticalLayout = new GridBagConstraints();
        verticalLayout.gridy = GridBagConstraints.RELATIVE;
        verticalLayout.gridx = 0;
        verticalLayout.fill = GridBagConstraints.HORIZONTAL;
        verticalLayout.weightx = 1.0;
        verticalLayout.anchor = GridBagConstraints.NORTH;


        // Selected download
        AtomicReference<String> downloadID = new AtomicReference<String>("");


        // This is for the panel to show the update description
        JPanel modVersionDescriptionPanel = new JPanel(new GridBagLayout());
        JScrollPane modVersionDescriptionScroll = new JScrollPane(modVersionDescriptionPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sets all the layout stuff for it
        int modDescriptionWidth = 275;
        modVersionDescriptionScroll.setBounds(frame.getWidth()-modDescriptionWidth, 225, modDescriptionWidth, frame.getHeight()-255);
        modVersionDescriptionScroll.setBorder(null); // Disables the border
        modVersionDescriptionScroll.setWheelScrollingEnabled(true);
        // The label
        JLabel modVersionDescriptionLabel = new JLabel();
        modVersionDescriptionPanel.add(modVersionDescriptionLabel, verticalLayout);
        // Finally add it
        frame.add(modVersionDescriptionScroll);



        // This is for the pannel to select MinecraftVersion
        JPanel modMinecraftVersionsPannel = new JPanel(new GridBagLayout());
        JScrollPane modMinecraftVersionsScroll = new JScrollPane(modMinecraftVersionsPannel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sets all the layout stuff for it
        modMinecraftVersionsScroll.setBounds(0, 225, 125, frame.getHeight()-255);
        modMinecraftVersionsScroll.setBorder(null); // Disables the border
        modMinecraftVersionsScroll.setWheelScrollingEnabled(true);
        // List to store all the buttons
        ArrayList<JButton> modMinecraftReleaseButtons = new ArrayList<>();
        frame.add(modMinecraftVersionsScroll);


        // This is for selecting the mod version
        JPanel modVersionsPannel = new JPanel(new GridBagLayout());
        JScrollPane modVersionsScroll = new JScrollPane(modVersionsPannel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sets all the layout stuff for it
        modVersionsScroll.setBounds(125, 225, 100, frame.getHeight()-255);
        modVersionsScroll.setBorder(null); // Disables the border
        modVersionsScroll.setWheelScrollingEnabled(true);
        // List to store all the buttons
        ArrayList<JButton> modReleaseButtons = new ArrayList<>();
        frame.add(modVersionsScroll);

        // Add all the buttons
        for (String mcVer : ModrinthGetter.mcVersions) {
            JButton btn = new JButton(mcVer);
            btn.setBackground(UIManager.getColor("Panel.background")); // Does the same thing as removing the background
            btn.setBorderPainted(false); // Removes the borders
//            btn.setHorizontalAlignment(SwingConstants.LEFT); // Sets the text to be on the left side rather than the center

            btn.addActionListener(e -> {
                // Clears the selected colors for the rest of the buttons
                for (JButton currentBtn : modMinecraftReleaseButtons)
                    currentBtn.setBackground(UIManager.getColor("Panel.background"));
                btn.setBackground(UIManager.getColor("Button.background")); // Sets this to the selected color

                // Clears the minecraft version panel
                modVersionsPannel.removeAll();
                modReleaseButtons.clear();


                // Adds all the buttons for the minecraft panel
                for (String modID : ModrinthGetter.mcVerToReleaseID.get(mcVer)) {
                    // No need to comment most of these as it is the same this as before
                    JButton btnDownload = new JButton(ModrinthGetter.releaseNames.get(modID));
                    btnDownload.setBackground(UIManager.getColor("Panel.background"));
                    btnDownload.setBorderPainted(false);
                    btnDownload.setHorizontalAlignment(SwingConstants.LEFT);

                    btnDownload.addActionListener(f -> {
                        downloadID.set(modID);

                        for (JButton currentBtn : modReleaseButtons)
                            currentBtn.setBackground(UIManager.getColor("Panel.background"));
                        btnDownload.setBackground(UIManager.getColor("Button.background"));


                        modVersionDescriptionLabel.setText(
                                WebDownloader.formatMarkdownToHtml(
                                        ModrinthGetter.changeLogs.get(modID), modDescriptionWidth-75)
                        );
                        modVersionDescriptionPanel.repaint();
                    });
                    modVersionsPannel.add(btnDownload, verticalLayout);
                    modReleaseButtons.add(btnDownload);
                }

                modVersionsScroll.getVerticalScrollBar().setValue(0); // Reset the scroll bar back to the top

                modVersionsPannel.repaint(); // Update the version pannel
                frame.validate(); // Update the frame
            });

            modMinecraftVersionsPannel.add(btn, verticalLayout);
            modMinecraftReleaseButtons.add(btn);
        }



        // Bar at the top
        frame.add(new JBox(UIManager.getColor("Separator.foreground"), 0, 220, frame.getWidth(), 5));
        // Minecraft version text
        JLabel textMcVersionHeader = new JLabel("Minecraft version");
        textMcVersionHeader.setBounds(0, 200, 125, 20);
        frame.add(textMcVersionHeader);
        // Version text
        JLabel textVersionHeader = new JLabel("Mod version");
        textVersionHeader.setBounds(125, 200, 150, 20);
        frame.add(textVersionHeader);




        // Stuff for setting the file install path
        JFileChooser minecraftDirPop = new JFileChooser();
        if (getOperatingSystem().equals(OperatingSystem.WINDOWS))
            minecraftDirPop.setCurrentDirectory(new File(System.getenv("APPDATA") + "/.minecraft/mods"));
        if (getOperatingSystem().equals(OperatingSystem.LINUX))
            minecraftDirPop.setCurrentDirectory(new File(System.getProperty("user.home") + "/.minecraft/mods"));
        minecraftDirPop.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton minecraftDirBtn = new JButton("Click to select install path");
        minecraftDirBtn.addActionListener(e -> {
            if (minecraftDirPop.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                minecraftDirBtn.setText(minecraftDirPop.getSelectedFile().toString());
        });
        minecraftDirBtn.setBounds(230, frame.getHeight()-100, 200, 20);
        frame.add(minecraftDirBtn);

        // Button for the install button
        JButton installMod = new JButton("Install " + ModInfo.READABLE_NAME);
        installMod.setBounds(230, frame.getHeight()-70, 200, 20);
        installMod.addActionListener(e -> {
            if (minecraftDirPop.getSelectedFile() == null) {
                JOptionPane.showMessageDialog(frame, "Please select your install directory", ModInfo.READABLE_NAME, JOptionPane.WARNING_MESSAGE);
                return;
            }

            URL downloadPath = ModrinthGetter.downloadUrl.get(downloadID.get());

            try {
                WebDownloader.downloadAsFile(
                        downloadPath,
                        minecraftDirPop.getSelectedFile().toPath().resolve(
                                ModInfo.NAME + "-" + ModrinthGetter.releaseNames.get(downloadID.get()) + ".jar"
                        ).toFile());

                JOptionPane.showMessageDialog(frame, "Installation done. \nYou can now close the installer", ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception f) {
                JOptionPane.showMessageDialog(frame, "Download failed. Check your internet connection \nStacktrace: " + f.getMessage(), ModInfo.READABLE_NAME, JOptionPane.ERROR_MESSAGE);
            }
        });
        frame.add(installMod);


        // Fabric installer
//        try {
//            WebDownloader.downloadAsFile(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.0/fabric-installer-0.11.0.jar"), new File(System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar"));
//            Runtime.getRuntime().exec("java -jar " + System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar");
//        } catch (Exception e) {e.printStackTrace();}




        frame.addLogo(); // Has to be run at the end cus of a bug with java swing (it may not be a bug but idk how to fix it so I'll call it a bug)

        frame.validate(); // Update to add the widgets
        frame.setVisible(true); // Start the ui
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
            return OperatingSystem.NONE; // If you are the 0.00001% who don't use one of these 3 os's then you get light theme
        }
    }
}
