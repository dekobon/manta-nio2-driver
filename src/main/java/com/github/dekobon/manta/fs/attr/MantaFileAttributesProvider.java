package com.github.dekobon.manta.fs.attr;

import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.joyent.manta.client.MantaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * {@link BasicFileAttributes} implementation for Manta.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileAttributesProvider extends BasicFileAttributesProvider {
    protected static final SimpleDateFormat HTTP_DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    protected static final Logger LOG =
            LoggerFactory.getLogger(MantaFileAttributesProvider.class);
    protected final MantaObject mantaObject;

    public MantaFileAttributesProvider(@Nonnull MantaObject mantaObject)
            throws IOException {
        this.mantaObject = mantaObject;
    }

    @Override
    public FileTime lastModifiedTime() {
        String lastModified = mantaObject.getHttpHeaders().getLastModified();

        if (lastModified == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("No last modified time for object: {}",
                         mantaObject.getPath());
            }
            return FileTime.from(Instant.EPOCH);
        }

        final Date date;

        try {
            // TODO: Change when this is finished: https://github.com/joyent/java-manta/issues/33
            date = HTTP_DATE_FORMAT.parse(lastModified);
        } catch (ParseException e) {
            LOG.warn("Unable to parse last modified time [{}] for object: {}",
                    lastModified, mantaObject.getPath());
            return FileTime.from(Instant.EPOCH);
        }

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
