package com.backbase.camel.hystrix.example;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

/**
 * Example Route that publishes a Hystrix Stream directly from Camel route
 *
 * Hystrix Stream is required in order to use Hystrix Dashboard
 *
 */
public class HystrixMetricsStreamRoute extends RouteBuilder{
    @Override
    public void configure() throws Exception {
        from("jetty:http://localhost:9999/hystrix.stream")
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
