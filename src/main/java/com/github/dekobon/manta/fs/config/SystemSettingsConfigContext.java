package com.github.dekobon.manta.fs.config;

import com.joyent.manta.config.*;

/**
 * Implementation of {@link com.joyent.manta.config.ConfigContext} that inherits from defaults,
 * environment variables and from Java system properties.
 *
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.0
 */
public class SystemSettingsConfigContext extends com.joyent.manta.config.SystemSettingsConfigContext
        implements ConfigContext {

    public SystemSettingsConfigContext(ConfigContext context) {
        super(context);
    }

    public SystemSettingsConfigContext() {
    }
}
