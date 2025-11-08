package com.dervarex.PandaClient.Minecraft.logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

public class LogWindow {
    private JTextPane logPane;
    private StyledDocument doc;
    private final List<String[]> logList; // für Filter

    public LogWindow() {
        logList = new ArrayList<>();

        // Build UI on the Swing Event Dispatch Thread to avoid threading issues
        SwingUtilities.invokeLater(() -> {
            // Hauptfenster
            JFrame frame = new JFrame("Log Window");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // don't exit the JVM when closing
            frame.setSize(700, 500);
            frame.setLayout(new BorderLayout());

            // Log Pane
            logPane = new JTextPane();
            logPane.setEditable(false);
            logPane.setBackground(new Color(30, 30, 30));
            logPane.setForeground(new Color(220, 220, 220));
            logPane.setCaretColor(Color.WHITE);
            doc = logPane.getStyledDocument();

            JScrollPane scrollPane = new JScrollPane(logPane);
            frame.add(scrollPane, BorderLayout.CENTER);

            // Obere Leiste: Filter + Copy
            JPanel topPanel = new JPanel();
            topPanel.setBackground(new Color(45,45,45));
            topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

            String[] levels = {"ALL", "INFO", "WARN", "ERROR"};
            JComboBox<String> levelFilter = new JComboBox<>(levels);
            levelFilter.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton arrow = new JButton("▼");
                    arrow.setBorder(BorderFactory.createEmptyBorder());
                    arrow.setContentAreaFilled(false);
                    arrow.setFocusPainted(false);
                    arrow.setForeground(Color.WHITE);
                    arrow.setPreferredSize(new Dimension(20, 20));
                    return arrow;
                }

                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                    g.setColor(new Color(30, 30, 30)); // Hintergrund
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            });

            levelFilter.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    if (index == -1) {
                        setBackground(new Color(30, 30, 30));
                    } else {
                        setBackground(isSelected ? new Color(60, 60, 60) : new Color(45, 45, 45));
                    }

                    setForeground(switch(value.toString()) {
                        case "INFO" -> new Color(200, 200, 200);
                        case "WARN" -> new Color(255, 165, 0);
                        case "ERROR" -> new Color(255, 80, 80);
                        default -> Color.WHITE;
                    });

                    setBorder(null);
                    return this;
                }
            });
            levelFilter.setBackground(new Color(30, 30, 30));
            levelFilter.setForeground(new Color(220, 220, 220));
            levelFilter.setPreferredSize(new Dimension(100, 30));
            levelFilter.setMaximumSize(new Dimension(100, 30));
            levelFilter.setMinimumSize(new Dimension(100, 30));
            levelFilter.setFont(new Font("Arial", Font.PLAIN, 12));
            levelFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            levelFilter.setFocusable(false);
            levelFilter.setBorder(BorderFactory.createEmptyBorder());
            levelFilter.setSelectedIndex(0);
            levelFilter.setToolTipText("Filter logs by level");
            levelFilter.setOpaque(true);

            topPanel.add(levelFilter);

            JButton copyButton = new JButton("Copy Logs");
            copyButton.setForeground(Color.WHITE);
            copyButton.setBackground(new Color(30, 30, 30));
            copyButton.setFocusPainted(false);
            copyButton.setBorderPainted(false);
            copyButton.setOpaque(true);
            copyButton.setContentAreaFilled(false);
            copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            copyButton.setPreferredSize(new Dimension(100, 30));
            copyButton.setMaximumSize(new Dimension(100, 30));
            copyButton.setMinimumSize(new Dimension(100, 30));
            copyButton.setFont(new Font("Arial", Font.PLAIN, 12));
            copyButton.setVerticalAlignment(SwingConstants.CENTER);
            copyButton.setHorizontalAlignment(SwingConstants.CENTER);
            copyButton.setMargin(new Insets(0, 0, 0, 0));
            copyButton.setBorder(BorderFactory.createEmptyBorder());
            copyButton.setFocusable(false);
            copyButton.setRolloverEnabled(false);
            topPanel.add(copyButton);

            frame.add(topPanel, BorderLayout.NORTH);

            // Copy Button Funktion
            copyButton.addActionListener(e -> {
                String text = logPane.getText();
                StringSelection selection = new StringSelection(text);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            });

            // Filter-Funktion
            levelFilter.addActionListener(e -> {
                String selected = (String) levelFilter.getSelectedItem();
                refreshLogs(selected);
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Log hinzufügen (thread-safe)
    public void log(String level, String message) {
        // schedule update on EDT to be safe
        SwingUtilities.invokeLater(() -> {
            logList.add(new String[]{level, message});
            insertLog(level, message);
        });
    }

    // Logs anzeigen (mit Filter) - must be called on EDT
    private void refreshLogs(String filter) {
        // if called off-EDT, schedule
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshLogs(filter));
            return;
        }

        logPane.setText(""); // Pane leeren
        for(String[] entry : logList){
            if(filter.equals("ALL") || entry[0].equalsIgnoreCase(filter)){
                insertLog(entry[0], entry[1]);
            }
        }
    }

    // Log mit Farbe einfügen - must be called on EDT
    private void insertLog(String level, String message){
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> insertLog(level, message));
            return;
        }

        Color color;
        switch(level.toUpperCase()){
            case "INFO": color = new Color(200,200,200); break; // helles Grau
            case "WARN": color = new Color(255,165,0); break; // Orange
            case "ERROR": color = new Color(255,80,80); break; // Rot
            default: color = new Color(180,180,180); break; // Grau
        }

        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, color);
        StyleConstants.setFontFamily(attr, "Monospaced");
        StyleConstants.setFontSize(attr, 12);

        try {
            doc.insertString(doc.getLength(), "["+level+"] "+message+"\n", attr);
            logPane.setCaretPosition(doc.getLength()); // Auto-Scroll
        } catch(BadLocationException e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            ClientLogger.log(sw.toString(), "ERROR", "LogWindow");
        }
    }

}
