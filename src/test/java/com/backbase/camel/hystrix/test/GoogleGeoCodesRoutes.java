package com.backbase.camel.hystrix.test;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.backbase.camel.hystrix.SyncHystrixCommandProcessor;
import com.netflix.hystrix.exception.HystrixRuntimeException;

public class GoogleGeoCodesRoutes extends RouteBuilder {
    Endpoint google;

    public void configure() {
        google = endpoint("http://bb-system-0205.local:9999/maps/api/geocode/json");

        from("quartz://geoCodes?trigger.repeatCount=10000&trigger.repeatInterval=300&fireNow=true")
            .routeId("GetGoogleGeoLocations")
            .setHeader("long",constant("40.714224"))
            .setHeader("lat",constant("-73.961452"))
            .setHeader(Exchange.HTTP_QUERY, simple("latlng=${headers.long},${headers.lat}"))
            .doTry()
                .process(new SyncHystrixCommandProcessor(this.getClass().getSimpleName(),
                        new GetGoogleGeoCodes(), new FallbackProcessor()))
            .doCatch(HystrixRuntimeException.class)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(constant("{\"error\" : \"Service is not available.\"}"))
            .end()
            .convertBodyTo(String.class)
            .log("${body}");
    }

    public class GetGoogleGeoCodes implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            Object response = exchange.getContext().createProducerTemplate().requestBodyAndHeaders(google,
                    null,
                    exchange.getIn().getHeaders());

            exchange.getIn().setBody(response);
        }
    }

    private class FallbackProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            exchange.getIn().setBody("{\"error\" : \"Service is not available\"}");
        }
    }
}
