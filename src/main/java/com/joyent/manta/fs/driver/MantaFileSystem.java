package com.joyent.manta.fs.driver;

import com.joyent.manta.fs.util.ImmutableCollector;
import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.path.matchers.PathMatcherFactory;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.github.fge.filesystem.provider.FileSystemRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MantaFileSystem extends FileSystem {
    private final AtomicBoolean open = new AtomicBoolean(true);

    private final URI uri;
    private final FileSystemRepository repository;
    private final MantaFileSystemDriver driver;
    private final FileSystemProvider provider;
    private final PathMatcherFactory pathMatcherFactory;
    private final FileAttributesFactory attributesFactory;


    /**
     * Constructor
     *
     * @param uri the filesystem URI
     * @param repository the filesystem repository
     * @param driver the filesystem driver
     * @param provider the filesystem provider
     */
    public MantaFileSystem(final URI uri,
                           final FileSystemRepository repository,
                           final MantaFileSystemDriver driver,
                           final FileSystemProvider provider) {
        this.uri = Objects.requireNonNull(uri);
        this.repository = Objects.requireNonNull(repository);
        this.driver = Objects.requireNonNull(driver);
        this.provider = Objects.requireNonNull(provider);

        final FileSystemFactoryProvider factoryProvider
                = repository.getFactoryProvider();
        pathMatcherFactory = factoryProvider.getPathMatcherFactory();
        attributesFactory = factoryProvider.getAttributesFactory();
    }

    public URI getUri() {
        return uri;
    }

    public FileSystemDriver getDriver() {
        return driver;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if (!open.getAndSet(false))
            return;

        IOException exception = null;

        try {
            driver.close();
        } catch (IOException e) {
            exception = e;
        }

        repository.unregister(uri);

        if (exception != null)
            throw exception;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public boolean isReadOnly() {
        return driver.getFileStore().isReadOnly();
    }

    @Override
    public String getSeparator() {
        return MantaPath.SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        final Path rootPath = MantaPath.rootDirectory(
                this,
                driver.getMantaClient(),
                driver.getConfig().getMantaHomeDirectory());

        return Collections.singletonList(rootPath);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singletonList(driver.getFileStore());
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return attributesFactory
                .getDescriptors()
                .keySet()
                .stream()
                .filter(attributesFactory::supportsFileAttributeView)
                .collect(ImmutableCollector.toImmutableSet());
    }

    @Override
    public Path getPath(final String first, final String... more) {
        return new MantaPath(first, this, driver.getMantaClient(),
                driver.getConfig().getMantaHomeDirectory(), more);
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        final int index = Objects.requireNonNull(syntaxAndPattern).indexOf(':');

        final String type, arg;

        if (index == -1) {
            type = "glob";
            arg = syntaxAndPattern;
        } else {
            type = syntaxAndPattern.substring(0, index);
            arg = syntaxAndPattern.substring(index + 1);
        }

        return pathMatcherFactory.getPathMatcher(type, arg);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return driver.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return driver.newWatchService();
    }
}
