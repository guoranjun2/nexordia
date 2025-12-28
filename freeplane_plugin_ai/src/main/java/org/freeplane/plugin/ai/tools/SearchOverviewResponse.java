package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchOverviewResponse {
    private final String mapIdentifier;
    private final String summary;
    private final List<String> themes;
    private final List<SearchOverviewSection> sections;
    private final List<SearchOverviewKeyword> keywords;

    public SearchOverviewResponse(String mapIdentifier, String summary, List<String> themes,
                                  List<SearchOverviewSection> sections, List<SearchOverviewKeyword> keywords) {
        this.mapIdentifier = mapIdentifier;
        this.summary = summary;
        this.themes = themes;
        this.sections = sections;
        this.keywords = keywords;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getThemes() {
        return themes;
    }

    public List<SearchOverviewSection> getSections() {
        return sections;
    }

    public List<SearchOverviewKeyword> getKeywords() {
        return keywords;
    }
}
