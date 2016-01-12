package com.github.dekobon.manta.fs.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

/**
 *
 * <p>Inspired by: <a href="http://www.jayway.com/2013/11/12/immutable-list-collector-in-java-8/">immutable-list-collector-in-java-8</a></p>
 */
public class Immutables {
    public static <t> Collector<t, List<t>, List<t>> toImmutableList() {
        return Collector.of(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableList);
    }

    public static <t> Collector<t, Set<t>, Set<t>> toImmutableSet() {
        return Collector.of(LinkedHashSet::new, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableSet);
    }

    public static <T> Set<T> immutableSet(T... values) {
        final List<T> list = Arrays.asList(values);
        final Set<T> set = new LinkedHashSet<>(list);
        return Collections.unmodifiableSet(set);
    }
}
