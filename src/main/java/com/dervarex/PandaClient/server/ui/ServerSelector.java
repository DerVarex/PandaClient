package com.dervarex.PandaClient.server.ui;

import com.dervarex.PandaClient.server.Server;
import com.dervarex.PandaClient.server.ServerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ServerSelector extends JFrame {

    private final DefaultListModel<Server> serverListModel = new DefaultListModel<>();
    private final JList<Server> serverJList = new JList<>(serverListModel);

    public ServerSelector() {
        setTitle("PandaClient Servers");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        Color bg = new Color(24, 24, 27);
        Color card = new Color(32, 36, 40);
        Color accent = new Color(76, 175, 80);
        Color textPrimary = new Color(245, 245, 247);
        Color textSecondary = new Color(180, 180, 185);

        Font titleFont = new Font("SansSerif", Font.BOLD, 18);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 13);

        getContentPane().setBackground(bg);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Servers");
        title.setFont(titleFont);
        title.setForeground(textPrimary);
        JLabel subtitle = new JLabel("Select a server or create a new one");
        subtitle.setFont(labelFont);
        subtitle.setForeground(textSecondary);
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);
        header.add(textPanel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Center: list
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        serverJList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(labelFont);
                if (value instanceof Server s) {
                    lbl.setText(s.getName() + "  •  " + s.getVersion() + (s.isRunning() ? "  (running)" : ""));
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
        serverJList.setBackground(card);
        serverJList.setSelectionBackground(new Color(60, 99, 72));
        serverJList.setSelectionForeground(textPrimary);
        JScrollPane scrollPane = new JScrollPane(serverJList);
        scrollPane.getViewport().setBackground(card);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);

        JButton create = new JButton("Create Server");
        stylePrimaryButton(create, labelFont, accent, textPrimary);
        create.addActionListener(e -> {
            CreateServer cs = new CreateServer();
            cs.setVisible(true);
        });

        JButton open = new JButton("Open Dashboard");
        styleSecondaryButton(open, labelFont, card, textPrimary, textSecondary);
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Server selected = serverJList.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(ServerSelector.this, "Please select a server first.", "No selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    // Start existing server and show dashboard; ServerManager handles wiring
                    ServerManager.startExisting(selected.getName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ServerSelector.this, "Failed to open server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        buttons.add(create);
        buttons.add(open);
        add(buttons, BorderLayout.SOUTH);

        reloadServers();
    }

    private void reloadServers() {
        serverListModel.clear();
        List<Server> servers = ServerManager.listServers();
        for (Server s : servers) {
            serverListModel.addElement(s);
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

    private void styleSecondaryButton(JButton button, Font font, Color bg, Color fg, Color textSecondary) {
        button.setFont(font);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}

