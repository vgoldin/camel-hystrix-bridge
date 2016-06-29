package com.backbase.camel.hystrix.stream;

import org.apache.camel.http.common.CamelServlet;
import org.apache.camel.component.servlet.DefaultHttpRegistry;

public class HttpRegistryFactory {
    private DefaultHttpRegistry defaultHttpRegistry;

    public HttpRegistryFactory(CamelServlet provider) {
        defaultHttpRegistry = new DefaultHttpRegistry();
        defaultHttpRegistry.register(provider);
    }

    public DefaultHttpRegistry getDefaultHttpRegistry() {
        return defaultHttpRegistry;
    }
}
