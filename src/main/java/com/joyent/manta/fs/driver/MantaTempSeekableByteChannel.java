package com.joyent.manta.fs.driver;

import com.joyent.manta.client.MantaClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link SeekableByteChannel} implementation that downloads an existing file
 * off of Manta to a temp file (if it exists) and then provides a reference
 * to the {@link SeekableByteChannel} for the temp file.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaTempSeekableByteChannel implements SeekableByteChannel {
    private final String mantaPath;
    private final SeekableByteChannel inner;
    private final MantaClient client;
    private final Path temp;
    private final Set<? extends OpenOption> options;

    public MantaTempSeekableByteChannel(final String mantaPath,
                                        final MantaClient client,
                                        final Set<? extends OpenOption> options)
            throws IOException {
        this.mantaPath = mantaPath;
        this.temp = Files.createTempFile("manta_nio", ".tmp");
        this.client = client;
        this.options = options;

        if (!options.contains(StandardOpenOption.TRUNCATE_EXISTING) &&
            client.existsAndIsAccessible(mantaPath)) {

            try (InputStream is = client.getAsInputStream(mantaPath)) {
                Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // We honor delete on close explicitly on in our close() method
        Set<? extends OpenOption> tempOptions = new HashSet<>(options);
        tempOptions.remove(StandardOpenOption.DELETE_ON_CLOSE);
        tempOptions.remove(StandardOpenOption.CREATE_NEW);

        this.inner = Files.newByteChannel(temp, tempOptions);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return inner.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return inner.write(src);
    }

    @Override
    public long position() throws IOException {
        return inner.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return inner.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return inner.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return inner.truncate(size);
    }

    @Override
    public boolean isOpen() {
        return inner.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (!isOpen()) {
            return;
        }

        inner.close();

        // We put the file that has completed all of its writes and reads onto Manta
        try (InputStream is = Files.newInputStream(temp)) {
            client.put(mantaPath, is);
        }

        Files.deleteIfExists(temp);

        if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
            client.delete(mantaPath);
        }
    }
}
