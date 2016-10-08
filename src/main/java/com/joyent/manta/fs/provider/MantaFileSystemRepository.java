package com.joyent.manta.fs.provider;

import com.joyent.manta.fs.config.ConfigContext;
import com.joyent.manta.fs.config.MapConfigContext;
import com.joyent.manta.fs.config.SystemSettingsConfigContext;
import com.joyent.manta.fs.driver.MantaFileSystem;
import com.joyent.manta.fs.driver.MantaFileSystemDriver;
import com.joyent.manta.fs.filestore.MantaFileStore;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.github.fge.filesystem.provider.FileSystemRepository;
import com.joyent.manta.client.MantaClient;
import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.URL;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemRepository implements FileSystemRepository {
    public static final String SCHEME = "manta";

    private final ConcurrentMap<FileSystemKey, MantaFileSystem> filesystems = new ConcurrentHashMap<>();
    private final FileSystemFactoryProvider factoryProvider;

    public MantaFileSystemRepository() {
        this.factoryProvider = new MantaFileSystemFactoryProvider();
        this.factoryProvider.validate();
    }

    @Nonnull
    public FileSystemDriver createDriver(final URI uri,
                                         final Map<String, ?> env)  throws IOException {
        final ConfigContext config = buildContext(uri, env);

        final MantaClient client = new MantaClient(config);
        final MantaFileStore fileStore = new MantaFileStore(
                client, factoryProvider.getAttributesFactory());
        return new MantaFileSystemDriver(config, fileStore, factoryProvider, client);
    }

    protected ConfigContext buildContext(final URI uri, final Map<String, ?> env) throws IOException {
        final SystemSettingsConfigContext config;

        if (env != null && !env.isEmpty()) {
            final ConfigContext mapContext = new MapConfigContext(env);
            config = new SystemSettingsConfigContext(mapContext);
        } else {
            config =  new SystemSettingsConfigContext();
        }

        // Set the Manta URI based on the passed in URI's host
        if (uri.getHost() != null) {
            try {
                URL url = URL.buildHierarchical("https", uri.getHost());

                if (uri.getPort() >= 0) {
                    url.withPort(uri.getPort());
                } else {
                    url.withPort(443);
                }

                config.setMantaURL(url.toString());
            } catch (GalimatiasParseException e) {
                throw new IOException(e);
            }
        }

        if (uri.getUserInfo() != null) {
            final String user = uri.getRawUserInfo().split(":")[0];
            config.setMantaUser(user);
        }

        return config;
    }

    @Override
    @Nonnull
    public String getScheme() {
        return SCHEME;
    }

    @Nonnull
    @Override
    public FileSystemFactoryProvider getFactoryProvider() {
        return factoryProvider;
    }

    @Override
    @Nonnull
    public FileSystem createFileSystem(final FileSystemProvider provider,
                                       final URI uri, final Map<String, ?> env)
            throws IOException {
        Objects.requireNonNull(provider, "Provider must be present");
        Objects.requireNonNull(env, "Environment must be present");
        checkURI(uri);

        if (filesystems.containsKey(uri)) {
            throw new FileSystemAlreadyExistsException();
        }

        final MantaFileSystemDriver driver = (MantaFileSystemDriver)createDriver(uri, env);
        final FileSystemKey key = new FileSystemKey(uri, driver.getConfig());

        filesystems.putIfAbsent(key, new MantaFileSystem(
                uri, this, driver, provider));

        return filesystems.get(key);
    }

    @Override
    @Nonnull
    public FileSystem getFileSystem(final URI uri) {
        checkURI(uri);

        final FileSystem fs = filesystems.get(uri);

        if (fs == null) {
            throw new FileSystemNotFoundException();
        }

        return fs;
    }

    @Override
    @Nonnull
    public Path getPath(final URI uri) {
        checkURI(uri);

        final MantaFileSystem fs = filesystems.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getUri().equals(uri) && entry.getValue().isOpen())
                .findFirst()
                .orElseThrow(() -> {
                    String msg = String.format("No file system found for URI: %s", uri);
                    return new FileSystemNotFoundException(msg);
                })
                .getValue();

        final String uriPath = uri.getPath();
        return fs.getPath(uriPath);
    }

    @Nonnull
    @Override
    public FileSystemDriver getDriver(final Path path) {
        Objects.requireNonNull(path, "Path must be present");

        FileSystem pathFileSystem = path.getFileSystem();

        MantaFileSystem matchingFileSystem = filesystems
                .values()
                .stream()
                .filter(fs -> fs == pathFileSystem)
                .findFirst().orElseThrow(FileSystemNotFoundException::new);

        if (!matchingFileSystem.isOpen()) {
            throw new ClosedFileSystemException();
        }

        return matchingFileSystem.getDriver();
    }


    @Override
    public void unregister(final URI uri)
    {
        filesystems.remove(uri);
    }

    // TODO: should be checked at the provider level, not here
    private void checkURI(@Nonnull final URI uri)
    {
        Objects.requireNonNull(uri, "URI must be present");

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI must be absolute");
        }

        if (!SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Only manta:// schemes/protocols are acceptable");
        }

        if (uri.isOpaque())
            throw new IllegalArgumentException("uri is not hierarchical "
                    + "(.isOpaque() returns true)");
    }
}
