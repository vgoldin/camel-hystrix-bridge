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

                Language language = exchange.getContext().resolveLanguage("simple");
                Expression expression = language.createExpression(uri);
                String uri = expression.evaluate(exchange, String.class);

                exchange.getIn().setHeader(Exchange.HTTP_METHOD, firstNonNull(verb, "GET"));

                Exchange response = template.send(uri, exchange);

                if (exchange.isFailed()) {
                    Exception exception = exchange.getException();

                    if (exception instanceof HttpOperationFailedException) {
                        HttpOperationFailedException httpException = (HttpOperationFailedException) exception;
                        int statusCode = httpException.getStatusCode();

                        if ((statusCode >= 500) && (statusCode <= 599)) {
                            throw exception;
                        } else {
                            exchange.setException(null);
                            exchange.getOut().setFault(false);

                            exchange.getOut().setHeader(Exchange.HTTP_URI, httpException.getUri());
                            exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
                            exchange.getOut().setBody(httpException.getResponseBody());
                        }
                    } else {
                        throw exception;
                    }
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
}
