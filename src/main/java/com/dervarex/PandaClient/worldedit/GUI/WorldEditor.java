package com.dervarex.PandaClient.worldedit.GUI;

import com.dervarex.PandaClient.worldedit.*;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

public class WorldEditor extends JFrame {

    private final World world;
    private static final Color BG_PRIMARY = new Color(15, 17, 24);
    private static final Color BG_SURFACE = new Color(26, 29, 38);
    private static final Color FG_PRIMARY = new Color(234, 236, 245);
    private static final Color FG_MUTED = new Color(168, 173, 189);
    private static final String INPUT_STYLE = "arc:12; background:#1d1f2a; foreground:#f4f5ff; borderColor:#2f3245; focusColor:#7f8bff;";
    private static final String BUTTON_STYLE = "arc:14; background:#5b65ff; foreground:#ffffff; borderColor:#737cff; focusColor:#8e97ff;";
    private static final String ROW_KEY = "WorldEditor.rowIndex";

    public WorldEditor(World world) {
        this.world = world;

        setTitle("Editing: " + world.name);
        setSize(640, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, BG_PRIMARY);
        getContentPane().setBackground(BG_PRIMARY);

        setContentPane(buildUI());
        setVisible(true);
    }

    private JPanel buildUI() {
        JPanel base = new JPanel(new BorderLayout(0, 16));
        base.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        base.setBackground(BG_PRIMARY);

        JLabel title = new JLabel("World Editor – " + world.name);
        title.setForeground(FG_PRIMARY);
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +18;");
        base.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.putClientProperty(FlatClientProperties.STYLE,
                "background:#0f1118; foreground:#f5f6ff; selectedBackground:#1e2130; underlineColor:#647aff;" +
                        "selectedForeground:#ffffff; hoverColor:#7c88ff; inactiveUnderlineColor:#2b2f3d;");
        tabs.setBackground(BG_SURFACE);
        tabs.setForeground(FG_PRIMARY);
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createLineBorder(new Color(34, 37, 47), 1, true));
        tabs.addTab("General", generalPanel());
        tabs.addTab("Gameplay", gameplayPanel());
        tabs.addTab("Time", timePanel());

        base.add(tabs, BorderLayout.CENTER);
        return base;
    }

    private JPanel generalPanel() {
        JPanel p = panel();

        JTextField nameField = new JTextField(world.name);
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "World Name");
        nameField.putClientProperty(FlatClientProperties.STYLE, INPUT_STYLE);

        JButton saveName = button("Apply");
        saveName.addActionListener(e -> {
            world.changeName(nameField.getText());
            setTitle("Editing: " + world.name);
        });

        addRow(p, "World Name", nameField, saveName);

        JLabel version = new JLabel(world.version);
        version.setForeground(FG_PRIMARY);
        version.putClientProperty(FlatClientProperties.STYLE, "font: +1;");
        addRow(p, "Version", version);

        JLabel seed = new JLabel(String.valueOf(world.seed));
        seed.setForeground(FG_PRIMARY);
        addRow(p, "Seed", seed);

        return p;
    }

    private JPanel gameplayPanel() {
        JPanel p = panel();

        JComboBox<Difficulty> diffBox = new JComboBox<>(Difficulty.values());
        diffBox.setSelectedItem(world.difficulty);
        diffBox.putClientProperty(FlatClientProperties.STYLE, INPUT_STYLE);

        JButton applyDiff = button("Apply");
        applyDiff.addActionListener(e ->
                world.changeDifficulty((Difficulty) diffBox.getSelectedItem())
        );

        addRow(p, "Difficulty", diffBox, applyDiff);

        JCheckBox commands = new JCheckBox("Allow Commands", world.commands);
        commands.addActionListener(e ->
                world.changeCommands(commands.isSelected())
        );
        commands.setForeground(FG_PRIMARY);
        commands.setBackground(BG_SURFACE);
        commands.putClientProperty(FlatClientProperties.STYLE, INPUT_STYLE + " icon.focusWidth:1; icon.arc:8;");
        addRow(p, "Commands", commands);

        JComboBox<Gamemode> modeBox = new JComboBox<>(Gamemode.values());
        modeBox.putClientProperty(FlatClientProperties.STYLE, INPUT_STYLE + " font: bold;");
        modeBox.setSelectedItem(world.gamemode);
        JButton applyGM = button("Apply");
        applyGM.addActionListener(e ->
                world.changeGamemode((Gamemode) modeBox.getSelectedItem())
        );

        addRow(p, "Gamemode", modeBox, applyGM);

        return p;
    }

    private JPanel timePanel() {
        JPanel p = panel();

        JSlider daySlider = new JSlider(0, 24000, (int) world.DayTime);
        daySlider.putClientProperty(FlatClientProperties.STYLE,
                INPUT_STYLE + " trackHeight:6; thumbSize:18,18; outlineWidth:1; outlineColor:#556;");
        daySlider.setPaintTicks(true);
        daySlider.setPaintLabels(true);
        daySlider.setBackground(BG_SURFACE);

        JButton applyTime = button("Apply");
        applyTime.addActionListener(e ->
                world.changeDayTime(daySlider.getValue())
        );

        addRow(p, "DayTime", daySlider, applyTime);

        return p;
    }

    private JPanel panel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 49, 59), 1, true),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        p.putClientProperty(ROW_KEY, 0);
        return p;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE, BUTTON_STYLE + " focusWidth:1;");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(false);
        return b;
    }

    private void addRow(JPanel parent, String label, JComponent comp) {
        addRow(parent, label, comp, null);
    }

    private void addRow(JPanel parent, String label, JComponent comp, JComponent endComp) {
        GridBagConstraints c = new GridBagConstraints();

        Integer row = (Integer) parent.getClientProperty(ROW_KEY);
        if (row == null) {
            row = 0;
        }
        c.gridy = row;
        parent.putClientProperty(ROW_KEY, row + 1);
        c.insets = new Insets(12, 8, 12, 8);

        c.gridx = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel l = new JLabel(label);
        l.setForeground(FG_MUTED);
        parent.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        comp.setForeground(FG_PRIMARY);
        comp.setBackground(BG_SURFACE);
        comp.setBorder(comp instanceof JLabel ? null : comp.getBorder());
        parent.add(comp, c);

        if (endComp != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            parent.add(endComp, c);
        }
    }
}
