package com.udderlywet.osrsstrategist;

import java.util.function.Consumer;

/** Explicit-user-click external links. No request is made by Compass itself. */
public final class SupportLinks
{
    /** Set only this value when the real optional tip destination exists. */
    public static final String SUPPORT_URL = "";

    private SupportLinks() { }

    static boolean isConfigured(String url)
    {
        if (url == null) return false;
        String value = url.trim().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("https://") && value.length() > 8;
    }

    static boolean openIfConfigured(String url, Consumer<String> browser)
    {
        if (!isConfigured(url) || browser == null) return false;
        browser.accept(url.trim());
        return true;
    }
}
