package com.dervarex.PandaClient.worldedit.GUI;

import com.dervarex.PandaClient.worldedit.World;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SelectWorldDashboard extends JPanel {

    public SelectWorldDashboard(List<World> worlds) {

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Select World");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(title, BorderLayout.NORTH);

        // Liste
        DefaultListModel<World> model = new DefaultListModel<>();
        worlds.forEach(model::addElement);

        JList<World> list = new JList<>(model);
        list.setCellRenderer(new WorldRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(60);
        list.setOpaque(false);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        add(scroll, BorderLayout.CENTER);

        // Optional: ActionButton
        JButton open = new JButton("Open World");
        open.putClientProperty("JButton.buttonType", "roundRect");
        open.putClientProperty("FlatLaf.style", "arc:20;");

        add(open, BorderLayout.SOUTH);

        open.addActionListener(e -> {
            World w = list.getSelectedValue();
            if (w != null) {
                System.out.println("Opening: " + w.getName() + " (" + w.getInstance() + ")");
                new WorldEditor(w);
            }
        });
    }
}
