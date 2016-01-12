package com.github.dekobon.manta.fs.attributes;

import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.joyent.manta.client.MantaObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * {@link BasicFileAttributes} implementation for Manta.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileAttributesProvider extends BasicFileAttributesProvider {
    protected final MantaObject mantaObject;

    public MantaFileAttributesProvider(@Nonnull MantaObject mantaObject)
            throws IOException {
        this.mantaObject = mantaObject;
    }

    @Override
    public FileTime lastModifiedTime() {
        final Date date = mantaObject.getLastModifiedTime();
        return FileTime.from(date.toInstant());
    }

    @Override
    public boolean isRegularFile() {
        return !mantaObject.isDirectory();
    }

    @Override
    public boolean isDirectory() {
        return mantaObject.isDirectory();
    }

    @Override
    public long size() {
        final Long contentLength = mantaObject.getContentLength();
        return contentLength == null ? -1 : contentLength;
    }
}
