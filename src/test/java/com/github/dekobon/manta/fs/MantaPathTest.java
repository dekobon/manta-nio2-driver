package com.github.dekobon.manta.fs;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

public class MantaPathTest {
    @Test
    public void path() {
        File file = new File("/var/folders/pb/n9k5tlnj4q746wnn566_4t7m0000gn/T/foo.Frh5MgPU");
        File dir = new File("/var/folders/pb/n9k5tlnj4q746wnn566_4t7m0000gn/T/");
        Path filePath = file.toPath();
        Path dirPath = dir.toPath();
        System.out.print(filePath.getClass());
    }
}
