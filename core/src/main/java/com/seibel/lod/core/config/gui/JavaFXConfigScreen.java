package com.seibel.lod.core.config.gui;

import javax.swing.*;
import java.awt.*;

public class JavaFXConfigScreen extends AbstractScreen {
    static {
        System.setProperty("java.awt.headless", "false");
    }

    @Override
    public void init() {
        Frame frame = EmbeddedFrameUtil.embeddedFrameCreate(this.minecraftWindow);
        frame.add(new ExampleScreen());
        EmbeddedFrameUtil.placeAtCenter(frame, this.width, this.height, 100, 100, 1);
    }

    public class ExampleScreen extends JComponent {
        public ExampleScreen() {
            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 0.5;
            constraints.gridx = 0;
            constraints.gridy = 0;
            add(new JLabel("Hello World!"), constraints);
        }
    }


    @Override
    public void render(float delta) {

    }

    @Override
    public void onClose() {

    }
}
