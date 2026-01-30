package org.freeplane.plugin.ai.chat;

import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class LiveChatListDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final int MAXIMUM_ROOT_TEXT_LENGTH = 40;
    private static final String CONTINUATION_MARK = " ...";

    private final LiveChatSessionManager sessionManager;
    private final LiveChatSessionTableModel tableModel;
    private final JTable table;
    private final LiveChatListHandler listHandler;

    LiveChatListDialog(AIChatPanel owner,
                       LiveChatSessionManager sessionManager,
                       AvailableMaps availableMaps,
                       TextController textController,
                       LiveChatListHandler listHandler) {
        super(findOwnerWindow(owner), TextUtils.getText("ai_chat_chats_dialog"), ModalityType.DOCUMENT_MODAL);
        this.sessionManager = sessionManager;
        this.listHandler = listHandler;
        this.tableModel = new LiveChatSessionTableModel(
            sessionManager,
            availableMaps,
            textController,
            TextUtils.getText("ai_chat_chats_column_name"),
            TextUtils.getText("ai_chat_chats_column_maps"));
        this.table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int column = table.columnAtPoint(event.getPoint());
                if (column == LiveChatSessionTableModel.COLUMN_NAME) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        JButton switchButton = TranslatedElementFactory.createButton("ai_chat_chats_switch");
        switchButton.addActionListener(event -> switchToSelected());
        JButton closeButton = TranslatedElementFactory.createButton("ai_chat_chats_close");
        closeButton.addActionListener(event -> closeSelected());
        table.getSelectionModel().addListSelectionListener(event -> updateButtonState(switchButton));

        JScrollPane scrollPane = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
        JPanel buttonPanel = new JPanel(flowLayout);
        buttonPanel.add(closeButton);
        buttonPanel.add(switchButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(600, 360);
        setLocationRelativeTo(owner);
        updateButtonState(switchButton);
    }

    void openDialog() {
        refresh();
        selectCurrentSession();
        setVisible(true);
    }

    void refresh() {
        tableModel.refresh();
        selectCurrentSession();
    }

    private void switchToSelected() {
        LiveChatSessionId sessionId = tableModel.sessionIdAt(table.getSelectedRow());
        if (sessionId == null) {
            return;
        }
        listHandler.switchTo(sessionId);
        dispose();
    }

    private void selectCurrentSession() {
        int rowIndex = tableModel.rowIndexForSession(sessionManager.getCurrentSessionId());
        if (rowIndex >= 0) {
            table.setRowSelectionInterval(rowIndex, rowIndex);
            return;
        }
        if (tableModel.getRowCount() > 0 && table.getSelectedRow() < 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void closeSelected() {
        dispose();
    }

    private static Window findOwnerWindow(AIChatPanel owner) {
        if (owner == null) {
            return null;
        }
        return SwingUtilities.getWindowAncestor(owner);
    }

    private void updateButtonState(JButton switchButton) {
        boolean hasSelection = table.getSelectedRow() >= 0;
        switchButton.setEnabled(hasSelection);
    }

    interface LiveChatListHandler {
        void switchTo(LiveChatSessionId sessionId);
        void close(LiveChatSessionId sessionId);
        void rename(LiveChatSessionId sessionId, String displayName);
    }

    private static final class LiveChatSessionTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final int COLUMN_NAME = 0;
        private static final int COLUMN_MAPS = 1;

        private final LiveChatSessionManager sessionManager;
        private final AvailableMaps availableMaps;
        private final TextController textController;
        private List<LiveChatSessionSummary> sessions;

        private final String nameColumnLabel;
        private final String mapsColumnLabel;

        private LiveChatSessionTableModel(LiveChatSessionManager sessionManager,
                                          AvailableMaps availableMaps,
                                          TextController textController,
                                          String nameColumnLabel,
                                          String mapsColumnLabel) {
            this.sessionManager = sessionManager;
            this.availableMaps = availableMaps;
            this.textController = textController;
            this.sessions = new ArrayList<>(sessionManager.listSessions());
            this.nameColumnLabel = nameColumnLabel;
            this.mapsColumnLabel = mapsColumnLabel;
        }

        @Override
        public int getRowCount() {
            return sessions.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == COLUMN_NAME ? nameColumnLabel : mapsColumnLabel;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COLUMN_NAME;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LiveChatSessionSummary summary = sessions.get(rowIndex);
            if (columnIndex == COLUMN_NAME) {
                return summary.getDisplayName();
            }
            return formatMapRootShortTextCounts(summary.getMapIds());
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != COLUMN_NAME) {
                return;
            }
            LiveChatSessionSummary summary = sessions.get(rowIndex);
            String displayName = value == null ? "" : value.toString().trim();
            if (displayName.isEmpty()) {
                return;
            }
            sessionManager.rename(summary.getId(), displayName);
            refresh();
        }

        LiveChatSessionId sessionIdAt(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= sessions.size()) {
                return null;
            }
            return sessions.get(rowIndex).getId();
        }

        int rowIndexForSession(LiveChatSessionId sessionId) {
            if (sessionId == null) {
                return -1;
            }
            for (int index = 0; index < sessions.size(); index++) {
                if (sessionId.equals(sessions.get(index).getId())) {
                    return index;
                }
            }
            return -1;
        }

        void refresh() {
            sessions = new ArrayList<>(sessionManager.listSessions());
            fireTableDataChanged();
        }

        private String formatMapRootShortTextCounts(List<String> mapIds) {
            if (mapIds == null || mapIds.isEmpty()) {
                return "";
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String mapId : mapIds) {
                String rootText = resolveRootShortText(mapId);
                if (rootText == null || rootText.isEmpty()) {
                    continue;
                }
                counts.put(rootText, counts.getOrDefault(rootText, 0) + 1);
            }
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey());
                if (entry.getValue() > 1) {
                    builder.append(" (x").append(entry.getValue()).append(")");
                }
            }
            return builder.toString();
        }

        private String resolveRootShortText(String mapId) {
            if (mapId == null || mapId.isEmpty()) {
                return null;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(mapId);
            } catch (IllegalArgumentException error) {
                return null;
            }
            MapModel mapModel = availableMaps.findMapModel(uuid);
            if (mapModel == null) {
                return null;
            }
            NodeModel rootNode = mapModel.getRootNode();
            if (rootNode == null) {
                return null;
            }
            return textController.getShortPlainText(rootNode, MAXIMUM_ROOT_TEXT_LENGTH, CONTINUATION_MARK);
        }
    }
}
