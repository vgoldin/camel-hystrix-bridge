package com.backbase.camel.hystrix.test;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.backbase.camel.hystrix.HystrixRestRouteBuilder;
import com.netflix.hystrix.exception.HystrixRuntimeException;

public class GoogleGeoCodesRoutes extends HystrixRestRouteBuilder {
    public GoogleGeoCodesRoutes() {
        super("http://bb-system-0205.local:9999/maps");
    }

    public void configure() {
        from("quartz://geoCodes?trigger.repeatCount=10000&trigger.repeatInterval=300&fireNow=true")
            .routeId("GetGoogleGeoLocations")
            .setHeader("long",constant("40.714224"))
            .setHeader("lat",constant("-73.961452"))
            .setHeader(Exchange.HTTP_QUERY, simple("latlng=${headers.long},${headers.lat}"))
            .doTry()
                .process(sync(get("/api/geocode/json"), new FallbackProcessor()))
            .doCatch(HystrixRuntimeException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(constant("{\"error\" : \"Service is not available.\"}"))
            .end()
            .convertBodyTo(String.class)
            .log("${body}");
    }

    private class FallbackProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setBody("{\"error\" : \"Service is not available. Fallback stub.\"}");
        }
    }
}
