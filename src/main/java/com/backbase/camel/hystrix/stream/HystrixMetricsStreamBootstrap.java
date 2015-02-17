package com.backbase.camel.hystrix.stream;

import java.util.EventObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.management.event.CamelContextStartedEvent;
import org.apache.camel.support.EventNotifierSupport;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

public class HystrixMetricsStreamBootstrap extends EventNotifierSupport {

    private static final String ROUTE_ID = "hystrixStream";

    @Override
    public void notify(EventObject event) throws Exception {
        if (event instanceof CamelContextStartedEvent) {
            CamelContextStartedEvent e = (CamelContextStartedEvent) event;
            CamelContext context = e.getContext();

            context.addRoutes(createRouteBuilder(context.getName()));
            context.startRoute(ROUTE_ID);
        }
    }

    @Override
    public boolean isEnabled(EventObject event) {
        return true;
    }

    private RouteBuilder createRouteBuilder(final String contextId) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("servlet:///"+contextId.toLowerCase()+".stream")
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
        };
    }
}
