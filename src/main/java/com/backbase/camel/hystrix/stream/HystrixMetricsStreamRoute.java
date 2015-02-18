package com.backbase.camel.hystrix.stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

public class HystrixMetricsStreamRoute extends RouteBuilder {

    private static final String ROUTE_ID = "HystrixStream";

    @Override
    public void configure() throws Exception {
        from("servlet:///hystrix.stream")
            .routeId(ROUTE_ID)
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
                    HttpServletResponse res = exchange.getIn().getBody(HttpServletResponse.class);

                    HystrixMetricsStreamServlet servlet = new HystrixMetricsStreamServlet();
                    servlet.service(req, res);
                }
            });
    }
}
