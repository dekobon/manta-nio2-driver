package com.joyent.manta.fs.provider;

import com.joyent.manta.fs.config.ConfigContext;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Objects;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class FileSystemKey {
    private final URI uri;
    private final ConfigContext config;

    public FileSystemKey(@Nonnull final URI uri, final ConfigContext config) {
        this.uri = uri;
        this.config = config;
    }

    public URI getUri() {
        return uri;
    }

    public ConfigContext getConfig() {
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSystemKey that = (FileSystemKey) o;
        return Objects.equals(uri, that.uri) &&
                Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, config);
    }
}
