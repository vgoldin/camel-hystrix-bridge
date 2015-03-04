package com.backbase.camel.hystrix.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.netflix.config.ConfigurationManager;

public class DynamicConfigUpdater implements ManagedService {
    @Override
    public void updated(Dictionary config) throws ConfigurationException {
        if (config == null) {
            return;
        }

        Properties props = new Properties();
        props.putAll(valueOf(config));

        ConfigurationManager.loadProperties(props);
    }

    public static <K, V> Map<K, V> valueOf(Dictionary<K, V> dictionary) {
        if (dictionary == null) {
            return null;
        }
        Map<K, V> map = new HashMap<K, V>(dictionary.size());
        Enumeration<K> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            K key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }
}
