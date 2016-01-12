package com.github.dekobon.manta.fs.attributes;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.joyent.manta.client.MantaObject;

/**
 * @author Elijah Zupancic
 * @since 1.0.0
 */
public class MantaFileAttributesFactory extends FileAttributesFactory {
    public MantaFileAttributesFactory() {
        setMetadataClass(MantaObject.class);
        addImplementation("basic", MantaFileAttributesProvider.class);
    }
}
