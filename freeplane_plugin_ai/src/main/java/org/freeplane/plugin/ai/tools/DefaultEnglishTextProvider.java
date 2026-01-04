package org.freeplane.plugin.ai.tools;

import org.freeplane.core.util.TextUtils;

public final class DefaultEnglishTextProvider implements EnglishTextProvider {
    @Override
    public String getEnglishText(String key) {
        return TextUtils.getOriginalRawText(key);
    }
}
