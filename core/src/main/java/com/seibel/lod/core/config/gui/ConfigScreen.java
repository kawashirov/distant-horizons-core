package com.seibel.lod.core.config.gui;

import javax.swing.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConfigScreen extends JFrame {
    private final JFXPanel fxPanel = new JFXPanel();

    public ConfigScreen() {
//        super("JavaFX in Swing");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(fxPanel, BorderLayout.CENTER);
//        setSize(300, 200);
//        setLocationRelativeTo(null);
//        setVisible(true);

        Platform.runLater(() -> initFX(fxPanel));
    }

    private void initFX(JFXPanel fxPanel) {
        Scene scene = createScene();
        fxPanel.setScene(scene);
    }

    private Scene createScene() {
        Label label = new Label("Hello from JavaFX!");
        return new Scene(label);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new ConfigScreen();
            frame.setSize(300, 200);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
