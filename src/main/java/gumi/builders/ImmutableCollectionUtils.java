package gumi.builders;

import static java.util.Collections.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Since I don't want the dependency to Guava to grow the JAR from kilobytes
 * to megabytes, I implement a few multimap helpers. If I find a way to reduce
 * the size of packaging Guava instead, I'll remove this.
 */
class ImmutableCollectionUtils {

    private ImmutableCollectionUtils() {
    }
    
    private static List<String> newList(final List<String> in, final String add) {
        final List<String> list = new CopyOnWriteArrayList<>(in);
        list.add(add);
        return unmodifiableList(list);
    }
    
    private static List<String> newList(final String ... args) {
        return unmodifiableList(new CopyOnWriteArrayList<>(args));
    }
    
    private static List<String> copy(final Collection<String> in) {
        return unmodifiableList(new CopyOnWriteArrayList(in));
    }
    
    static Map<String, List<String>> copy(final Map<String, List<String>> in) {
        final Map<String, List<String>> ret = new HashMap<>();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            ret.put(e.getKey(), unmodifiableList(new ArrayList(e.getValue())));
        }
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndAdd(final Map<String, List<String>> in,
            final String key, final String value) {
        final Map<String, List<String>> ret = new HashMap<>();
        boolean added = false;
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (key.equals(e.getKey())) {
                ret.put(e.getKey(), newList(e.getValue(), value));
                added = true;
            } else {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        if (!added) {
            ret.put(key, newList(value));
        }
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndSet(final Map<String, List<String>> in,
            final String key, final String value) {
        final Map<String, List<String>> ret = new HashMap<>();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (!key.equals(e.getKey())) {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        ret.put(key, newList(value));
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndRemove(final Map<String, List<String>> in,
            final String key, final String value) {
        final Map<String, List<String>> ret = new HashMap<>();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (key.equals(e.getKey())) {
                ret.put(e.getKey(), newList(e.getValue(), value));
            } else {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndRemove(final Map<String, List<String>> in,
            final String key) {
        final Map<String, List<String>> ret = new HashMap<>();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (!key.equals(e.getKey())) {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        return unmodifiableMap(ret);
    }
}
