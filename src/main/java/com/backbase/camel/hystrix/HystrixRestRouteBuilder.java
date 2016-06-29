package com.backbase.camel.hystrix;

import static com.google.common.base.Objects.firstNonNull;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.spi.Language;

public abstract class HystrixRestRouteBuilder extends RouteBuilder {
    private final String basePath;

    public HystrixRestRouteBuilder(String basePath) {
        this.basePath = basePath;
    }

    public Processor put(final String template) {
        return rest(uri(template), "PUT");
    }

    public Processor delete(final String template) {
        return rest(uri(template), "DELETE");
    }

    public Processor post(final String template) {
        return rest(uri(template), "POST");
    }

    public Processor get(final String template) {
        return rest(uri(template), "GET");
    }

    public static Processor rest(final String uri, final String verb) {
        return new Processor() {
            private ProducerTemplate template;

            @Override
            public void process(Exchange exchange) throws Exception {
                if (template == null) {
                    template = exchange.getContext().createProducerTemplate();
                }

                String resolvedURI = resolveURI(exchange, uri);
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, firstNonNull(verb, "GET"));
                Exchange response = template.send(resolvedURI, exchange);

                if (exchange.isFailed()) {
                    processFailedExchange(exchange);
                } else {
                    exchange.setOut(response.getOut());
                }
            }
        };
    }

    public String uri(String template) {
        return basePath + template;
    }

    public Processor sync(Processor actualProcessor) {
        return SyncHystrixCommandProcessor.sync(this.getClass().getSimpleName(), actualProcessor, null);
    }

    public Processor sync(Processor actualProcessor, Processor fallbackProcessor) {
        return SyncHystrixCommandProcessor.sync(this.getClass().getSimpleName(), actualProcessor, fallbackProcessor);
    }

    private static String resolveURI(Exchange exchange, String template) {
        Language language = exchange.getContext().resolveLanguage("simple");
        Expression expression = language.createExpression(template);
        return expression.evaluate(exchange, String.class);
    }

    private static void processFailedExchange(Exchange exchange) throws Exception {
        Exception exception = exchange.getException();

        if (exception instanceof HttpOperationFailedException) {
            HttpOperationFailedException httpException = (HttpOperationFailedException) exception;
            int statusCode = httpException.getStatusCode();

            if ((statusCode >= 500) && (statusCode <= 599)) {
                throw exception;
            } else {
                processClientSideError(exchange, httpException, statusCode);
            }
        } else {
            throw exception;
        }
    }

    private static void processClientSideError(Exchange exchange, HttpOperationFailedException httpException, int statusCode) {
        exchange.setException(null);
        exchange.getOut().setFault(false);

        exchange.getOut().setHeader(Exchange.HTTP_URI, httpException.getUri());
        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getOut().setBody(httpException.getResponseBody());
    }
}
