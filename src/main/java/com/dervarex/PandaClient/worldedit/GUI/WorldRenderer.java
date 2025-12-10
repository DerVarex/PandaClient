package com.dervarex.PandaClient.worldedit.GUI;

import com.dervarex.PandaClient.worldedit.World;

import javax.swing.*;
import java.awt.*;

public class WorldRenderer extends JPanel implements ListCellRenderer<World> {

    private final JLabel name = new JLabel();
    private final JLabel instance = new JLabel();

    public WorldRenderer() {
        setLayout(new BorderLayout());
        setOpaque(false);

        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        instance.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instance.setForeground(new Color(180, 180, 180));

        JPanel text = new JPanel(new GridLayout(2,1));
        text.setOpaque(false);
        text.add(name);
        text.add(instance);

        add(text, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends World> list,
            World value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
    {
        name.setText(value.getName());
        instance.setText("Instance: " + value.getInstance().getName());

        if (isSelected) {
            setBackground(new Color(60, 60, 60));
        } else {
            setBackground(new Color(40, 40, 40));
        }

        // corners
        putClientProperty("FlatLaf.style", "arc:12; background:@background;");

        setOpaque(true);
        return this;
    }
}
