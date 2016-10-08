package com.github.dekobon.manta.fs.driver;

import com.github.dekobon.manta.fs.provider.MantaFileSystemRepository;
import com.joyent.manta.client.MantaClient;
import org.apache.commons.io.FilenameUtils;
import com.joyent.manta.org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Implementation of {@link java.nio.file.Path} that is backed by properties
 * relevant to a Manta object.
 *
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaPath implements Path {
    /* I'm honestly not very proud of this implementation. This whole thing
     * could be redone with a preparsed elements array. */

    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR = new String(new char[] { SEPARATOR_CHAR });
    public static final String HOME_DIR_ALIAS = "~~";
    private static final String ROOT_DIRECTORY_PATH = "/";

    private final char separatorChar;
    private final String separator;
    private final String objectPath;
    private final String homeDir;
    private final MantaFileSystem fileSystem;
    private final MantaClient mantaClient;

    public MantaPath(final String first, final MantaFileSystem fileSystem,
                     final MantaClient mantaClient,
                     final String homeDir, final String... more) {
        if (first == null) throw new IllegalArgumentException(
                "Object path must not be null");

        this.fileSystem = fileSystem;
        this.mantaClient = mantaClient;
        this.homeDir = homeDir;
        this.separatorChar = fileSystem == null ?
                '/' : fileSystem.getSeparator().charAt(0);
        this.separator = new String(new char[] { separatorChar });
        this.objectPath = buildObjectPath(first, more);
        validatePath(objectPath);
    }

    protected String buildObjectPath(String first, String... more) {
        if (more == null || more.length == 0) {
            return normalizeObjectPath(first);
        }

        // Don't pre add first if it is the separator, otherwise we will get a path like //foo
        final StringBuilder builder;

        if (first.equals(separator)) {
            builder = new StringBuilder();
        } else {
            builder = new StringBuilder(first);
        }

        for (int i = 0; i < more.length; i++) {
            final String part = more[i];

            if (part == null) continue;
            if (part.isEmpty()) continue;
            if (part.equals(this.separator)) continue;

            String normalized = FilenameUtils.normalizeNoEndSeparator(part, true);

            boolean emptyFirstValue = i == 0 && first.isEmpty();
            boolean hasLeadingSeparator = normalized.charAt(0) == separatorChar;

            if (!hasLeadingSeparator && !emptyFirstValue) {
                builder.append(this.separatorChar);
            }

            builder.append(normalized);
        }

        String builtPath = builder.toString();

        // If everything is empty, then we assume we are getting the root directory
        if (first.isEmpty() && builtPath.isEmpty()) {
            return separator;
            // When we've excluded all of the possible extra parts, we can't append double separators together,
            // so we just return the value of first.
        } else if (builtPath.isEmpty()) {
            return normalizeObjectPath(first);
        } else {
            return builtPath;
        }
    }

    /**
     * Checks a given path string to see if it contains forbidden characters.
     * @param aObjectPath path string to check for invalid characters
     * @throws InvalidPathException thrown when an invalid character is detected
     */
    protected static void validatePath(String aObjectPath) {
        final int badCharPos = aObjectPath.indexOf("\u0000");
        if (badCharPos >= 0) {
            throw new InvalidPathException(aObjectPath,
                    "Path included character \\u0000", badCharPos);
        }

    }

    @Override
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return !objectPath.isEmpty() && objectPath.charAt(0) == separatorChar;
    }

    @Override
    public Path getRoot() {
        if (!objectPath.isEmpty() && objectPath.charAt(0) == separatorChar) {
            return rootDirectory();
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        // There is no filename available for the root directory
        if (objectPath.equals(ROOT_DIRECTORY_PATH)) return null;

        String normalized = normalizeObjectPath(objectPath);
        String fileName = FilenameUtils.getBaseName(normalized);
        return new MantaPath(fileName, fileSystem, mantaClient, homeDir);
    }

    @Override
    public Path getParent() {
        // The root directory doesn't have a parent
        if (objectPath.equals(ROOT_DIRECTORY_PATH)) return null;
        // We imitate the behavior of UnixPath in these cases
        if (objectPath.equals(".") || objectPath.equals("..")) return null;
        // There is no parent when we have an empty path
        if (objectPath.isEmpty()) return null;

        String[] subPaths = subPaths();

        if (subPaths.length < 2) return null;

        String normalized = normalizeObjectPath(objectPath);
        String parent = FilenameUtils.getFullPathNoEndSeparator(normalized);

        return new MantaPath(parent, fileSystem, mantaClient, homeDir);
    }

    @Override
    public int getNameCount() {
        if (this.toString().equals(ROOT_DIRECTORY_PATH)) return 0;

        return subPaths().length;
    }

    @Override
    public Path getName(int index) {
        if (index < 0) throw new IllegalArgumentException("index must be above 0");
        String[] subPaths = subPaths();
        if (index > subPaths.length) throw new IllegalArgumentException(
                "index must not be greater than the number of elements available");

        String namePath = subPaths.length > 0 ? subPaths[index] : "";

        return new MantaPath(namePath, fileSystem, mantaClient, homeDir);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        if (beginIndex < 0) throw new IllegalArgumentException(
                "beginIndex must be above 0");

        if (endIndex < 0) throw new IllegalArgumentException(
                "endIndex must be above 0");

        String[] subPaths = subPaths();

        if (beginIndex > subPaths.length) throw new IllegalArgumentException(
                "beginIndex is greater than the number of elements available");

        if (endIndex <= beginIndex) throw new IllegalArgumentException(
                "endIndex must be less or equal to beginIndex");

        // On the edge case of an subpathing an empty path, return self
        if (objectPath.isEmpty() && beginIndex == 0 && endIndex == 1) {
            return this;
        }

        if (endIndex > subPaths.length) {
            throw new IllegalArgumentException(
                    "endIndex must not be greater than the number of elements available");
        }

        StringBuilder builder = new StringBuilder();

        for (int i = beginIndex; i < endIndex; i++) {
            String part = subPaths[i];

            builder.append(part);

            if (i < endIndex - 1) {
                builder.append(separator);
            }
        }

        return new MantaPath(builder.toString(), fileSystem, mantaClient, homeDir);
    }

    protected String[] subPaths() {
        if (objectPath == null) return null;

        return StringUtils.split(objectPath, separatorChar);
    }

    @Override
    public boolean startsWith(Path other) {
        if (other instanceof MantaPath) {
            MantaPath otherMantaPath = (MantaPath)other;
            return startsWith(otherMantaPath.objectPath);
        } else {
            return startsWith(other.toString());
        }
    }

    @Override
    public boolean startsWith(String other) {
        if (objectPath.isEmpty() && other.isEmpty()) return true;
        if (objectPath.isEmpty() && other.equals(separator)) return false;
        if (other == null || other.isEmpty()) return false;

        String resolveOther = resolveObjectPath(other);
        String resolveOtherWithSeparator = resolveOther.endsWith(separator) ?
                resolveOther : resolveOther + separator;
        String resolve = resolveObjectPath(objectPath);
        String resolveWithSeparator = resolve.endsWith(separator) ?
                resolve : resolve + separator;

        return resolveWithSeparator.startsWith(resolveOtherWithSeparator);
    }

    @Override
    public boolean endsWith(Path other) {
        return endsWith(other.toString());
    }

    @Override
    public boolean endsWith(String other) {
        if (objectPath.isEmpty() && other.isEmpty()) return true;
        if (objectPath.isEmpty() && other.equals(separator)) return false;
        if (other == null || other.isEmpty()) return false;

        /* Just do a simple equality if we have a fully specified path on other
         * Example:
         *   objectPath: /foo/bar -> other: /bar => false
         *   objectPath: /foo/bar -> other: /foo/bar => true
         */
        if (other.startsWith(separator)) {
            return objectPath.equals(other);
        }

        String[] subPaths = subPaths();
        String[] otherPaths = StringUtils.split(other, separatorChar);

        int sizeDiff = subPaths.length - otherPaths.length;

        // if the other sizes are greater than the subpaths, it's false
        if (sizeDiff < 0) return false;

        // if there are no path separators, just do a normal ends with [foo, foo]
        if (subPaths.length == 0 && otherPaths.length == 0) return objectPath.endsWith(other);

        // If there were no path separators in other, but in objectPath [/foo/bar, bar]
        if (subPaths.length >= 0 && otherPaths.length == 0) {
            return subPaths[subPaths.length-1].endsWith(other);
        }

        int subPathsIndex = subPaths.length - 1;
        int otherPathsIndex = otherPaths.length - 1;

        while (subPathsIndex >= 0 && otherPathsIndex >= 0) {
            final String lastSubPath = subPaths[subPathsIndex];
            final String lastOtherPath = otherPaths[otherPathsIndex];

            if (!lastSubPath.equals(lastOtherPath)) return false;

            subPathsIndex--;
            otherPathsIndex--;
        }

        return true;
    }

    @Override
    public Path normalize() {
        String normalized = normalizeObjectPath(objectPath);
        return new MantaPath(normalized, fileSystem, mantaClient, homeDir);
    }

    protected String normalizeObjectPath(String aObjectPath) {
        final String resolve;

        if (!aObjectPath.isEmpty() && aObjectPath.charAt(0) == separatorChar) {
            resolve = resolveObjectPath(aObjectPath);
        } else {
            resolve = aObjectPath;
        }

        String normalized = FilenameUtils.normalizeNoEndSeparator(resolve, true);

        if (normalized == null) {
            return FilenameUtils.normalizeNoEndSeparator(resolveObjectPath(aObjectPath), true);
        } else {
            return normalized;
        }

    }

    @Override
    public Path resolve(Path other) {
        if (other instanceof MantaPath) {
            MantaPath otherMantaPath = (MantaPath)other;
            return resolve(otherMantaPath.objectPath);
        } else {
            return resolve(other.toString());
        }
    }

    @Override
    public Path resolve(String other) {
        final String resolved;

        if (objectPath.isEmpty() && other.isEmpty()) {
            resolved = "";
        } else if (objectPath.isEmpty() && other.charAt(0) != separatorChar) {
            resolved = resolveObjectPath(other);
        } else if (objectPath.isEmpty()) {
            resolved = resolveObjectPath(other);
        } else if (other.isEmpty()) {
            resolved = resolveObjectPath(objectPath);
        } else if (other.charAt(0) != separatorChar) {
            resolved = resolveObjectPath(objectPath + separator + other);
        } else {
            resolved = resolveObjectPath(other);
        }

        return new MantaPath(resolved, fileSystem, mantaClient, homeDir);
    }

    protected String resolveObjectPath(String objectPath) {
        // We don't include this in the switch because separator is not static final
        if (objectPath.equals(separator)) return separator;

        switch (objectPath) {
            case "":
                return "";
            case "..":
                return separator;
            case ".":
                return separator;
        }

        String[] parts = objectPath.split(separator);
        StringBuilder builder = new StringBuilder();

        boolean startingSeparator = objectPath.startsWith(separator);
        boolean trailingSeparator = objectPath.endsWith(separator);
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            final String lastPart = i > 0 ? parts[i-1] : "";

            if (part.isEmpty()) continue;
            if (part.equals(".")) continue;
            if (part.equals("..") && i == 0) continue;
            if (part.equals("..") && builder.length() == 0) continue;

            if (part.equals("..")) {
                final int start = builder.length() - lastPart.length() - 1;
                final int end = builder.length();
                builder.delete(start, end);
                continue;
            }

            if (builder.length() == 0 && startingSeparator) {
                builder.append(separator);
            } else if (builder.length() > 0){
                builder.append(separator);
            }

            builder.append(part);
        }

        if (trailingSeparator) builder.append(separator);
        if (builder.length() == 0) builder.append(separator);

        return builder.toString();
    }

    @Override
    public Path resolveSibling(Path other) {
        Objects.requireNonNull(other);

        /* PathDoc: If other is absolute, then this method returns other. */
        if (other.isAbsolute()) {
            return other;
        }

        /* PathDoc: If this path does not have a parent path but is absolute,
         * then this method returns other from the root path. */
        if (getParent() == null && isAbsolute()) {
            return new MantaPath(rootDirectory().toString(), this.fileSystem,
                    this.mantaClient, this.homeDir, other.toString());
        /* If this path does not have a parent path, then this method
         * returns other. */
        } else if (getParent() == null) {
            return other;
        }

        /* PathDoc: If other is an empty path then this method returns this path's
         * parent, or where this path doesn't have a parent, the empty path. */
        if (other.toString().isEmpty()) {
            return getParent();
        }

        return new MantaPath(getParent().toString(), this.fileSystem, this.mantaClient,
                this.homeDir, other.toString());
    }

    @Override
    public Path resolveSibling(final String other) {
        Objects.requireNonNull(other);

        /* Since we don't know what type of path other is, we assume it is a
         * MantaPath. */
        final Path otherPath = new MantaPath(other, this.fileSystem,
                this.mantaClient, this.homeDir);

        return resolveSibling(otherPath);
    }

    @Override
    public Path relativize(Path other) {
        Objects.requireNonNull(other);
        throw new UnsupportedOperationException("Relative paths are not supported");
    }

    @Override
    public URI toUri() {
        return URI.create(
                String.format("%s://%s",
                        MantaFileSystemRepository.SCHEME,
                        objectPath)
        );
    }

    @Override
    public Path toAbsolutePath() {
        final String pathString = objectPath;
        final String realpath;

        if (pathString.startsWith(HOME_DIR_ALIAS)) {
            final String relative = pathString.substring(HOME_DIR_ALIAS.length());
            realpath = String.format("/%s/%s", homeDir, relative);
        } else if (pathString.startsWith(SEPARATOR + HOME_DIR_ALIAS)) {
            final String relative = pathString.substring(SEPARATOR.length()
                    + HOME_DIR_ALIAS.length());
            realpath = String.format("/%s/%s", homeDir, relative);
        } else {
            realpath = pathString;
        }

        final String normalized = FilenameUtils.normalizeNoEndSeparator(realpath);

        return new MantaPath(normalized, fileSystem, mantaClient, homeDir);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        /* Symbolic links don't exist, so we just duplicate the behavior of
         * of toAbsolutePath(). */
        return toAbsolutePath();
    }

    @Override
    public synchronized File toFile() {
        if (mantaClient == null) {
            throw new UnsupportedOperationException("toFile is not support with a null MantaClient");
        }

        try {
            return mantaClient.getToTempFile(toRealPath().toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Error getting Manta object", e);
        }
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Iterator<Path> iterator() {
        final String[] subPaths;
        final MantaPath parent = this;
        final String rootPath;

        {
            // If we have absolute paths, iterating is simple as iterating each element
            if (parent.isAbsolute()) {
                rootPath = ROOT_DIRECTORY_PATH;
                subPaths = subPaths();
            // With a relative path, we have to supply the first element of the
            } else {
                final String[] allSubPaths = subPaths();

                if (allSubPaths.length > 0) {
                    rootPath = allSubPaths[0];
                    subPaths = Arrays.copyOfRange(allSubPaths, 1, allSubPaths.length);
                } else {
                    rootPath = "";
                    subPaths = allSubPaths;
                }
            }
        }

        return new Iterator<Path>() {
            private volatile int index = 0;

            @Override
            public boolean hasNext() {
                if (subPaths.length == 0) {
                    return false;
                }

                return index <= subPaths.length;
            }

            @Override
            public synchronized Path next() {
                if (subPaths.length < 1 || index > subPaths.length) {
                    return null;
                }

                return new MantaPath(rootPath, parent.fileSystem,
                        parent.mantaClient, parent.homeDir, Arrays.copyOf(subPaths, index++));
            }

        };
    }

    @Override
    public int compareTo(final Path other) {
        Objects.requireNonNull(other);

        /* PathDoc: This method may not be used to compare paths that are associated
         * with different file system providers. */
        if (!fileSystem.provider().equals(other.getFileSystem().provider())) {
            String template = "Path being compared to belongs to a different "
                    + "file system provider. Expecting: %s Actually: %s";
            String msg = String.format(template,
                    fileSystem.provider().getClass(),
                    other.getFileSystem().provider().getClass());
            throw new ClassCastException(msg);
        }

        final Path absoluteThis = isAbsolute() ? this : toAbsolutePath();
        final Path absoluteThat = other.isAbsolute() ? other : other.toAbsolutePath();

        return absoluteThis.toString().compareTo(absoluteThat.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MantaPath that = (MantaPath) o;
        Path thatAbsolutePath = that.isAbsolute() ? that : that.toAbsolutePath();
        Path thisAbsolutePath = isAbsolute() ? this : this.toAbsolutePath();

        return thisAbsolutePath.toString().equals(thatAbsolutePath.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectPath);
    }

    public String toString() {
        return objectPath;
    }

    protected MantaPath rootDirectory() {
        return new MantaPath(ROOT_DIRECTORY_PATH, this.fileSystem, mantaClient,
                homeDir);
    }

    static MantaPath rootDirectory(final MantaFileSystem fileSystem,
                                   final MantaClient mantaClient,
                                   final String homeDir) {
        return new MantaPath(ROOT_DIRECTORY_PATH,
                fileSystem, mantaClient, homeDir);
    }
}
