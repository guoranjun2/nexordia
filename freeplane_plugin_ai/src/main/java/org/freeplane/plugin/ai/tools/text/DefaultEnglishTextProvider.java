package org.freeplane.plugin.ai.tools.text;

import org.freeplane.core.util.TextUtils;

public class DefaultEnglishTextProvider implements EnglishTextProvider {
    @Override
    public String getEnglishText(String key) {
        return TextUtils.getOriginalRawText(key);
    }
}
