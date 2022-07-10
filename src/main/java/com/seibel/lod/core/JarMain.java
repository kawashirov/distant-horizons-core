package com.seibel.lod.core;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.jar.DarkModeDetector;
import com.seibel.lod.core.jar.gui.BaseJFrame;
import com.seibel.lod.core.jar.installer.GitlabGetter;
import com.seibel.lod.core.jar.JarDependencySetup;
import com.seibel.lod.core.jar.installer.WebDownloader;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main class when you run the standalone jar
 *
 * @author coolGi
 */
public class JarMain {
    public static final File jarFile = new File(JarMain.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    public static final boolean isDarkTheme = DarkModeDetector.isDarkMode();
    public static boolean isOffline = WebDownloader.netIsAvailable();

    public static void main(String[] args) {
        // Sets up the local
        if (JarMain.accessFile("assets/lod/lang/" + Locale.getDefault().toString().toLowerCase() + ".json") == null) {
            System.out.println("The language setting [" + Locale.getDefault().toString().toLowerCase() + "] isn't allowed yet. Defaulting to [" + Locale.US.toString().toLowerCase() + "].");
            Locale.setDefault(Locale.US);
        }
        // Set up the theme
        if (isDarkTheme)
            FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();


        JarDependencySetup.createInitialBindings();
        SingletonHandler.finishBinding();
        GitlabGetter.init();
        System.out.println("WARNING: The standalone jar still work in progress");

//        JOptionPane.showMessageDialog(null, "The GUI for the standalone jar isn't made yet\nIf you want to use the mod then put it in your mods folder", "Distant Horizons", JOptionPane.WARNING_MESSAGE);

        if (getOperatingSystem().equals(OperatingSystem.MACOS)) {
            System.out.println("If you want the installer then please use Linux or for the time being.\nMacOS support/testing will come later on");
        }

        BaseJFrame frame = new BaseJFrame(false, false);
        frame.addExtraButtons(true);
//        String[] optionsToChoose = {"Apple", "Orange", "Banana", "Pineapple"};
//        JComboBox<String> jTest = new JComboBox<>(optionsToChoose);
//        jTest.setBounds(400, 250, 140, 20);
//        frame.add(jTest);
//        jTest.addActionListener(e -> { System.out.println("test"); });


        // Buttons which you want to be stacked vertically should be added with this (`frame.add(obj, this);`)
        GridBagConstraints verticalLayout = new GridBagConstraints();
        verticalLayout.gridy = GridBagConstraints.RELATIVE;
        verticalLayout.gridx = 0;
        verticalLayout.fill = GridBagConstraints.HORIZONTAL;
        verticalLayout.weightx = 1.0;
        verticalLayout.anchor = GridBagConstraints.NORTH;


// Old style
/*
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
        minecraftDirBtn.setBounds(frame.getWidth() / 2 - (200 / 2), 250, 200, 20);
        frame.add(minecraftDirBtn);

        JComboBox<String> modVersions = new JComboBox<>(
                Arrays.copyOf(GitlabGetter.readableReleaseNames.toArray(), GitlabGetter.readableReleaseNames.toArray().length, String[].class)
        );
        modVersions.setBounds(frame.getWidth() / 2 - (200 / 2), 280, 200, 20);

        JComboBox<String> modMcVersion = new JComboBox<>();
        modMcVersion.setBounds(frame.getWidth() / 2 - (200 / 2), 310, 200, 20);
        modMcVersion.setModel(new DefaultComboBoxModel(
                Arrays.copyOf(
                        GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray(),
                        GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray().length,
                        String[].class
                ))
        );

        modVersions.addActionListener(e -> {
            modMcVersion.setModel(new DefaultComboBoxModel(
                    Arrays.copyOf(
                            GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray(),
                            GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(modVersions.getSelectedIndex())).toArray().length,
                            String[].class
                    ))
            );
        });
        frame.add(modVersions);
        frame.add(modMcVersion);


        // Fabric installer
//        try {
//            WebDownloader.downloadAsFile(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.0/fabric-installer-0.11.0.jar"), new File(System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar"));
//            Runtime.getRuntime().exec("java -jar " + System.getProperty("java.io.tmpdir") + "/fabricInstaller.jar");
//        } catch (Exception e) {e.printStackTrace();}

        JButton installMod = new JButton("Install " + ModInfo.READABLE_NAME);
        installMod.setBounds(frame.getWidth() / 2 - (200 / 2), 340, 200, 20);
        installMod.addActionListener(e -> {
            if (minecraftDirPop.getSelectedFile() == null) {
                JOptionPane.showMessageDialog(frame, "Please select your install directory", ModInfo.READABLE_NAME, JOptionPane.WARNING_MESSAGE);
                return;
            }

//            JOptionPane.showMessageDialog(frame, "Installing "+ModInfo.READABLE_NAME+" version "+modVersions.getSelectedItem()+" for Minecraft version "+modMcVersion.getSelectedItem()+" \nAt "+minecraftDirPop.getSelectedFile(), ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);

            URL downloadPath = GitlabGetter.getRelease(
                    GitlabGetter.releaseNames.get(modVersions.getSelectedIndex()),
                    (String) modMcVersion.getSelectedItem());
            try {
                if (downloadPath.toString().contains("curseforge.com"))
                    downloadPath = new URL(downloadPath.toString() + "/file");
            } catch (Exception f) {
                f.printStackTrace();
            }

            try {
                WebDownloader.downloadAsFile(
                        downloadPath,
                        minecraftDirPop.getSelectedFile().toPath().resolve(
                                ModInfo.NAME + "-" + GitlabGetter.releaseNames.get(modVersions.getSelectedIndex()) + "-" + ((String) modMcVersion.getSelectedItem()) + ".jar"
                        ).toFile());

                JOptionPane.showMessageDialog(frame, "Installation done. \nYou can now close the installer", ModInfo.READABLE_NAME, JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception f) {
                JOptionPane.showMessageDialog(frame, "Download failed. Check your internet connection \nStacktrace: " + f.getMessage(), ModInfo.READABLE_NAME, JOptionPane.ERROR_MESSAGE);
            }
        });
        frame.add(installMod);
*/
        // Selected versions
        // (What does atomic reference mean. I'm just using it cus java hates having normal strings)
        AtomicReference<String> modVersion = new AtomicReference<String>("");
        AtomicReference<String> minecraftVersion = new AtomicReference<String>("");



        // This is for the pannel to select MinecraftVersion
        JPanel modMinecraftVersionsPannel = new JPanel(new GridBagLayout());
        JScrollPane modMinecraftVersionsScroll = new JScrollPane(modMinecraftVersionsPannel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sets all the layout stuff for it
        modMinecraftVersionsScroll.setBounds(150, 225, 150, frame.getHeight()-255);
        modMinecraftVersionsScroll.setBorder(null); // Disables the border
        modMinecraftVersionsScroll.setWheelScrollingEnabled(true);
        // List to store all the buttons
        ArrayList<JButton> modMinecraftReleaseButtons = new ArrayList<>();
        // Add the pannel to the main frame
        frame.add(modMinecraftVersionsScroll);



        // This is for selecting the mod version
        JPanel modVersionsPannel = new JPanel(new GridBagLayout());
        JScrollPane modVersionsScroll = new JScrollPane(modVersionsPannel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sets all the layout stuff for it
        modVersionsScroll.setBounds(0, 225, 150, frame.getHeight()-255);
        modVersionsScroll.setBorder(null); // Disables the border
        modVersionsScroll.setWheelScrollingEnabled(true);
        // List to store all the buttons
        ArrayList<JButton> modReleaseButtons = new ArrayList<>();

        // Add all the buttons
        for (String release: GitlabGetter.readableReleaseNames) {
            JButton btn = new JButton(release);
            btn.setBackground(UIManager.getColor ("Panel.background")); // Does the same thing as removing the background
            btn.setBorderPainted(false); // Removes the borders
            btn.setHorizontalAlignment(SwingConstants.LEFT); // Sets the text to be on the left side rather than the center

            btn.addActionListener(e -> {
                modVersion.set(GitlabGetter.releaseNames.get(GitlabGetter.readableReleaseNames.indexOf(btn.getText())));

                // Clears the selected colors for the rest of the buttons
                for (JButton currentBtn: modReleaseButtons)
                    currentBtn.setBackground(UIManager.getColor ("Panel.background"));
                btn.setBackground(UIManager.getColor("Button.background")); // Sets this to the selected color

                // Clears the minecraft version panel
                modMinecraftVersionsPannel.removeAll();
                modMinecraftReleaseButtons.clear();

                // Adds all the buttons for the minecraft panel
                for (String releaseMC: GitlabGetter.getMcVersionsInRelease(GitlabGetter.releaseNames.get(GitlabGetter.readableReleaseNames.indexOf(btn.getText())))) {
                    // No need to comment most of these as it is the same this as before
                    JButton btnMC = new JButton(releaseMC);
                    btnMC.setBackground(UIManager.getColor ("Panel.background"));
                    btnMC.setBorderPainted(false);
                    btnMC.setHorizontalAlignment(SwingConstants.LEFT);

                    btnMC.addActionListener(f -> {
                        minecraftVersion.set(btnMC.getText());

                        for (JButton currentBtn: modMinecraftReleaseButtons)
                            currentBtn.setBackground(UIManager.getColor ("Panel.background"));
                        btnMC.setBackground(UIManager.getColor("Button.background"));
                    });
                    modMinecraftVersionsPannel.add(btnMC, verticalLayout);
                    modMinecraftReleaseButtons.add(btnMC);
                }
                modMinecraftVersionsPannel.repaint(); // Update the minecraft ver panel
                frame.validate(); // Update the frame
            });

            modVersionsPannel.add(btn, verticalLayout);
            modReleaseButtons.add(btn);
        }

        frame.add(modVersionsScroll);






        frame.addLogo(); // Has to be ran at the end cus of a bug with java swing (it may not be a bug but idk how to fix it so I'll call it a bug)

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

    /** Get a file within the mods resources */
    public static InputStream accessFile(String resource) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // this is the path within the jar file
        InputStream input = loader.getResourceAsStream("/resources/" + resource);
        if (input == null) {
            // this is how we load file within editor (eg eclipse)
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
