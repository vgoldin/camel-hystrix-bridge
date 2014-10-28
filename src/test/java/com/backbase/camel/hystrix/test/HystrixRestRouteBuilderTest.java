package com.backbase.camel.hystrix.test;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class HystrixRestRouteBuilderTest extends CamelTestSupport {
    @Test
    public void testSuccessfulRequest() throws Exception {
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("https://maps.googleapis.com/maps/api/geocode/json")
                        .skipSendToOriginalEndpoint()
                        .to("mock:mockedEndpoint");
            }
        });

        template.sendBody("direct:GetGoogleGeoLocations", null);

        getMockEndpoint("mock:mockedEndpoint").expectedMessageCount(1);
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFailedRequest() throws Exception {
        context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("https://maps.googleapis.com/maps/api/geocode/json")
                        .skipSendToOriginalEndpoint()
                        .throwException(new RuntimeException("Failed"));
            }
        });

        try {
            template.sendBody("direct:GetGoogleGeoLocations", null);

            fail("Exchange did not failed with Exception, as expected.");
        } catch (CamelExecutionException ex) {
            String fallbackBody = ex.getExchange().getIn().getBody().toString();

            assertThat("Fallback response", fallbackBody,
                    equalTo("{\"error\" : \"Service is not available. Fallback stub.\"}"));
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new GoogleGeoCodesRoutes();
    }
}
