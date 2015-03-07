package com.github.dekobon.manta.fs;

import java.util.Map;

/**
 * Configuration object that contains all of the parameters needed to connect
 * to a Manta instance.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaConfigContext {
    public static final String MANTA_USER_ENV_KEY = "MANTA_USER";
    public static final String MANTA_USER_SYS_PROP_KEY = "manta.user";
    public static final String MANTA_URL_ENV_KEY = "MANTA_URL";
    public static final String MANTA_URL_SYS_PROP_KEY = "manta.url";
    public static final String MANTA_KEY_PATH_SYS_PROP_KEY = "manta.key_path";
    public static final String MANTA_KEY_PATH_ENV_KEY = "MANTA_KEY_PATH";
    public static final String MANTA_KEY_FINGERPRINT_SYS_PROP_KEY = "manta.key_fingerprint";
    public static final String MANTA_KEY_FINGERPRINT_ENV_KEY = "MANTA_KEY_ID";

    private String mantaUrl = "https://us-east.manta.joyent.com";
    private String mantaUser;
    private String mantaKeyPath;
    private String mantaKeyFingerprint;

    public MantaConfigContext() {
        // Setting these to null reverts them to their ENV or sysprop values
        setMantaUrl(null);
        setMantaUser(null);
        setMantaKeyPath(null);
        setMantaKeyFingerprint(null);
    }

    public MantaConfigContext(String mantaUrl, String mantaUser, String mantaKeyPath,
                              String mantaKeyFingerprint) {

        setMantaUrl(mantaUrl);
        setMantaUser(mantaUser);
        setMantaKeyPath(mantaKeyPath);
        setMantaKeyFingerprint(mantaKeyFingerprint);
    }

    public MantaConfigContext(Map<String, ?> env) {
        String mantaUser = String.valueOf(env.get(MANTA_USER_SYS_PROP_KEY));
        String mantaUrl = String.valueOf(env.get(MANTA_URL_SYS_PROP_KEY));
        String mantaKeyPath = String.valueOf(env.get(MANTA_KEY_FINGERPRINT_SYS_PROP_KEY));
        String mantaKeyFingerprint = String.valueOf(env.get(MANTA_KEY_FINGERPRINT_SYS_PROP_KEY));

        setMantaUrl(mantaUrl);
        setMantaUser(mantaUser);
        setMantaKeyPath(mantaKeyPath);
        setMantaKeyFingerprint(mantaKeyFingerprint);
    }

    /**
     * Taken from Apache Commons Lang 3.
     *
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    protected static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Taken from Apache Commons Lang 3.
     *
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     *  not empty and not null and not whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isNotBlank(String) to isNotBlank(CharSequence)
     */
    protected static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Finds the correct setting for the property and returns it.
     * @param passedValue setting given to us by the constructor
     * @param systemProperty system property key to look for setting
     * @param envVar environment variable name to look for setting
     * @param errorMessage error message to display if setting is not found
     * @return the proper setting
     */
    private String validateAndReturnParam(String passedValue,
                                          String systemProperty,
                                          String envVar,
                                          String errorMessage) {
        if (isNotBlank(passedValue)) return passedValue;

        String systemPropertyValue = System.getProperty(systemProperty);
        if (isNotBlank(systemPropertyValue)) return systemPropertyValue;

        String envVarValue = System.getenv(envVar);
        if (isNotBlank(envVarValue)) return envVarValue;

        throw new IllegalArgumentException(errorMessage);
    }

    public String getMantaUrl() {
        return mantaUrl;
    }

    public void setMantaUrl(String mantaUrl) {
        this.mantaUrl = validateAndReturnParam(mantaUrl,
                MANTA_URL_SYS_PROP_KEY, MANTA_URL_ENV_KEY,
                "Manta URL must be specified. It is typically a value like: " +
                        "https://us-east.manta.joyent.com");
    }

    public String getMantaUser() {
        return mantaUser;
    }

    public void setMantaUser(String mantaUser) {
        this.mantaUser = validateAndReturnParam(mantaUser,
                MANTA_USER_SYS_PROP_KEY, MANTA_USER_ENV_KEY,
                "Manta User is the primary account holder's username");
    }

    public String getMantaKeyPath() {
        return mantaKeyPath;
    }

    public void setMantaKeyPath(String mantaKeyPath) {
        this.mantaKeyPath = validateAndReturnParam(mantaKeyPath,
                MANTA_KEY_PATH_SYS_PROP_KEY, MANTA_KEY_PATH_ENV_KEY,
                "Manta Key Path is the path to the SSH key user to access manta");
    }

    public String getMantaKeyFingerprint() {
        return mantaKeyFingerprint;
    }

    public void setMantaKeyFingerprint(String mantaKeyFingerprint) {
        this.mantaKeyFingerprint = validateAndReturnParam(mantaKeyFingerprint,
                MANTA_KEY_FINGERPRINT_SYS_PROP_KEY, MANTA_KEY_FINGERPRINT_ENV_KEY,
                "Manta key finger print is the fingerprint of the SSH key used " +
                        "to access manta");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MantaConfigContext{");
        sb.append("mantaUrl='").append(mantaUrl).append('\'');
        sb.append(", mantaUser='").append(mantaUser).append('\'');
        sb.append(", mantaKeyPath='").append(mantaKeyPath).append('\'');
        sb.append(", mantaKeyFingerprint='").append(mantaKeyFingerprint).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
