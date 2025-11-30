package com.dervarex.PandaClient.server.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MainDashboard extends JFrame {

    private final JTextArea consoleOut = new JTextArea();
    private final List<String> consoleLogs = new ArrayList<>();

    // Optional: link to Server instance (set by ServerManager)
    private com.dervarex.PandaClient.server.Server attachedServer;

    public void attachServer(com.dervarex.PandaClient.server.Server server) {
        this.attachedServer = server;
    }

    public MainDashboard(String name, String version) {
        // --- WINDOW BASICS ---
        setTitle("PandaClient Server Dashboard");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Colors aligned with CreateServer
        Color bg = new Color(24, 24, 27);          // background
        Color card = new Color(32, 36, 40);        // card panels
        Color accent = new Color(76, 175, 80);     // green accent
        Color danger = new Color(220, 82, 82);     // red accent for kill
        Color textPrimary = new Color(245, 245, 247);
        Color textSecondary = new Color(180, 180, 185);

        Font titleFont = new Font("SansSerif", Font.BOLD, 20);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 13);

        getContentPane().setBackground(bg);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ----------------- Top Panel: Server Info + Controls -----------------
        RoundedPanel topPanel = new RoundedPanel(new BorderLayout(16, 10), 20);
        topPanel.setBackground(card);

        // Left: server title + meta info
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(name);
        title.setFont(titleFont);
        title.setForeground(textPrimary);

        JLabel subtitle = new JLabel("Minecraft Server • Version " + version);
        subtitle.setFont(labelFont);
        subtitle.setForeground(textSecondary);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        metaRow.setOpaque(false);
        metaRow.add(makeInfoLabel("TPS", "20.0", labelFont, textSecondary, textPrimary));
        metaRow.add(makeInfoLabel("Players", "3", labelFont, textSecondary, textPrimary));

        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(subtitle);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(metaRow);

        topPanel.add(headerPanel, BorderLayout.WEST);

        // Right: control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        JButton start = new JButton("Start");
        JButton stop = new JButton("Stop");
        JButton kill = new JButton("Kill");

        styleFilledButton(start, accent, textPrimary);
        styleFilledButton(stop, new Color(55, 59, 65), textPrimary);
        styleFilledButton(kill, danger, textPrimary);

        start.addActionListener(e -> {
            addConsoleLog("[SYSTEM] Start pressed");
            if (attachedServer != null) attachedServer.start();
        });
        stop.addActionListener(e -> {
            addConsoleLog("[SYSTEM] Stop pressed");
            if (attachedServer != null) attachedServer.stop();
        });
        kill.addActionListener(e -> {
            addConsoleLog("[SYSTEM] Kill pressed");
            if (attachedServer != null) attachedServer.shutdown();
        });

        buttonPanel.add(start);
        buttonPanel.add(stop);
        buttonPanel.add(kill);

        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ----------------- Main Panel (Left: Settings/Players/Plugins, Right: Console) -----------------
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // ----------------- Right: Console -----------------
        RoundedPanel consolePanel = new RoundedPanel(new BorderLayout(6, 6), 18);
        consolePanel.setBackground(card);

        JLabel consoleTitle = new JLabel("Console");
        consoleTitle.setForeground(textPrimary);
        consoleTitle.setFont(labelFont);
        consolePanel.add(consoleTitle, BorderLayout.NORTH);

        consoleOut.setEditable(false);
        consoleOut.setLineWrap(true);
        consoleOut.setWrapStyleWord(true);
        consoleOut.setBackground(new Color(16, 16, 19));   // dark, but not pure black
        consoleOut.setForeground(textPrimary);
        consoleOut.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(consoleOut);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        consolePanel.add(scrollPane, BorderLayout.CENTER);

        JTextField consoleIn = new JTextField();
        consoleIn.setBackground(new Color(28, 28, 32));   // slightly lighter than console area
        consoleIn.setForeground(textPrimary);
        consoleIn.setCaretColor(textPrimary);
        consoleIn.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JButton send = new JButton("Send");
        styleGhostButton(send, card, textPrimary, textSecondary);
        send.addActionListener(e -> {
            String cmd = consoleIn.getText().trim();
            if (!cmd.isEmpty()) {
                addConsoleLog("> " + cmd);
                consoleIn.setText("");
                if (attachedServer != null) {
                    // send command to server process stdin
                    // method will be added to Server class
                    attachedServer.sendCommand(cmd);
                }
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(consoleIn, BorderLayout.CENTER);
        inputPanel.add(send, BorderLayout.EAST);
        consolePanel.add(inputPanel, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.45;
        gbc.weighty = 1.0;
        mainPanel.add(consolePanel, gbc);

        // ----------------- Left: Settings / Players / Plugins -----------------
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints lgbc = new GridBagConstraints();
        lgbc.insets = new Insets(6, 6, 6, 6);
        lgbc.fill = GridBagConstraints.BOTH;

        // Settings Panel (top)
        RoundedPanel settingsPanel = new RoundedPanel(new BorderLayout(6, 6), 18);
        settingsPanel.setBackground(card);

        JLabel settingsLabel = new JLabel("Settings");
        settingsLabel.setForeground(textPrimary);
        settingsLabel.setFont(labelFont);
        settingsPanel.add(settingsLabel, BorderLayout.NORTH);

        // placeholder content
        JPanel settingsContent = new JPanel();
        settingsContent.setOpaque(false);
        settingsContent.add(new JLabel("Server settings coming soon"));
        settingsContent.getComponent(0).setForeground(textSecondary);
        settingsPanel.add(settingsContent, BorderLayout.CENTER);

        lgbc.gridx = 0;
        lgbc.gridy = 0;
        lgbc.weightx = 1.0;
        lgbc.weighty = 0.4;
        leftPanel.add(settingsPanel, lgbc);

        // Players & Plugins (bottom)
        JPanel bottomLeft = new JPanel(new GridLayout(1, 2, 8, 0));
        bottomLeft.setOpaque(false);

        // Players Panel
        RoundedPanel playersPanel = new RoundedPanel(new BorderLayout(4, 4), 18);
        playersPanel.setBackground(card);
        JLabel playersTitle = new JLabel("Players");
        playersTitle.setForeground(textPrimary);
        playersTitle.setFont(labelFont);
        playersPanel.add(playersTitle, BorderLayout.NORTH);

        String[] playerCols = {"Name", "Ping", "Status"};
        DefaultTableModel pm = new DefaultTableModel(playerCols, 0);
        pm.addRow(new Object[]{"dervarex", 42, "online"});
        pm.addRow(new Object[]{"friend", 70, "online"});
        pm.addRow(new Object[]{"bot", 999, "afk"});
        JTable playersTable = new JTable(pm);
        styleTable(playersTable, bg, card, textPrimary, textSecondary);
        playersPanel.add(new JScrollPane(playersTable), BorderLayout.CENTER);
        bottomLeft.add(playersPanel);

        // Plugins Panel
        RoundedPanel pluginsPanel = new RoundedPanel(new BorderLayout(4, 4), 18);
        pluginsPanel.setBackground(card);
        JLabel pluginsTitle = new JLabel("Plugins");
        pluginsTitle.setForeground(textPrimary);
        pluginsTitle.setFont(labelFont);
        pluginsPanel.add(pluginsTitle, BorderLayout.NORTH);

        String[] pluginCols = {"Plugin", "Version", "Enabled"};
        DefaultTableModel plm = new DefaultTableModel(pluginCols, 0);
        plm.addRow(new Object[]{"EssentialsX", "2.19.0", "✓"});
        plm.addRow(new Object[]{"WorldEdit", "7.2.10", "✓"});
        JTable pluginsTable = new JTable(plm);
        styleTable(pluginsTable, bg, card, textPrimary, textSecondary);
        pluginsPanel.add(new JScrollPane(pluginsTable), BorderLayout.CENTER);
        bottomLeft.add(pluginsPanel);

        lgbc.gridx = 0;
        lgbc.gridy = 1;
        lgbc.weightx = 1.0;
        lgbc.weighty = 0.6;
        leftPanel.add(bottomLeft, lgbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.55;
        gbc.weighty = 1.0;
        mainPanel.add(leftPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    // ----------------- Logging -----------------
    public void addConsoleLog(String log) {
        consoleLogs.add(log);
        refreshConsoleLog();
    }

    private void refreshConsoleLog() {
        // Take a snapshot to avoid ConcurrentModificationException if logs arrive while painting
        java.util.List<String> snapshot = new java.util.ArrayList<>(consoleLogs);
        SwingUtilities.invokeLater(() -> {
            consoleOut.setText("");
            for (String s : snapshot) {
                consoleOut.append(s + "\n");
            }
            consoleOut.setCaretPosition(consoleOut.getDocument().getLength());
        });
    }

    // ----------------- Helper -----------------
    private JLabel makeInfoLabel(String key, String value, Font labelFont, Color keyColor, Color valueColor) {
        JLabel l = new JLabel("<html><span style='color: " + toHtmlColor(keyColor) + "'>" + key + ": </span>" +
                "<span style='color: " + toHtmlColor(valueColor) + "'>" + value + "</span></html>");
        l.setFont(labelFont);
        return l;
    }

    private String toHtmlColor(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void styleFilledButton(JButton button, Color base, Color fg) {
        button.setBackground(base);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16)); // no visible border/outline
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        // simple hover/press transitions (no white outlines)
        Color hover = blend(base, Color.WHITE, 0.08f);
        Color pressed = blend(base, Color.BLACK, 0.18f);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // restore hover if still inside
                if (button.getBounds().contains(e.getPoint())) {
                    button.setBackground(hover);
                } else {
                    button.setBackground(base);
                }
            }
        });
    }

    private void styleGhostButton(JButton button, Color bg, Color fg, Color textSecondary) {
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12)); // no line border
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color hoverText = blend(fg, Color.WHITE, 0.15f);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(hoverText);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(fg);
            }
        });
    }

    private Color blend(Color c1, Color c2, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int red = (int) (c1.getRed() * (1 - r) + c2.getRed() * r);
        int green = (int) (c1.getGreen() * (1 - r) + c2.getGreen() * r);
        int blue = (int) (c1.getBlue() * (1 - r) + c2.getBlue() * r);
        return new Color(red, green, blue);
    }

    private void styleTable(JTable table, Color bg, Color card, Color textPrimary, Color textSecondary) {
        table.setBackground(card);
        table.setForeground(textPrimary);
        table.setGridColor(new Color(60, 60, 67));
        table.setSelectionBackground(new Color(60, 99, 72));
        table.setSelectionForeground(textPrimary);
        table.setRowHeight(22);
        table.getTableHeader().setBackground(bg.darker());
        table.getTableHeader().setForeground(textSecondary);
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

    public static void showUI() {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            MainDashboard ui = new MainDashboard("TestServer", "1.20.2");
            ui.setVisible(true);

            // Example: manual logging
            ui.addConsoleLog("[SYSTEM] Dashboard loaded");
        });
    }

    public static void main(String[] args) {
        showUI();
    }
}
