package com.github.dekobon.manta.fs.provider;

import com.github.dekobon.manta.fs.driver.MantaFileSystem;
import org.apache.commons.io.FilenameUtils;

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

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemProvider extends FileSystemProvider {
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR = new String(new char[] { SEPARATOR_CHAR });
    public static final String HOME_DIR_ALIAS = "~~";

    @Override
    public String getScheme() {
        return MantaFileSystem.SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {

        return null;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return null;
    }

    @Override
    public Path getPath(URI uri) {
        return null;
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {
        final String target = findRealPath(path);
        mantaClient.deleteRecursive(target);
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
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

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
}
