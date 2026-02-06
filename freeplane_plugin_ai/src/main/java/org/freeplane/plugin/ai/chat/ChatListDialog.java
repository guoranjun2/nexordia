package org.freeplane.plugin.ai.chat;

import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptId;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptStatus;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptStore;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptSummary;
import org.freeplane.plugin.ai.chat.history.MapRootShortTextCount;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ChatListDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final LiveChatSessionManager sessionManager;
    private final ChatTranscriptStore transcriptStore;
    private final MapRootShortTextFormatter mapRootShortTextFormatter;
    private final ChatListHandler listHandler;
    private final ChatListTableModel tableModel;
    private final JTable table;
    private final JButton openChatButton;
    private final JButton deleteChatButton;
    private final JButton closeDialogButton;

    ChatListDialog(AIChatPanel owner,
                   LiveChatSessionManager sessionManager,
                   ChatTranscriptStore transcriptStore,
                   MapRootShortTextFormatter mapRootShortTextFormatter,
                   ChatListHandler listHandler) {
        super(findOwnerWindow(owner), TextUtils.getText("ai_chat_chats_dialog"), ModalityType.DOCUMENT_MODAL);
        this.sessionManager = sessionManager;
        this.transcriptStore = transcriptStore;
        this.mapRootShortTextFormatter = mapRootShortTextFormatter;
        this.listHandler = listHandler;
        this.tableModel = new ChatListTableModel(sessionManager, transcriptStore, mapRootShortTextFormatter,
            listHandler, TextUtils.getText("ai_chat_chats_column_name"),
            TextUtils.getText("ai_chat_chats_column_maps"));
        this.table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(ChatListTableModel.COLUMN_STATUS).setPreferredWidth(24);
        table.getColumnModel().getColumn(ChatListTableModel.COLUMN_NAME).setPreferredWidth(220);
        table.getColumnModel().getColumn(ChatListTableModel.COLUMN_MAPS).setPreferredWidth(320);
        table.getColumnModel().getColumn(ChatListTableModel.COLUMN_STATUS).setCellRenderer(
            new StatusIconRenderer(tableModel));
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int column = table.columnAtPoint(event.getPoint());
                if (column == ChatListTableModel.COLUMN_NAME) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int row = table.rowAtPoint(event.getPoint());
                if (row < 0) {
                    return;
                }
                table.setRowSelectionInterval(row, row);
                openChat();
            }
        });

        openChatButton = TranslatedElementFactory.createButton("ai_chat_chats_open");
        openChatButton.addActionListener(event -> openChat());
        deleteChatButton = new JButton(TextUtils.getText("delete"));
        deleteChatButton.addActionListener(event -> deleteChat());
        closeDialogButton = TranslatedElementFactory.createButton("ai_chat_chats_close");
        closeDialogButton.addActionListener(event -> closeDialog());
        table.getSelectionModel().addListSelectionListener(event -> updateButtonState());

        JScrollPane scrollPane = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
        JPanel buttonPanel = new JPanel(flowLayout);
        buttonPanel.add(openChatButton);
        buttonPanel.add(deleteChatButton);
        buttonPanel.add(closeDialogButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(720, 380);
        setLocationRelativeTo(owner);
        updateButtonState();
    }

    void openDialog() {
        refresh();
        selectCurrentSession();
        setVisible(true);
    }

    void refresh() {
        tableModel.refresh();
        selectCurrentSession();
        updateButtonState();
    }

    private void openChat() {
        ChatListItem item = tableModel.itemAt(table.getSelectedRow());
        if (item == null) {
            return;
        }
        if (item.getStatus() == ChatListItemStatus.LIVE) {
            listHandler.switchTo(item.getLiveSessionId());
            dispose();
        } else if (item.getStatus() == ChatListItemStatus.TRANSCRIPT) {
            listHandler.startChatFromTranscript(item.getTranscriptId());
            dispose();
        }
    }

    private void deleteChat() {
        ChatListItem item = tableModel.itemAt(table.getSelectedRow());
        if (item == null) {
            return;
        }
        if (item.getStatus() == ChatListItemStatus.LIVE) {
            if (item.getLiveSessionId() != null) {
                listHandler.deleteLiveSession(item.getLiveSessionId());
            }
            if (item.getTranscriptId() != null) {
                listHandler.deleteTranscript(item.getTranscriptId());
            }
            refresh();
            return;
        }
        if (item.getTranscriptId() != null) {
            listHandler.deleteTranscript(item.getTranscriptId());
            refresh();
        }
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

    private void closeDialog() {
        dispose();
    }

    private static Window findOwnerWindow(AIChatPanel owner) {
        if (owner == null) {
            return null;
        }
        return SwingUtilities.getWindowAncestor(owner);
    }

    private void updateButtonState() {
        ChatListItem item = tableModel.itemAt(table.getSelectedRow());
        boolean hasSelection = item != null;
        boolean isLive = hasSelection && item.getStatus() == ChatListItemStatus.LIVE;
        boolean isTranscript = hasSelection && item.getStatus() == ChatListItemStatus.TRANSCRIPT;
        openChatButton.setEnabled(isLive || isTranscript);
        deleteChatButton.setEnabled(hasSelection && (isLive || isTranscript || item.getStatus() == ChatListItemStatus.ERROR));
        closeDialogButton.setEnabled(true);
    }

    interface ChatListHandler {
        void switchTo(LiveChatSessionId sessionId);
        void close(LiveChatSessionId sessionId);
        void deleteLiveSession(LiveChatSessionId sessionId);
        void rename(LiveChatSessionId sessionId, String displayName);
        void renameTranscript(ChatTranscriptId transcriptId, String displayName);
        void startChatFromTranscript(ChatTranscriptId transcriptId);
        void deleteTranscript(ChatTranscriptId transcriptId);
    }

    private static class ChatListTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final int COLUMN_STATUS = 0;
        private static final int COLUMN_NAME = 1;
        private static final int COLUMN_MAPS = 2;

        private final LiveChatSessionManager sessionManager;
        private final ChatTranscriptStore transcriptStore;
        private final MapRootShortTextFormatter mapRootShortTextFormatter;
        private final ChatListHandler handler;
        private List<ChatListItem> items;
        private final String nameColumnLabel;
        private final String mapsColumnLabel;

        private ChatListTableModel(LiveChatSessionManager sessionManager,
                                   ChatTranscriptStore transcriptStore,
                                   MapRootShortTextFormatter mapRootShortTextFormatter,
                                   ChatListHandler handler,
                                   String nameColumnLabel,
                                   String mapsColumnLabel) {
            this.sessionManager = sessionManager;
            this.transcriptStore = transcriptStore;
            this.mapRootShortTextFormatter = mapRootShortTextFormatter;
            this.handler = handler;
            this.nameColumnLabel = nameColumnLabel;
            this.mapsColumnLabel = mapsColumnLabel;
            this.items = new ArrayList<>();
            refresh();
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            if (column == COLUMN_NAME) {
                return nameColumnLabel;
            }
            if (column == COLUMN_MAPS) {
                return mapsColumnLabel;
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex != COLUMN_NAME) {
                return false;
            }
            ChatListItem item = itemAt(rowIndex);
            return item != null && item.getStatus() != ChatListItemStatus.ERROR;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ChatListItem item = items.get(rowIndex);
            if (columnIndex == COLUMN_STATUS) {
                return item.getStatus();
            }
            if (columnIndex == COLUMN_NAME) {
                return item.getDisplayName();
            }
            if (columnIndex == COLUMN_MAPS) {
                return mapRootShortTextFormatter.formatCounts(item.getMapRootShortTextCounts());
            }
            return "";
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != COLUMN_NAME) {
                return;
            }
            ChatListItem item = itemAt(rowIndex);
            if (item == null) {
                return;
            }
            String displayName = value == null ? "" : value.toString().trim();
            if (displayName.isEmpty()) {
                return;
            }
            if (item.getLiveSessionId() != null) {
                handler.rename(item.getLiveSessionId(), displayName);
            } else if (item.getTranscriptId() != null) {
                handler.renameTranscript(item.getTranscriptId(), displayName);
            }
            refresh();
        }

        ChatListItem itemAt(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= items.size()) {
                return null;
            }
            return items.get(rowIndex);
        }

        int rowIndexForSession(LiveChatSessionId sessionId) {
            if (sessionId == null) {
                return -1;
            }
            for (int index = 0; index < items.size(); index++) {
                ChatListItem item = items.get(index);
                if (sessionId.equals(item.getLiveSessionId())) {
                    return index;
                }
            }
            return -1;
        }

        void refresh() {
            items = buildItems();
            fireTableDataChanged();
        }

        private List<ChatListItem> buildItems() {
            List<ChatListItem> results = new ArrayList<>();
            Map<String, ChatTranscriptSummary> transcriptById = new HashMap<>();
            LiveChatSessionId currentSessionId = sessionManager.getCurrentSessionId();
            for (ChatTranscriptSummary summary : transcriptStore.list()) {
                if (summary == null || summary.getId() == null || summary.getId().getFileName() == null) {
                    continue;
                }
                transcriptById.put(summary.getId().getFileName(), summary);
            }
            for (LiveChatSessionSummary session : sessionManager.listSessions()) {
                ChatTranscriptId transcriptId = session.getTranscriptId();
                if (transcriptId != null) {
                    transcriptById.remove(transcriptId.getFileName());
                }
                List<MapRootShortTextCount> mapCounts = session.getMapRootShortTextCounts();
                if (mapCounts == null || mapCounts.isEmpty()) {
                    mapCounts = mapRootShortTextFormatter.buildCounts(session.getMapIds());
                }
                boolean currentLiveSession = session.getId() != null && session.getId().equals(currentSessionId);
                results.add(new ChatListItem(ChatListItemStatus.LIVE, session.getId(), transcriptId,
                    session.getDisplayName(), mapCounts, session.getLastActivityTimestamp(), currentLiveSession));
            }
            for (ChatTranscriptSummary summary : transcriptById.values()) {
                ChatListItemStatus status = summary.getStatus() == ChatTranscriptStatus.ERROR
                    ? ChatListItemStatus.ERROR
                    : ChatListItemStatus.TRANSCRIPT;
                String displayName = summary.getDisplayName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = summary.getId() == null ? "" : summary.getId().getLeafFileName();
                }
                results.add(new ChatListItem(status, null, summary.getId(), displayName,
                    summary.getMapRootShortTextCounts(), summary.getTimestamp(), false));
            }
            results.sort(Comparator.comparingLong(ChatListItem::getLastUpdatedTimestamp).reversed());
            return results;
        }
    }

    private static class StatusIconRenderer implements TableCellRenderer {
        private static final int MARKER_SIZE = 6;
        private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();
        private final Icon liveCurrentIcon = new StatusTriangleIcon(new Color(0x2E7D32), MARKER_SIZE);
        private final Icon liveIcon = new StatusDotIcon(new Color(0x2E7D32), MARKER_SIZE);
        private final Icon transcriptIcon = new StatusDotIcon(new Color(0xF9A825), MARKER_SIZE);
        private final Icon errorIcon = new StatusDotIcon(new Color(0xC62828), MARKER_SIZE);
        private final ChatListTableModel model;

        private StatusIconRenderer(ChatListTableModel model) {
            this.model = model;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) fallback.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            ChatListItem item = model.itemAt(row);
            if (item == null) {
                label.setIcon(null);
                return label;
            }
            if (item.getStatus() == ChatListItemStatus.LIVE) {
                label.setIcon(item.isCurrentLiveSession() ? liveCurrentIcon : liveIcon);
            } else if (item.getStatus() == ChatListItemStatus.ERROR) {
                label.setIcon(errorIcon);
            } else {
                label.setIcon(transcriptIcon);
            }
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }
    }

    private static class StatusDotIcon implements Icon {
        private final Color color;
        private final int diameter;

        private StatusDotIcon(Color color, int diameter) {
            this.color = color;
            this.diameter = diameter;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.setColor(color);
            graphics.fillOval(x, y, diameter, diameter);
        }

        @Override
        public int getIconWidth() {
            return diameter;
        }

        @Override
        public int getIconHeight() {
            return diameter;
        }
    }

    private static class StatusTriangleIcon implements Icon {
        private final Color color;
        private final int size;

        private StatusTriangleIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            int[] xPoints = {x, x, x + size};
            int[] yPoints = {y, y + size, y + size / 2};
            graphics.setColor(color);
            graphics.fillPolygon(xPoints, yPoints, 3);
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
