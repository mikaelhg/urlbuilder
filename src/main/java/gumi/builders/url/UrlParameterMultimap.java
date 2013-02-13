package gumi.builders.url;

import java.util.*;

/**
 * A String to String multimap implementation best suitable for 0-100 entries.
 */
public class UrlParameterMultimap implements Map<String, List<String>> {

    private List<Entry<String, String>> data;

    public UrlParameterMultimap() {
        this.data = new LinkedList<Entry<String, String>>();
    }

    private UrlParameterMultimap(final List<Entry<String, String>> data) {
        this.data = data;
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    private static final Entry<String, String> newEntry(final String key, final String value) {
        return new AbstractMap.SimpleImmutableEntry<String, String>(key, value);
    }

    public UrlParameterMultimap deepCopy() {
        return new UrlParameterMultimap(new LinkedList<Entry<String, String>>(data));
    }

    public UrlParameterMultimap immutable() {
        return new UrlParameterMultimap(Collections.unmodifiableList(data));
    }

    public boolean containsKey(final Object key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(final Object value) {
        if (value == null) {
            throw new NullPointerException("value can't be null");
        }
        for (final Entry<String, String> e : data) {
            if (value.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    public List<String> get(final Object key) {
        final List<String> ret = new ArrayList<String>();
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey())) {
                ret.add(e.getValue());
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    public UrlParameterMultimap add(final String key, final String value) {
        data.add(newEntry(key, value));
        return this;
    }

    public UrlParameterMultimap replaceValues(final String key, final String value) {
        this.remove(key);
        this.add(key, value);
        return this;
    }

    public List<String> put(final String key, final List<String> value) {
        final List<String> overflow = new ArrayList<String>(value);
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey()) && value.contains(e.getValue())) {
                overflow.remove(key);
            } else if (key.equals(e.getKey())) {
                data.remove(e);
            }
        }
        for (final String v : overflow) {
            this.add(key, v);
        }
        return null;
    }

    public List<String> remove(final Object key) {
        if (key == null) {
            throw new NullPointerException("can't remove null");
        }
        final List<String> ret = new ArrayList<String>();
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey())) {
                ret.add(e.getValue());
                data.remove(e);
            }
        }
        return ret;
    }

    public UrlParameterMultimap removeAllValues(final String key) {
        this.remove(key);
        return this;
    }

    public UrlParameterMultimap remove(final String key, final String value) {
        if (key == null || value == null) {
            throw new NullPointerException("can't remove null");
        }
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey()) && value.equals(e.getValue())) {
                data.remove(e);
            }
        }
        return this;
    }

    public void putAll(Map<? extends String, ? extends List<String>> m) {
        for (final Entry<? extends String, ? extends List<String>> e : m.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        data.clear();
    }

    public Set<String> keySet() {
        final Set<String> ret = new HashSet<String>();
        for (final Entry<String, String> e : data) {
            ret.add(e.getKey());
        }
        return ret;
    }

    public List<Entry<String, String>> flatEntrySet() {
        return data;
    }

    public Set<Entry<String, List<String>>> entrySet() {
        final LinkedHashMap<String, List<String>> entries = new LinkedHashMap<String, List<String>>();
        for (final Entry<String, String> e : data) {
            if (!entries.containsKey(e.getKey())) {
                entries.put(e.getKey(), new LinkedList<String>());
            }
            entries.get(e.getKey()).add(e.getValue());
        }
        for (final Entry<String, List<String>> e : entries.entrySet()) {
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableSet(entries.entrySet());
    }

    public Collection<List<String>> values() {
        final List<List<String>> ret = new LinkedList<List<String>>();
        for (final Entry<String, List<String>> e : this.entrySet()) {
            ret.add(e.getValue());
        }
        return Collections.unmodifiableList(ret);
    }
}
