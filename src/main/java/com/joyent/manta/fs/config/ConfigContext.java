package com.joyent.manta.fs.config;

import com.joyent.manta.fs.provider.MantaFileSystemRepository;

import java.net.URI;

/**
 * Configuration context interface defining the configuration parameters needed
 * to enable the Manta NIO2 driver.
 *
 * @since 1.0.0
 * @author Elijah Zupancic
 */
public interface ConfigContext extends com.joyent.manta.config.ConfigContext {

    /**
     * Create a Manta URI (manta://...) based on the configuration values present in
     * the context.
     *
     * @param config Manta configuration context used to configure nio2 driver
     * @return URI that can be accessed using NIO2 methods
     */
    static URI mantaURIFromContext(final ConfigContext config) {
        final URI mantaURI = URI.create(config.getMantaURL());

        StringBuilder builder = new StringBuilder();
        builder.append(MantaFileSystemRepository.SCHEME)
               .append("://")
               .append(mantaURI.getHost())
               .append(":");

        final String port;

        if (mantaURI.getPort() >= 0) {
            port = String.valueOf(mantaURI.getPort());
        } else {
            port = "443";
        }

        builder.append(port);

        return URI.create(builder.toString());
    }
}
