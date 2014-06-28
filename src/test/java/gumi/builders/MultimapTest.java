package gumi.builders;

import gumi.builders.url.UrlParameterMultimap;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.List;


/**
 * Ideally, these tests should provide 100% coverage for UrlParameterMultimap.
 */
public class MultimapTest {

    private static Map.Entry<String, String> newEntry(final String key, final String value) {
        return new AbstractMap.SimpleImmutableEntry<String, String>(key, value);
    }

    @Test
    public void createMultimap() {
        final UrlParameterMultimap m1 = UrlParameterMultimap.newMultimap();
    }

    @Test
    public void createImmutable() {
        final UrlParameterMultimap.Immutable m1 = UrlParameterMultimap.newMultimap().immutable();
    }

    @Test
    public void putArray() {
        final UrlParameterMultimap m1 = UrlParameterMultimap.newMultimap();
        m1.put("a", Arrays.asList("1", "2", "3"));
        m1.put("b", Arrays.asList("4", "5", "6"));
        m1.put("a", Arrays.asList("2", "3", "7"));
        assertEquals(Arrays.asList(newEntry("a", "2"), newEntry("a", "3"), newEntry("b", "4"),
                newEntry("b", "5"), newEntry("b", "6"), newEntry("a", "7")),
                m1.flatEntryList());
    }

    @Test
    public void addEntries() {
        final UrlParameterMultimap m1 = UrlParameterMultimap.newMultimap();
        m1.add("a", "1").add("b", "2").add("c", "3");
        assertEquals(m1.flatEntryList().get(0), newEntry("a", "1"));
        assertEquals(m1.flatEntryList().get(1), newEntry("b", "2"));
        assertEquals(m1.flatEntryList().get(2), newEntry("c", "3"));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void mutateImmutableParameterMap() {
        final UrlParameterMultimap m1 = UrlParameterMultimap.newMultimap();
        m1.add("a", "1").add("b", "2").add("c", "3");
        final UrlParameterMultimap.Immutable i1 = m1.immutable();
        i1.add("d", "4");
    }
    
    @Test
    public void containsKey() {
        UrlParameterMultimap parameterMap = newMapWithContent();
        assertTrue(parameterMap.containsKey("key1"));
        assertFalse(parameterMap.containsKey("key2"));
    }

    @Test
    public void containsValue() {
        UrlParameterMultimap parameterMap = newMapWithContent();
        assertTrue(parameterMap.containsValue("value1"));
        assertFalse(parameterMap.containsValue("value2"));
    }

    @Test
    public void replaceValues() {
        UrlParameterMultimap parameterMap = newMapWithContent();
        parameterMap.replaceValues("key1", "value2");
        assertFalse(parameterMap.containsValue("value1"));
        assertTrue(parameterMap.containsValue("value2"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullContainsKey() {
        newMapWithContent().containsKey(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullContainsValue() {
        newMapWithContent().containsValue(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullRemove() {
        newMapWithContent().remove(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullRemove2() {
        newMapWithContent().remove("", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullRemove3() {
        newMapWithContent().remove(null, "");
    }

    @Test
    public void removeKey() {
        final UrlParameterMultimap parameterMap = newMapWithContent();
        final List<String> removedMap = parameterMap.remove("key1");
        assertFalse(parameterMap.containsKey("key1"));
        assertTrue(removedMap.contains("value1"));
    }

    @Test
    public void removeKeyAndValue() {
        final UrlParameterMultimap parameterMap = newMapWithContent();
        parameterMap.add("key1", "value2");
        parameterMap.remove("key1", "value1");
        assertTrue(parameterMap.containsKey("key1"));
        parameterMap.remove("key1", "value2");
        assertFalse(parameterMap.containsKey("key1"));
    }

    @Test
    public void checkSizeIsEmptyAndClear() {
        final UrlParameterMultimap parameterMap = newMapWithContent();
        assertFalse(parameterMap.isEmpty());
        assertEquals(1, parameterMap.size());
        parameterMap.clear();
        assertTrue(parameterMap.isEmpty());
        assertEquals(0, parameterMap.size());
    }

    @Test
    public void removeAllValues() {
        final UrlParameterMultimap parameterMap = newMapWithContent();
        parameterMap.add("key1", "value2");
        parameterMap.removeAllValues("key1");
        assertFalse(parameterMap.containsKey("key1"));
        assertFalse(parameterMap.containsValue("value1"));
        assertFalse(parameterMap.containsValue("value2"));
    }

    private static UrlParameterMultimap newMapWithContent() {
        return UrlParameterMultimap.newMultimap().add("key1", "value1");
    }
    
}
