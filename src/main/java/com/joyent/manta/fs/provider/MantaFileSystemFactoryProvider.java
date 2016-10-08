package com.joyent.manta.fs.provider;

import com.joyent.manta.fs.attributes.MantaFileAttributesFactory;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileSystemFactoryProvider extends FileSystemFactoryProvider {
    public MantaFileSystemFactoryProvider() {
        setAttributesFactory(new MantaFileAttributesFactory());
    }
}
