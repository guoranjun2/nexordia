package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TagCategoryNamePolicy {
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String normalizeSegment(String value) {
        return value == null ? "" : value.trim();
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public static List<String> copyValidatedPath(List<String> sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        ArrayList<String> copy = new ArrayList<>(sourcePath.size());
        for (String pathPart : sourcePath) {
            if (isBlank(pathPart)) {
                throw new IllegalArgumentException("path contains blank segment");
            }
            copy.add(pathPart);
        }
        return Collections.unmodifiableList(copy);
    }

    public static List<String> copyValidatedOptionalPath(List<String> inputPath) {
        if (inputPath == null) {
            return null;
        }
        ArrayList<String> copy = new ArrayList<>(inputPath.size());
        for (String pathItem : inputPath) {
            if (isBlank(pathItem)) {
                throw new IllegalArgumentException("path contains blank segment");
            }
            copy.add(pathItem);
        }
        return Collections.unmodifiableList(copy);
    }

    public static List<String> normalizePath(List<String> sourcePath) {
        ArrayList<String> normalizedPath = new ArrayList<>(sourcePath.size());
        for (String segment : sourcePath) {
            String normalizedSegment = normalizeSegment(segment);
            if (!normalizedSegment.isEmpty()) {
                normalizedPath.add(normalizedSegment);
            }
        }
        return normalizedPath;
    }

    public static String normalizeQualifiedName(String qualifiedName, String separator) {
        if (qualifiedName == null) {
            return "";
        }
        if (separator == null || separator.isEmpty()) {
            return normalizeSegment(qualifiedName);
        }
        String[] segments = qualifiedName.split(Pattern.quote(separator), -1);
        return joinPath(normalizePath(Arrays.asList(segments)), separator);
    }

    public static String joinPath(List<String> path, String separator) {
        if (path.isEmpty()) {
            return "";
        }
        return String.join(separator, path);
    }

    public static Tag normalizeTag(Tag tag) {
        if (tag == null) {
            return Tag.EMPTY_TAG;
        }
        String normalizedContent = normalizeSegment(tag.getContent());
        if (normalizedContent.isEmpty()) {
            return Tag.EMPTY_TAG;
        }
        if (normalizedContent.equals(tag.getContent())) {
            return tag;
        }
        return new Tag(normalizedContent, tag.getColor());
    }
}
