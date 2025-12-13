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
        name.setForeground(Color.WHITE);
        instance.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instance.setForeground(new Color(170, 170, 190));

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
            boolean cellHasFocus
    ){
        name.setText(value.getName());
        instance.setText("Instance: " + value.getInstance().getName());

        Color fill = isSelected ? new Color(65, 72, 92) : new Color(38, 40, 46);
        setBackground(fill);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? new Color(110, 122, 255) : new Color(48, 51, 60), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        putClientProperty("FlatLaf.style", "arc:18; background:@background;");

        setOpaque(true);
        return this;
    }
}
