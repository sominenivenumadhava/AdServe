package com.adserve.util;

/**
 * Predictable cache key generator for ad-serving Redis keys.
 * Format: adserve:eligible:{COUNTRY}:{DEVICE}:{CATEGORY}
 */
public final class AdCacheKeyUtil {

    public static final String KEY_PREFIX = "adserve:eligible:";
    public static final String GLOBAL_PATTERN = "adserve:eligible:*";

    private AdCacheKeyUtil() {
        // Utility class
    }

    /**
     * Constructs a normalized cache key for eligible ad lookups.
     *
     * @param country Target country code (e.g., IN, US)
     * @param device Target device type (e.g., ANDROID, IOS, DESKTOP)
     * @param category Target content category (e.g., GAMING, SPORTS)
     * @return Canonical Redis key string (e.g. adserve:eligible:IN:ANDROID:GAMING)
     */
    public static String buildEligibleAdsKey(String country, String device, String category) {
        String normalizedCountry = normalize(country);
        String normalizedDevice = normalize(device);
        String normalizedCategory = normalize(category);

        return KEY_PREFIX + normalizedCountry + ":" + normalizedDevice + ":" + normalizedCategory;
    }

    /**
     * Normalizes a targeting dimension token by trimming and converting to uppercase.
     */
    private static String normalize(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "UNKNOWN";
        }
        return token.trim().toUpperCase();
    }
}
