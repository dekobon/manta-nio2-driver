package com.github.dekobon.manta.fs.config;

import com.joyent.manta.config.*;

import java.util.Map;

/**
 * {@link com.joyent.manta.config.ConfigContext} implementation that is used for configuring instances
 * from a Map.
 *
 * @author <a href="https://github.com/dekobon">Elijah Zupancic</a>
 * @since 1.0.0
 */
public class MapConfigContext extends com.joyent.manta.config.MapConfigContext
        implements ConfigContext {
    public MapConfigContext(Map<?, ?> backingMap) {
        super(backingMap);
    }
}
