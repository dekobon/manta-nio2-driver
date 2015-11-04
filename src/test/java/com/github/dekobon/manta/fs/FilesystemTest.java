package com.github.dekobon.manta.fs;

import com.github.dekobon.manta.fs.provider.MantaFileSystemProvider;
import com.github.dekobon.manta.fs.provider.MantaFileSystemRepository;
import com.github.fge.filesystem.provider.FileSystemRepository;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by elijah on 11/3/15.
 */
public class FilesystemTest {
    @Test
    public void tryme() throws IOException {
        final URI uri = URI.create("manta://elijah.zupancic/");
        final Map<String, String> env = new HashMap<>();

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository
                = new MantaFileSystemRepository();
        final FileSystemProvider provider = new MantaFileSystemProvider(repository);

        try (
            /*
             * Create the filesystem...
             */
            final FileSystem mantaFs = provider.newFileSystem(uri, env);
        ) {
            /*
             * And use it! You should of course adapt this code...
             */
            // Equivalent to FileSystems.getDefault().getPath(...)
            final Path src = mantaFs.getPath("/elijah.zupancic/stor");
            fileList(src);
//            // Here we create a path for our Manta fs...
//            final Path dst = ("/Example3.java");
//            // Here we copy the file from our local fs to dropbox!
//            Files.copy(src, dst);
        }
    }

    public static List<String> fileList(Path directory) {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                fileNames.add(path.toString());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return fileNames;
    }
}
