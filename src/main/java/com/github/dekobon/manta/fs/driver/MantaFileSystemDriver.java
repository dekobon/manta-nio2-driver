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
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Set;

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
            throws IOException
    {
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
}
