package org.freeplane.plugin.ai.tools.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentWriteRequest {
    private final String text;
    @Description("Optional content type for new node text. Only set when the user explicitly requests Markdown or LaTeX (or formatting that requires it); otherwise omit to keep the default plain text.")
    private final ContentType textContentType;
    private final String details;
    @Description("Optional content type for new node details. Only set when the user explicitly requests Markdown or LaTeX (or formatting that requires it); otherwise omit to keep the default plain text.")
    private final ContentType detailsContentType;
    private final String note;
    @Description("Optional content type for new node note. Only set when the user explicitly requests Markdown or LaTeX (or formatting that requires it); otherwise omit to keep the default plain text.")
    private final ContentType noteContentType;
    private final List<AttributeEntry> attributes;
    private final List<String> tags;
    private final List<String> icons;
    @Description("Optional hyperlink to set on the new node.")
    private final String hyperlink;

    @JsonCreator
    public NodeContentWriteRequest(@JsonProperty("text") String text,
                                   @JsonProperty("textContentType") ContentType textContentType,
                                   @JsonProperty("details") String details,
                                   @JsonProperty("detailsContentType") ContentType detailsContentType,
                                   @JsonProperty("note") String note,
                                   @JsonProperty("noteContentType") ContentType noteContentType,
                                   @JsonProperty("attributes") List<AttributeEntry> attributes,
                                   @JsonProperty("tags") List<String> tags,
                                   @JsonProperty("icons") List<String> icons,
                                   @JsonProperty("hyperlink") String hyperlink) {
        this.text = text;
        this.textContentType = textContentType;
        this.details = details;
        this.detailsContentType = detailsContentType;
        this.note = note;
        this.noteContentType = noteContentType;
        this.attributes = attributes;
        this.tags = tags;
        this.icons = icons;
        this.hyperlink = hyperlink;
    }

    public String getText() {
        return text;
    }

    public ContentType getTextContentType() {
        return textContentType;
    }

    public String getDetails() {
        return details;
    }

    public ContentType getDetailsContentType() {
        return detailsContentType;
    }

    public String getNote() {
        return note;
    }

    public ContentType getNoteContentType() {
        return noteContentType;
    }

    public List<AttributeEntry> getAttributes() {
        return attributes;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getIcons() {
        return icons;
    }

    public String getHyperlink() {
        return hyperlink;
    }
}
