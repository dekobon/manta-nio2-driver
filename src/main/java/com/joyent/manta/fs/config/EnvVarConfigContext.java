package com.joyent.manta.fs.config;

/**
 * An implementation of {@link ConfigContext} that reads its configuration
 * from expected environment variables.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class EnvVarConfigContext extends com.joyent.manta.config.EnvVarConfigContext
        implements ConfigContext {
}
