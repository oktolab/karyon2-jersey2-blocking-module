package org.glassfish.jersey.server;

import static com.netflix.config.ConfigurationManager.getConfigInstance;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesBasedResourceConfig extends ResourceConfig {

	private static final Logger logger = LoggerFactory.getLogger(PropertiesBasedResourceConfig.class);
//    private static final String JERSEY_ROOT_PACKAGE = "com.sun.jersey";
	private static final String JERSEY_ROOT_PACKAGE = "jersey.config.server.provider.packages";
    public static final String PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";
    public static final String COMMON_DELIMITERS = " ,;\n";
    
    private volatile boolean initialized;
    
    private TypeSafePropertiesDelegate typeSafePropertiesDelegate;
    
    public PropertiesBasedResourceConfig() {
    	initIfRequired();
	}
    
    public PropertiesDelegate getPropertiesDelegate() {
    	return typeSafePropertiesDelegate;
    }
    
    @Override
    Set<Class<?>> _getClasses() {
    	initIfRequired();
    	return super._getClasses();
    }
    
	private synchronized void initIfRequired() {
        if (initialized) {
            return;
        }
        initialized = true;
        String pkgNamesStr = getConfigInstance().getString(PROPERTY_PACKAGES, null);
        if (null == pkgNamesStr) {
            logger.warn("No property defined with name: " + PROPERTY_PACKAGES +
                        ", this means that jersey can not find any of your resource/provider classes.");
        } else {
            String[] pkgNames = getElements(new String[]{pkgNamesStr}, COMMON_DELIMITERS);
            logger.info("Packages to scan by jersey {}", Arrays.toString(pkgNames));
            
            registerFinder(new PackageNamesScanner(pkgNames, true)); // TODO init(
        }
        Map<String, Object> jerseyProperties = createPropertiesMap();
        setProperties(jerseyProperties); // TODO setPropertiesAndFeatures
    }

    private Map<String, Object> createPropertiesMap() {
        Properties properties = new Properties();
        Iterator<String> iter = getConfigInstance().getKeys(JERSEY_ROOT_PACKAGE);
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        while (iter.hasNext()) {
            String key = iter.next();
            properties.setProperty(key, getConfigInstance().getString(key));
            propertiesMap.put(key, getConfigInstance().getString(key));
        }
        typeSafePropertiesDelegate = new TypeSafePropertiesDelegate(propertiesMap);
        return new MapSafePropertiesDelegate(properties);
    }
    
    private static class TypeSafePropertiesDelegate implements PropertiesDelegate {

    	private Map<String, Object> map;
    	
    	public TypeSafePropertiesDelegate(Map<String, Object> map) {
    		this.map = map;
    	}

		@Override
		public Object getProperty(String name) {
			return map.get(name);
		}

		@Override
		public Collection<String> getPropertyNames() {
			return map.keySet();
		}

		@Override
		public void setProperty(String name, Object object) {
				map.put(name, object);
		}

		@Override
		public void removeProperty(String name) {
			this.map.remove(name);
		}
    }
	private static class MapSafePropertiesDelegate implements Map<String, Object> {

        private final Properties properties;
        // This intends to not make a copy of the properties but just refer to the property name & delegate to the
        // properties instance for values.
        private final Set<Entry<String, Object>> entrySet;

        public MapSafePropertiesDelegate(Properties properties) {
            this.properties = properties;
            entrySet = new HashSet<Entry<String, Object>>(properties.size());
            for (final String propName : properties.stringPropertyNames()) {
                entrySet.add(new Entry<String, Object>() {
                    @Override
                    public String getKey() {
                        return propName;
                    }

                    @Override
                    public Object getValue() {
                        return MapSafePropertiesDelegate.this.properties.getProperty(propName);
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException("Writes are not supported on jersey features and properties map.");
                    }
                });
            }
        }

        @Override
        public int size() {
            return properties.size();
        }

        @Override
        public boolean isEmpty() {
            return properties.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return properties.contains(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return properties.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return properties.getProperty(String.valueOf(key));
        }

        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException("Writes are not supported on jersey features and properties map.");
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("Writes are not supported on jersey features and properties map.");
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new UnsupportedOperationException("Writes are not supported on jersey features and properties map.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Writes are not supported on jersey features and properties map.");
        }

        @Override
        public Set<String> keySet() {
            return properties.stringPropertyNames();
        }

        @Override
        public Collection<Object> values() {
            return properties.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return entrySet;
        }
    }
	
	/**
     * Get a canonical array of String elements from a String array
     * where each entry may contain zero or more elements separated by ';'.
     *
     * @param elements an array where each String entry may contain zero or more
     *        ';' separated elements.
     * @return the array of elements, each element is trimmed, the array will
     *         not contain any empty or null entries.
     */
    public static String[] getElements(String[] elements) {
        // keeping backwards compatibility
        return getElements(elements, ";");
    }

    /**
     * Get a canonical array of String elements from a String array
     * where each entry may contain zero or more elements separated by characters
     * in delimiters string.
     *
     * @param elements an array where each String entry may contain zero or more
     *        delimiters separated elements.
     * @param delimiters string with delimiters, every character represents one
     *        delimiter.
     * @return the array of elements, each element is trimmed, the array will
     *         not contain any empty or null entries.
     */
    public static String[] getElements(String[] elements, String delimiters) {
        List<String> es = new LinkedList<String>();
        for (String element : elements) {
            if (element == null) continue;
            element = element.trim();
            if (element.length() == 0) continue;
            for (String subElement : getElements(element, delimiters)) {
                if (subElement == null || subElement.length() == 0) continue;
                es.add(subElement);
            }
        }
        return es.toArray(new String[es.size()]);
    }
    
    /**
     * Get a canonical array of String elements from a String
     * that may contain zero or more elements separated by characters in
     * delimiters string.
     *
     * @param elements a String that may contain zero or more
     *        delimiters separated elements.
     * @param delimiters string with delimiters, every character represents one
     *        delimiter.
     * @return the array of elements, each element is trimmed.
     */
    private static String[] getElements(String elements, String delimiters) {
        String regex = "[";
        for(char c : delimiters.toCharArray())
            regex += Pattern.quote(String.valueOf(c));
        regex += "]";

        String[] es = elements.split(regex);
        for (int i = 0; i < es.length; i++) {
            es[i] = es[i].trim();
        }
        return es;
    }
}
