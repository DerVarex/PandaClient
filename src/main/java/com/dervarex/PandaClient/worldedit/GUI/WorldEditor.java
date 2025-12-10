package com.dervarex.PandaClient.worldedit.GUI;

import com.dervarex.PandaClient.worldedit.*;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

public class WorldEditor extends JFrame {

    private final World world;

    public WorldEditor(World world) {
        this.world = world;

        setTitle("Editing: " + world.name);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setContentPane(buildUI());
        setVisible(true);
    }

    private JPanel buildUI() {
        JPanel base = new JPanel(new BorderLayout());
        base.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("World Editor – " + world.name);
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold +18;");
        base.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", generalPanel());
        tabs.addTab("Gameplay", gameplayPanel());
        tabs.addTab("Time", timePanel());

        base.add(tabs, BorderLayout.CENTER);
        return base;
    }

    // ------------------------------------------------------------
    // TAB: GENERAL
    // ------------------------------------------------------------

    private JPanel generalPanel() {
        JPanel p = panel();

        // Name -----------------------------------------------------
        JTextField nameField = new JTextField(world.name);
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "World Name");
        nameField.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        JButton saveName = button("Apply");
        saveName.addActionListener(e -> {
            world.changeName(nameField.getText());
            setTitle("Editing: " + world.name);
        });

        addRow(p, "World Name", nameField, saveName);

        // Version ---------------------------------------------------
        JLabel version = new JLabel(world.version);
        version.putClientProperty(FlatClientProperties.STYLE, "font: +1;");
        addRow(p, "Version", version);

        // Seed ------------------------------------------------------
        JLabel seed = new JLabel(String.valueOf(world.seed));
        addRow(p, "Seed", seed);

        return p;
    }


    // ------------------------------------------------------------
    // TAB: GAMEPLAY
    // ------------------------------------------------------------

    private JPanel gameplayPanel() {
        JPanel p = panel();

        // Difficulty
        JComboBox<Difficulty> diffBox =
                new JComboBox<>(Difficulty.values());
        diffBox.setSelectedItem(world.difficulty);
        diffBox.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        JButton applyDiff = button("Apply");
        applyDiff.addActionListener(e ->
                world.changeDifficulty((Difficulty) diffBox.getSelectedItem())
        );

        addRow(p, "Difficulty", diffBox, applyDiff);

        // Commands
        JCheckBox commands = new JCheckBox("Allow Commands", world.commands);
        commands.addActionListener(e ->
                world.changeCommands(commands.isSelected())
        );
        commands.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        addRow(p, "Commands", commands);

        // Gamemode
        JComboBox<Gamemode> modeBox =
                new JComboBox<>(Gamemode.values());
        modeBox.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");

        JButton applyGM = button("Apply");
        applyGM.addActionListener(e ->
                world.changeGamemode((Gamemode) modeBox.getSelectedItem())
        );

        addRow(p, "Gamemode", modeBox, applyGM);

        return p;
    }

    // ------------------------------------------------------------
    // TAB: TIME
    // ------------------------------------------------------------

    private JPanel timePanel() {
        JPanel p = panel();

        JSlider daySlider = new JSlider(0, 24000, (int) world.DayTime);
        daySlider.putClientProperty(FlatClientProperties.STYLE, "arc: 10;");
        daySlider.setPaintTicks(true);
        daySlider.setPaintLabels(true);

        JButton applyTime = button("Apply");
        applyTime.addActionListener(e ->
                world.changeDayTime(daySlider.getValue())
        );

        addRow(p, "DayTime", daySlider, applyTime);

        return p;
    }

    // ------------------------------------------------------------
    // UTILITY BUILDER FUNKTIONEN
    // ------------------------------------------------------------

    private JPanel panel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; background: #444; foreground: #fff;"
        );
        return b;
    }

    private void addRow(JPanel parent, String label, JComponent comp) {
        addRow(parent, label, comp, null);
    }

    private void addRow(JPanel parent, String label, JComponent comp, JComponent endComp) {
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = parent.getComponentCount() / 3;
        c.insets = new Insets(8, 8, 8, 8);

        // Label
        c.gridx = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.WEST;
        parent.add(new JLabel(label), c);

        // Component
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        parent.add(comp, c);

        // Optional Button
        if (endComp != null) {
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            parent.add(endComp, c);
        }
    }
}
