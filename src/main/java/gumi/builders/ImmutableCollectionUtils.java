package gumi.builders;

import static java.util.Collections.*;
import java.util.*;

/**
 * Since I don't want the dependency to Guava to grow the JAR from kilobytes
 * to megabytes, I reinvent a few multimap helpers. If I find a way to reduce
 * the size of packaging Guava instead, I'll remove this.
 */
class ImmutableCollectionUtils {

    private ImmutableCollectionUtils() {
    }

    private static List<String> copyAndAdd(final List<String> in, final String add) {
        final List<String> list = new ArrayList<String>(in);
        list.add(add);
        return unmodifiableList(list);
    }

    private static List<String> copyAndRemove(final List<String> in, final String remove) {
        final List<String> list = new ArrayList<String>(in);
        list.remove(remove);
        return unmodifiableList(list);
    }

    private static List<String> newList(final String arg) {
        return unmodifiableList(singletonList(arg));
    }
    
    private static List<String> copy(final Collection<String> in) {
        return unmodifiableList(new ArrayList<String>(in));
    }
    
    private static Map<String, List<String>> newMap() {
        return new HashMap<String, List<String>>();
    }

    /** Deep copy, with immutable values. */
    static Map<String, List<String>> copy(final Map<String, List<String>> in) {
        final Map<String, List<String>> ret = newMap();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            ret.put(e.getKey(), unmodifiableList(new ArrayList<String>(e.getValue())));
        }
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndAdd(final Map<String, List<String>> in,
            final String key, final String value)
    {
        final Map<String, List<String>> ret = newMap();
        boolean added = false;
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (key.equals(e.getKey())) {
                ret.put(e.getKey(), copyAndAdd(e.getValue(), value));
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
            final String key, final String value)
    {
        final Map<String, List<String>> ret = newMap();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (!key.equals(e.getKey())) {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        ret.put(key, newList(value));
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndRemove(final Map<String, List<String>> in,
            final String key, final String value)
    {
        final Map<String, List<String>> ret = newMap();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (key.equals(e.getKey())) {
                ret.put(e.getKey(), copyAndRemove(e.getValue(), value));
            } else {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        return unmodifiableMap(ret);
    }

    static Map<String, List<String>> copyAndRemove(final Map<String, List<String>> in,
            final String key)
    {
        final Map<String, List<String>> ret = newMap();
        for (final Map.Entry<String, List<String>> e : in.entrySet()) {
            if (!key.equals(e.getKey())) {
                ret.put(e.getKey(), copy(e.getValue()));
            }
        }
        return unmodifiableMap(ret);
    }
}
