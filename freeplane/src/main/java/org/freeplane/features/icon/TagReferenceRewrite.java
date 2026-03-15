package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagReferenceRewrite {
    private final String sourceQualifiedName;
    private final String targetQualifiedName;

    public TagReferenceRewrite(String sourceQualifiedName, String targetQualifiedName) {
        if (sourceQualifiedName == null) {
            throw new IllegalArgumentException("sourceQualifiedName must not be null");
        }
        if (targetQualifiedName == null) {
            throw new IllegalArgumentException("targetQualifiedName must not be null");
        }
        this.sourceQualifiedName = sourceQualifiedName;
        this.targetQualifiedName = targetQualifiedName;
    }

    public static List<TagReferenceRewrite> fromPairs(List<String> replacementPairs) {
        if (replacementPairs == null) {
            throw new IllegalArgumentException("replacementPairs must not be null");
        }
        if ((replacementPairs.size() % 2) != 0) {
            throw new IllegalArgumentException("replacementPairs must contain old/new pairs");
        }
        ArrayList<TagReferenceRewrite> rewrites = new ArrayList<>(replacementPairs.size() / 2);
        for (int i = 0; i < replacementPairs.size(); i += 2) {
            rewrites.add(new TagReferenceRewrite(replacementPairs.get(i), replacementPairs.get(i + 1)));
        }
        return Collections.unmodifiableList(rewrites);
    }

    public static List<String> toPairs(List<TagReferenceRewrite> referenceRewrites) {
        if (referenceRewrites == null) {
            throw new IllegalArgumentException("referenceRewrites must not be null");
        }
        ArrayList<String> replacementPairs = new ArrayList<>(referenceRewrites.size() * 2);
        for (TagReferenceRewrite rewrite : referenceRewrites) {
            if (rewrite == null) {
                throw new IllegalArgumentException("referenceRewrites must not contain null");
            }
            replacementPairs.add(rewrite.getSourceQualifiedName());
            replacementPairs.add(rewrite.getTargetQualifiedName());
        }
        return Collections.unmodifiableList(replacementPairs);
    }

    public String getSourceQualifiedName() {
        return sourceQualifiedName;
    }

    public String getTargetQualifiedName() {
        return targetQualifiedName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceQualifiedName, targetQualifiedName);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagReferenceRewrite)) {
            return false;
        }
        TagReferenceRewrite other = (TagReferenceRewrite) obj;
        return Objects.equals(sourceQualifiedName, other.sourceQualifiedName)
            && Objects.equals(targetQualifiedName, other.targetQualifiedName);
    }
}
