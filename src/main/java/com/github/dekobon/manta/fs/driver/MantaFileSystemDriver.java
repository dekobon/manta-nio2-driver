package com.github.dekobon.manta.fs.driver;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.joyent.manta.client.MantaClient;
import com.joyent.manta.client.MantaObject;
import com.joyent.manta.exception.MantaException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
@ParametersAreNonnullByDefault
public class MantaFileSystemDriver extends UnixLikeFileSystemDriverBase {
    protected final MantaClient mantaClient;

    public MantaFileSystemDriver(final FileStore fileStore,
                                 final FileSystemFactoryProvider provider,
                                 final MantaClient mantaClient) {
        super(fileStore, provider);
        this.mantaClient = mantaClient;
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path,
                                      final Set<OpenOption> options)
            throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();
        final MantaObject mantaObject;

        try {
            mantaObject = mantaClient.get(target);
        } catch (MantaException e) {
            // TODO: Add parameters
            throw new RuntimeException(e);
        }

        if (mantaObject.isDirectory()) {
            throw new IsDirectoryException(target);
        }

        return mantaObject.getDataInputStream();
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path,
                                        final Set<OpenOption> options)
            throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();
        final MantaObject mantaObject = new MantaObject(target);

        throw new UnsupportedOperationException("Waiting on https://github.com/joyent/java-manta/issues/34");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = dir.toRealPath().toString();

        final Collection<MantaObject> objects;

        try {
            objects = mantaClient.listObjects(target);
        } catch (MantaException e) {
            // TODO: Set better exception params
            throw new IOException(e);
        }

        //noinspection AnonymousInnerClassWithTooManyMethods
        return new DirectoryStream<Path>() {
            private final AtomicBoolean alreadyOpen = new AtomicBoolean(false);

            @Override
            public Iterator<Path> iterator() {
                // required by the contract
                if (alreadyOpen.getAndSet(true)) {
                    throw new IllegalStateException();
                }

                return new MantaObjectPathIterator(dir, objects);
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs)
            throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = dir.toRealPath().toString();

        try {
            mantaClient.putDirectory(target, null);
        } catch (MantaException e) {
            // TODO: Paramaterize exception
            throw new IOException(e);
        }
    }

    @Override
    public void delete(final Path path) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();

        try {
            mantaClient.deleteRecursive(target);
        } catch (MantaException e) {
            // TODO: Parameterize exception
            throw new IOException(e);
        }
    }

    @Override
    public void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String from = source.toRealPath().toString();
        final String link = target.toRealPath().toString();

        try {
            MantaObject sourceObject = mantaClient.head(from);

            if (sourceObject.isDirectory()) {
                // TODO: Write directory copy logic
                throw new UnsupportedOperationException("Implement me");
            } else {
                mantaClient.putSnapLink(link, from, null);
            }
        } catch (MantaException e) {
            // TODO: Parameterize exception
            throw new IOException(e);
        }
    }

    @Override
    public void move(Path source, Path target, Set<CopyOption> options) throws IOException {
        copy(source, target, options);
        delete(source);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        final String target = path.toRealPath().toString();

        try {
            MantaObject object = mantaClient.head(target);

            for (final AccessMode mode : modes) {
                switch (mode) {
                    // You can't execute anything in Manta in the traditional sense
                    case EXECUTE:
                        throw new AccessDeniedException(target);
                    default:
                        // do nothing
                }
            }
        } catch (MantaException e) {
            // TODO: Parameterize exception
            throw new IOException(e);
        }
    }

    @Nonnull
    @Override
    public Object getPathMetadata(Path path) throws IOException {
        String target = path.toRealPath().toString();

        try {
            return mantaClient.head(target).getPath();
        } catch (MantaException e) {
            // TODO: Parameterize exception
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
