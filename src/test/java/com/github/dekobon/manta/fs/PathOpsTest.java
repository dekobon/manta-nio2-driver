package com.github.dekobon.manta.fs;

import org.junit.Test;
import static com.github.dekobon.manta.fs.PathOps.test;

public class PathOpsTest {
    @Test
    public void pathConstruction() {
        test("/")
                .string("/");
        test("/", "")
                .string("/");
        test("/", "foo")
                .string("/foo");
        test("/", "/foo")
                .string("/foo");
        test("/", "foo/")
                .string("/foo");
        test("foo", "bar", "gus")
                .string("foo/bar/gus");
        test("")
                .string("");
        test("", "/")
                .string("/");
        test("", "foo", "", "bar", "", "/gus")
                .string("foo/bar/gus");
        test("", "/foo", "", "bar", "", "/gus")
                .string("/foo/bar/gus");
    }

    @Test
    public void allComponents() {
        test("/a/b/c")
                .root("/")
                .parent("/a/b")
                .name("c");
    }

    @Test
    public void rootComponentOnly() {
        test("/")
                .root("/")
                .parent(null)
                .name(null);
    }
}
