package com.github.dekobon.manta.fs.config;

/**
 * Implementation of {@link ConfigContext} that links together multiple contexts.
 * This allows you to create tiers of configuration in which certain configuration
 * contexts are given priority over others.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class ChainedConfigContext extends com.joyent.manta.config.ChainedConfigContext
        implements ConfigContext {
}
