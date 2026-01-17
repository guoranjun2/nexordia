package org.freeplane.plugin.ai.chat;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;

import io.github.gitbucket.markedj.Marked;
import io.github.gitbucket.markedj.Options;

class ChatMessageRenderer {
    private final Options markdownOptions;

    ChatMessageRenderer() {
        markdownOptions = createMarkdownOptions();
    }

    String renderMessage(String text, boolean renderMarkdown) {
        if (renderMarkdown) {
            return renderMarkdownMessage(text);
        }
        return formatPlainText(text);
    }

    private Options createMarkdownOptions() {
        Options options = new Options();
        options.setSafelist(null);
        return options;
    }

    private String renderMarkdownMessage(String text) {
        try {
            String renderedMarkup = Marked.marked(text, markdownOptions);
            if (renderedMarkup == null) {
                return formatPlainText(text);
            }
            return renderedMarkup;
        } catch (RuntimeException exception) {
            LogUtils.severe(exception);
            return formatPlainText(text);
        }
    }

    private String formatPlainText(String text) {
        String escaped = HtmlUtils.toXMLEscapedText(text);
        String normalized = escaped.replace("\r\n", "\n").replace("\r", "\n");
        return normalized.replace("\n", "<br>");
    }
}
