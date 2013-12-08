/*
Copyright 2013 Mikael Gueck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package gumi.builders.url;

import java.util.*;

/**
 * A String to String multimap implementation best suitable for 0-100 entries.
 */
public class UrlParameterMultimap implements Map<String, List<String>> {

    private final List<Entry<String, String>> data;

    public static final class Immutable extends UrlParameterMultimap {
        public Immutable(final List<Entry<String, String>> data) {
            super(Collections.unmodifiableList(new LinkedList<Entry<String, String>>(data)));
        }
    }

    private UrlParameterMultimap(final List<Entry<String, String>> data) {
        this.data = data;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    private static Entry<String, String> newEntry(final String key, final String value) {
        return new AbstractMap.SimpleImmutableEntry<String, String>(key, value);
    }

    public static UrlParameterMultimap newMultimap() {
        return new UrlParameterMultimap(new LinkedList<Entry<String, String>>());
    }

    /**
     * Make a mutable copy.
     */
    public UrlParameterMultimap deepCopy() {
        return new UrlParameterMultimap(new LinkedList<Entry<String, String>>(data));
    }

    /**
     * Make a immutable copy.
     */
    public Immutable immutable() {
        if (this instanceof Immutable) {
            return (Immutable) this;
        } else {
            return new Immutable(data);
        }
    }

    @Override
    public boolean containsKey(final Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key can't be null");
        }
        for (final Entry<String, String> e : data) {
            if (key.equals(e.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value can't be null");
        }
        for (final Entry<String, String> e : data) {
            if (value.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
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

    @Override
    public List<String> put(final String key, final List<String> value) {
        final List<String> overflow = new ArrayList<String>(value);
        final ListIterator<Entry<String, String>> it = data.listIterator();
        while (it.hasNext()) {
            final Entry<String, String> e = it.next();
            if (key.equals(e.getKey()) && value.contains(e.getValue())) {
                overflow.remove(e.getValue());
            } else if (key.equals(e.getKey())) {
                it.remove();
            }
        }
        for (final String v : overflow) {
            this.add(key, v);
        }
        return null;
    }

    @Override
    public List<String> remove(final Object key) {
        if (key == null) {
            throw new IllegalArgumentException("can't remove null");
        }
        final List<String> ret = new ArrayList<String>();
        final ListIterator<Entry<String, String>> it = data.listIterator();
        while (it.hasNext()) {
            final Entry<String, String> e = it.next();
            if (key.equals(e.getKey())) {
                ret.add(e.getValue());
                it.remove();
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
            throw new IllegalArgumentException("can't remove null");
        }
        final ListIterator<Entry<String, String>> it = data.listIterator();
        while (it.hasNext()) {
            final Entry<String, String> e = it.next();
            if (key.equals(e.getKey()) && value.equals(e.getValue())) {
                it.remove();
            }
        }
        return this;
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        for (final Entry<? extends String, ? extends List<String>> e : m.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        final Set<String> ret = new HashSet<String>();
        for (final Entry<String, String> e : data) {
            ret.add(e.getKey());
        }
        return ret;
    }

    public List<Entry<String, String>> flatEntryList() {
        return data;
    }

    @Override
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

    @Override
    public Collection<List<String>> values() {
        final List<List<String>> ret = new LinkedList<List<String>>();
        for (final Entry<String, List<String>> e : this.entrySet()) {
            ret.add(e.getValue());
        }
        return Collections.unmodifiableList(ret);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof UrlParameterMultimap)) {
            return false;
        }
        final UrlParameterMultimap otherMultimap = (UrlParameterMultimap) other;
        return data.equals(otherMultimap.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
