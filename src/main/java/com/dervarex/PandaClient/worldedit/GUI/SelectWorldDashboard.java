package com.dervarex.PandaClient.worldedit.GUI;

import com.dervarex.PandaClient.worldedit.World;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SelectWorldDashboard extends JPanel {

    public SelectWorldDashboard(List<World> worlds) {

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(14, 15, 19));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Select World");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JLabel subtitle = new JLabel("Pick the world you want to explore.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(180, 180, 190));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JTextField search = new JTextField();
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        search.setBackground(new Color(28, 30, 35));
        search.setForeground(new Color(220, 220, 220));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(74, 74, 82)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        search.setText("Filter worlds (coming soon)");
        search.setEditable(false);
        search.setFont(new Font("SansSerif", Font.PLAIN, 13));

        header.add(title);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(12));
        header.add(search);

        add(header, BorderLayout.NORTH);

        // Liste
        DefaultListModel<World> model = new DefaultListModel<>();
        worlds.forEach(model::addElement);

        JList<World> list = new JList<>(model);
        list.setCellRenderer(new WorldRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(72);
        list.setOpaque(false);
        list.setBackground(new Color(23, 25, 30));
        list.setBorder(null);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        JPanel listSurface = new JPanel(new BorderLayout());
        listSurface.setBackground(new Color(26, 28, 33));
        listSurface.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(62, 62, 72), 1, true),
                BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        listSurface.add(scroll, BorderLayout.CENTER);

        add(listSurface, BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionBar.setOpaque(false);
        actionBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton open = new JButton("Open World");
        open.putClientProperty("JButton.buttonType", "roundRect");
        open.putClientProperty("FlatLaf.style", "arc:24; background:linear-gradient(to right, #5C7BFF, #3C5DE6);");
        open.setForeground(Color.WHITE);
        open.setPreferredSize(new Dimension(150, 32));
        open.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        open.setFocusPainted(false);
        open.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));

        actionBar.add(open);
        add(actionBar, BorderLayout.SOUTH);

        open.addActionListener(e -> {
            World w = list.getSelectedValue();
            if (w != null) {
                System.out.println("Opening: " + w.getName() + " (" + w.getInstance() + ")");
                new WorldEditor(w);
            }
        });
    }
}
