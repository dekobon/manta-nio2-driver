package com.github.dekobon.manta.fs;

import com.joyent.manta.client.MantaClient;
import com.joyent.manta.com.google.api.client.repackaged.com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manta implementation of a NIO.2 {@link java.nio.file.spi.FileSystemProvider}.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemProvider extends FileSystemProvider {
    public static final String SCHEME = "manta";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MantaConfigContext defaultContext;
    final ConcurrentMap<URI, MantaFileSystem> fileSystemCache =
            new ConcurrentHashMap<>();

    private AtomicReference<MantaClient> clientRef = new AtomicReference<>();

    public MantaFileSystemProvider(MantaConfigContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public MantaFileSystemProvider() {
        this.defaultContext = new MantaConfigContext();
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return defaultNewFileSystem(uri, env);
    }

    private MantaFileSystem defaultNewFileSystem(URI uri, Map<String, ?> env) {
        if (uri == null) throw new IllegalArgumentException(
                "URI must not be null");

        if (!uri.getScheme().equals(SCHEME)) throw new IllegalArgumentException(
                String.format("Invalid URI: [%s] URI protocol/scheme must be %s",
                        uri, SCHEME));

        final MantaConfigContext configContext;

        if (env == null || env.isEmpty()) {
            configContext = defaultContext;
        } else {
            configContext = new MantaConfigContext(env);
        }

        /* We lazily instantiate the Manta client and share it between
         * threads and all instances of MantaFileSystem because it should be
         * thread-safe and we don't need a bunch of duplicate copies. */
         try {
             clientRef.compareAndSet(null, MantaClient.newInstance(
                     configContext.getMantaUrl(),
                     configContext.getMantaUser(),
                     configContext.getMantaKeyPath(),
                     configContext.getMantaKeyFingerprint()));

         } catch (IOException e) {
             throw new MantaRuntimeException("Error setting up Manta " +
                     "configuration context", e);
         }
        return new MantaFileSystem(uri, clientRef.get(), this);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        if (uri == null) throw new IllegalArgumentException(
                "URI must not be null");

        if (!uri.getScheme().equals(SCHEME)) throw new IllegalArgumentException(
                String.format("Invalid URI: [%s] URI protocol/scheme must be %s",
                        uri, SCHEME));

        final MantaFileSystem fileSystem = fileSystemCache.get(uri);

        if (fileSystem == null) throw new FileSystemNotFoundException(
            String.format("No filesystem open for URI: %s", uri));

        return fileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        String host = Objects.firstNonNull(uri.getHost(), "default-host");
        URI hostUri = URI.create(String.format("%s://%s", uri.getScheme(),
                host));

        fileSystemCache.putIfAbsent(hostUri, defaultNewFileSystem(uri, null));
        MantaFileSystem fileSystem = fileSystemCache.get(hostUri);

        return fileSystem.getPath(uri.getPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not implementd yet");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("Not implementd yet");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return MantaFileSystem.FILE_STORE;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException("Not implementd yet");
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implementd yet");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implementd yet");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implementd yet");
    }
}
