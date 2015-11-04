package com.github.dekobon.manta.fs.provider;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.dekobon.manta.fs.config.MapConfigContext;
import com.github.dekobon.manta.fs.config.SystemSettingsConfigContext;
import com.github.dekobon.manta.fs.driver.MantaFileSystemDriver;
import com.github.dekobon.manta.fs.filestore.MantaFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.joyent.manta.client.MantaClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemRepository extends FileSystemRepositoryBase {
    public MantaFileSystemRepository() {
        super("manta", new MantaFileSystemFactoryProvider());
    }

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri,
                                         final Map<String, ?> env)  throws IOException {
        final ConfigContext mapContext = new MapConfigContext(env);
        final ConfigContext config = new SystemSettingsConfigContext(mapContext);
        final MantaClient client = MantaClient.newInstance(config);
        final MantaFileStore fileStore = new MantaFileStore(
                client, factoryProvider.getAttributesFactory());
        return new MantaFileSystemDriver(fileStore, factoryProvider, client);
    }
}
