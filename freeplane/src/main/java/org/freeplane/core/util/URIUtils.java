package org.freeplane.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URIUtils {
    private static final Pattern URI_SCHEME_PATTERN =
        Pattern.compile("^(\\p{Alpha}[\\p{Alnum}+.-]+):(.*)$");
    
    public static URI createURIFromString(String uriString) throws URISyntaxException {
        if (uriString == null || uriString.trim().isEmpty()) {
            return new URI(null, null, "", null);
        }
        
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            return createURIWithFallbackParsing(uriString, e);
        }
    }

    private static URI createURIWithFallbackParsing(String uriString, URISyntaxException originalException)
        throws URISyntaxException {
        String[] uriTextParts = splitAtFirstFragmentSeparator(uriString);
        String mainPart = uriTextParts[0];
        String fragment = uriTextParts[1];

        if (mainPart.isEmpty() && uriString.startsWith("#")) {
            return new URI(null, null, "", null, fragment);
        }

        if (uriString.startsWith("file:")) {
            return createFileURIFromText(mainPart, fragment);
        }

        if (hasNoScheme(mainPart)) {
            return createRelativeURIFromText(mainPart, fragment);
        }

        Matcher matcher = URI_SCHEME_PATTERN.matcher(mainPart);
        if (matcher.matches()) {
            return new URI(matcher.group(1), matcher.group(2), fragment);
        }

        throw originalException;
    }

    private static URI createFileURIFromText(String mainPart, String fragment) throws URISyntaxException {
        String path = mainPart.substring(5);
        String query = null;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            query = path.substring(queryIndex + 1);
            path = path.substring(0, queryIndex);
        }
        return new URI("file", null, path, query, fragment);
    }

    private static URI createRelativeURIFromText(String mainPart, String fragment) throws URISyntaxException {
        String path = mainPart;
        String query = null;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            query = path.substring(queryIndex + 1);
            path = path.substring(0, queryIndex);
        }
        return new URI(null, null, path, query, fragment);
    }

    private static boolean hasNoScheme(String uriString) {
        return !hasScheme(uriString);
    }

    private static boolean hasScheme(String uriString) {
        if (uriString.contains("://")) {
            return true;
        }
        if (isWindowsDrivePath(uriString)) {
            return false;
        }
        return URI_SCHEME_PATTERN.matcher(uriString).matches();
    }

    private static boolean isWindowsDrivePath(String uriString) {
        return uriString.length() >= 3
            && Character.isLetter(uriString.charAt(0))
            && uriString.charAt(1) == ':'
            && (uriString.charAt(2) == '/' || uriString.charAt(2) == '\\');
    }

    private static String[] splitAtFirstFragmentSeparator(String uriString) {
        int fragmentIndex = uriString.indexOf('#');
        if (fragmentIndex < 0) {
            return new String[]{uriString, null};
        }
        return new String[]{
            uriString.substring(0, fragmentIndex),
            uriString.substring(fragmentIndex + 1)
        };
    }
}
