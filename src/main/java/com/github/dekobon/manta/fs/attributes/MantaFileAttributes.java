package com.github.dekobon.manta.fs.attributes;

import com.joyent.manta.client.MantaObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * {@link BasicFileAttributes} implementation for Manta.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileAttributes
        implements BasicFileAttributeView, BasicFileAttributes {
    protected final MantaObject mantaObject;

    public MantaFileAttributes(@Nonnull final MantaObject mantaObject)
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

    @Override
    public String name() {
        return mantaObject.getPath();
    }

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return this;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        // not supported
    }

    @Override
    public Object fileKey() {
        return mantaObject.getPath();
    }

    @Override
    public FileTime lastAccessTime() {
        return null;
    }

    @Override
    public FileTime creationTime() {
        return null;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }
}
