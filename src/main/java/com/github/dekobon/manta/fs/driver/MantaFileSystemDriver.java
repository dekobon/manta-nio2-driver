package com.github.dekobon.manta.fs.driver;

import com.github.dekobon.manta.fs.config.ConfigContext;
import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaClientHttpResponseException;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
@ParametersAreNonnullByDefault
public class MantaFileSystemDriver extends UnixLikeFileSystemDriverBase {
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR = new String(new char[] { SEPARATOR_CHAR });
    public static final String HOME_DIR_ALIAS = "~~";

    protected final MantaClient mantaClient;
    protected final ConfigContext config;


    public MantaFileSystemDriver(final ConfigContext config,
                                 final FileStore fileStore,
                                 final FileSystemFactoryProvider provider,
                                 final MantaClient mantaClient) {
        super(fileStore, provider);
        this.config = config;
        this.mantaClient = mantaClient;
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path,
                                      final Set<OpenOption> options)
            throws IOException {
        final String target = findRealPath(path);
        return mantaClient.getAsInputStream(target);
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
                                        final Set<OpenOption> options)
            throws IOException {
        // Implement me using temp files
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        return new MantaDirectoryStream(dir, mantaClient, this);
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
            throws IOException {
        final String target = findRealPath(dir);
        mantaClient.putDirectory(target, null);
    }

    @Override
    public void delete(final Path path) throws IOException {
        final String target = findRealPath(path);
        mantaClient.deleteRecursive(target);
    }

    @Override
    public void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
        if (isMantaPath(source) && isMantaPath(target)) {
            copyFromMantaFileToMantaFile(source, target, options);
        } else if (!isMantaPath(source) && isMantaPath(target)) {
            copyFromAnyPathToMantaFile(source, target, options);
        } else if (isMantaPath(source) && !isMantaPath(target)) {
            copyFromMantaFileToAnyPath(source, target, options);
        } else {
            CopyOption[] copyOptions = new CopyOption[options.size()];
            options.toArray(copyOptions);
            Files.copy(source, target, copyOptions);
        }
    }

    public boolean isMantaPath(final Path path) {
        final URI uri = Objects.requireNonNull(path.toUri());
        if (uri.getScheme() == null) {
            return false;
        } else {
            final String mantaScheme = getFileStore().type();
            return uri.getScheme().equals(mantaScheme);
        }
    }

    protected void copyFromMantaFileToAnyPath(Path source, Path target, Set<CopyOption> options) throws IOException {
        final String from = findRealPath(source);

        try (InputStream is = mantaClient.getAsInputStream(from)) {
            Files.copy(is, target);
        }
    }

    protected void copyFromAnyPathToMantaFile(Path source, Path target, Set<CopyOption> options) throws IOException {
        final String to = findRealPath(target);

        try (InputStream fs = Files.newInputStream(source);
             InputStream is = new BufferedInputStream(fs)) {

            mantaClient.put(to, is);
        }
    }

    protected void copyFromMantaFileToMantaFile(Path source, Path target, Set<CopyOption> options) throws IOException {
        final String from = findRealPath(source);
        final String link = findRealPath(target);

        MantaObject sourceObject = mantaClient.head(from);

        if (sourceObject.isDirectory()) {
            // TODO: Write directory copy logic
            throw new UnsupportedOperationException("Implement me");
        } else {
            mantaClient.putSnapLink(link, from, null);
        }
    }

    @Override
    public void move(Path source, Path target, Set<CopyOption> options) throws IOException {
        copy(source, target, options);
        delete(source);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        final String target = findRealPath(path);

        try {
            mantaClient.head(target);

            for (final AccessMode mode : modes) {
                switch (mode) {
                    // You can't execute anything in Manta in the traditional sense
                    case EXECUTE:
                        throw new AccessDeniedException(target, null,
                                "Remote execution of Manta files is not supported");
                    default:
                        // do nothing
                }
            }
        } catch (MantaClientHttpResponseException e) {
            switch (e.getStatusCode()) {
                case 404:
                    throw new NoSuchFileException(target);
                case 403:
                case 401:
                    throw new AccessDeniedException(target, null,
                            "You are not authorized to access this resource");
                case 405:
                    throw new AccessDeniedException(target, null,
                            "Access to this remote API is not supported");
                default:
                    throw new AccessDeniedException(target, null, String.format(
                            "Status Code: %d. Message: %s",
                            e.getStatusCode(), e.getStatusMessage()));
            }
        }
    }

    @Nonnull
    @Override
    public Object getPathMetadata(Path path) throws IOException {
        String target = findRealPath(path);
        return mantaClient.head(target);

    }

    @Override
    public SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs)
            throws IOException
    {
        final String target = findRealPath(path);

        if (options.contains(StandardOpenOption.CREATE_NEW)) {
            if (mantaClient.existsAndIsAccessible(target)) {
                String msg = String.format("File already exists: %s", target);
                throw new IOException(msg);
            }
        }

        if (options.contains(StandardOpenOption.READ) &&
            !options.contains(StandardOpenOption.WRITE)) {
            return mantaClient.getSeekableByteChannel(target);
        }

        return new MantaTempSeekableByteChannel(target, mantaClient, options);
    }

    /**
     * Converts a NIO2 path to a Manta filesystem path.
     * @param path NIO2 path object
     * @return relative filesystem path used to identify an object on Manta
     * @throws IOException thrown when NIO2 can't convert to a real path
     */
    public String findRealPath(final Path path) throws IOException {
        final Path real = path.toRealPath();
        final String pathString = real.toString();

        final String realpath;

        if (pathString.startsWith(HOME_DIR_ALIAS)) {
            final String relative = pathString.substring(HOME_DIR_ALIAS.length());
            realpath = String.format("/%s/%s", config.getMantaUser(), relative);
        } else if (pathString.startsWith(SEPARATOR + HOME_DIR_ALIAS)) {
            final String relative = pathString.substring(SEPARATOR.length()
                    + HOME_DIR_ALIAS.length());
            realpath = String.format("/%s/%s", config.getMantaUser(), relative);
        } else {
            realpath = pathString;
        }

        return FilenameUtils.normalizeNoEndSeparator(realpath);
    }

    @Override
    public void close() throws IOException {
        mantaClient.closeQuietly();
    }

    public ConfigContext getConfig() {
        return config;
    }

    public MantaClient getMantaClient() {
        return mantaClient;
    }
}
