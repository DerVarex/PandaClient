package com.dervarex.PandaClient.server.ui;

import com.dervarex.PandaClient.Main;
import com.dervarex.PandaClient.server.ServerConfig;
import com.dervarex.PandaClient.server.ServerManager;
import com.dervarex.PandaClient.server.ServerType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class CreateServer extends JFrame {
    public CreateServer() {
        setTitle("Create New Server");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- COLORS & FONTS (modern, dark, subtle green accent) ---
        Color bg = new Color(24, 24, 27);          // window background
        Color card = new Color(32, 36, 40);        // card background
        Color accent = new Color(76, 175, 80);     // green accent
        Color textPrimary = new Color(245, 245, 247);
        Color textSecondary = new Color(180, 180, 185);

        Font titleFont = new Font("SansSerif", Font.BOLD, 18);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 13);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);

        setBackground(bg);
        getContentPane().setBackground(bg);

        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 67)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
        getRootPane().setBorder(border);

        // --- MAIN CARD PANEL ---
        JPanel panel = new RoundedPanel(new BorderLayout(0, 16), 18);
        panel.setOpaque(false);
        panel.setBackground(card);

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Create Server");
        title.setFont(titleFont);
        title.setForeground(textPrimary);

        JLabel subtitle = new JLabel("Set up a new Minecraft server instance");
        subtitle.setFont(labelFont);
        subtitle.setForeground(textSecondary);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(title);
        texts.add(Box.createVerticalStrut(2));
        texts.add(subtitle);

        header.add(texts, BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        // --- FORM ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name");
        nameLabel.setFont(labelFont);
        nameLabel.setForeground(textSecondary);
        form.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        JTextField nameField = createTextField(fieldFont, card, textPrimary, textSecondary);
        form.add(nameField, gbc);

        // Software (Server type)
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel softwareLabel = new JLabel("Software");
        softwareLabel.setFont(labelFont);
        softwareLabel.setForeground(textSecondary);
        form.add(softwareLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        JComboBox<ServerType> softwareCombo = new JComboBox<>(ServerType.values());
        softwareCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(fieldFont);
                if (value instanceof ServerType type) {
                    lbl.setText(type.getDisplayName());
                }
                if (isSelected) {
                    lbl.setBackground(accent.darker());
                    lbl.setForeground(textPrimary);
                } else {
                    lbl.setBackground(card);
                    lbl.setForeground(textPrimary);
                }
                return lbl;
            }
        });
        styleComboBox(softwareCombo, fieldFont, card, textPrimary, textSecondary);
        form.add(softwareCombo, gbc);

        // Version (simple text field for now)
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel versionLabel = new JLabel("Version");
        versionLabel.setFont(labelFont);
        versionLabel.setForeground(textSecondary);
        form.add(versionLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JTextField versionField = createTextField(fieldFont, card, textPrimary, textSecondary);
        form.add(versionField, gbc);

        // --- EULA row (checkbox + text + link) ---
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;

        JPanel eulaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        eulaPanel.setOpaque(false);

        JCheckBox checkbox = new JCheckBox();
        checkbox.setOpaque(false);

        JLabel eulaText = new JLabel("I agree to the Mojang ");
        eulaText.setFont(labelFont);
        eulaText.setForeground(textSecondary);

        JLabel eulaLink = new JLabel("EULA");
        eulaLink.setFont(labelFont);
        eulaLink.setForeground(new Color(102, 187, 255)); // link color
        eulaLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eulaLink.setText("<html><u>EULA</u></html>");

        Color linkNormal = new Color(102, 187, 255);
        Color linkHover  = new Color(144, 202, 249);

        eulaLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                eulaLink.setForeground(linkHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                eulaLink.setForeground(linkNormal);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            desktop.browse(new URI("https://account.mojang.com/documents/minecraft_eula"));
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CreateServer.this,
                            "Could not open EULA link. Please open it manually:\n" +
                                    "https://account.mojang.com/documents/minecraft_eula",
                            "Open EULA",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        eulaPanel.add(checkbox);
        eulaPanel.add(eulaText);
        eulaPanel.add(eulaLink);

        form.add(eulaPanel, gbc);

        //        // Port
//        gbc.gridx = 0; gbc.gridy = 3;
//        JLabel portLabel = new JLabel("Port (optional)");
//        portLabel.setFont(labelFont);
//        portLabel.setForeground(textSecondary);
//        form.add(portLabel, gbc);
//
//        gbc.gridx = 1; gbc.gridy = 3;
//        JTextField portField = createTextField(fieldFont, card, textPrimary, textSecondary);
//        form.add(portField, gbc);

        panel.add(form, BorderLayout.CENTER);

        // --- BUTTONS ---
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttons.setOpaque(false);

        JButton cancel = new JButton("Cancel");
        styleSecondaryButton(cancel, fieldFont, card, textPrimary, textSecondary);
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton("Create Server");
        stylePrimaryButton(save, fieldFont, accent, textPrimary);
        save.addActionListener(e -> {
            if (!checkbox.isSelected()) {
                JOptionPane.showMessageDialog(this,
                        "You must agree to the EULA to create a server.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String name = nameField.getText().trim();
            String version = versionField.getText().trim();
            ServerType type = (ServerType) softwareCombo.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a server name.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (version.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a Minecraft version.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Create & start server and open dashboard
                Main.getServerConfig().createServer(name, version, type != null ? type : ServerType.VANILLA, ServerManager.getServerDir(name));
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Failed to create or start server:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(cancel);
        buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
    }

    // --- SMALL HELPERS FOR STYLING ---

    private JTextField createTextField(Font font, Color bg, Color fg, Color placeholder) {
        JTextField field = new JTextField();
        field.setFont(font);
        field.setForeground(fg);
        field.setCaretColor(fg);
        field.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 67)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<?> combo, Font font, Color bg, Color fg, Color secondary) {
        combo.setFont(font);
        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 67)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        if (combo.getEditor().getEditorComponent() instanceof JComponent editor) {
            editor.setBackground(bg);
            editor.setForeground(fg);
        }
    }

    private void stylePrimaryButton(JButton button, Font font, Color accent, Color fg) {
        button.setFont(font);
        button.setBackground(accent);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button, Font font, Color bg, Color fg, Color borderColor) {
        button.setFont(font);
        button.setContentAreaFilled(false);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(borderColor));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CreateServer createServer = new CreateServer();
            createServer.setVisible(true);
        });
    }

    static class RoundedPanel extends JPanel {
        private final int arc;

        public RoundedPanel(LayoutManager layout, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintChildren(g);
        }
    }
}
