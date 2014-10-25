package com.backbase.camel.hystrix;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

public class SyncHystrixCommandProcessor implements Processor {
    private Processor actualProcessor;
    private Processor fallbackProcessor;

    /**
     * Construct Synchronous (InOut MEP) Camel processor for Hystrix Command
     * without Fallback processor
     *
     * @param actualProcessor the actual Camel processor that will be used to process the exchange
     */
    public SyncHystrixCommandProcessor(Processor actualProcessor) {
        this(actualProcessor, null);
    }

    /**
     * Construct Synchronous (InOut MEP) Camel processor for Hystrix Command
     * with Fallback processor, that will be used in case of open circuit
     *
     * @param actualProcessor the actual Camel processor that will be used to process the exchange
     * @param fallbackProcessor the fallback Camel processor that will be used in case of open circuit
     */
    public SyncHystrixCommandProcessor(Processor actualProcessor, Processor fallbackProcessor) {
        this.actualProcessor = actualProcessor;
        this.fallbackProcessor = fallbackProcessor;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // -- set Camel Message Exchange Pattern to Synchronous Request-Response
        exchange.setPattern(ExchangePattern.InOut);

        // -- execute synchronous Hystrix command
        Message response  = new GenericCommand(exchange).execute();

        exchange.setIn(response);
    }

    private class GenericCommand extends HystrixCommand<Message> {
        private Exchange exchange;

        /**
         * Construct a generic command that will use the simple class name of the enclosed Camel Processor as a
         * name of the execution group and routeId as a name of the command
         *
         * @param exchange the current Camel exchange
         */
        private GenericCommand(Exchange exchange) {
            super(Setter.withGroupKey(
                    HystrixCommandGroupKey.Factory.asKey(actualProcessor.getClass().getSimpleName())).andCommandKey(
                    HystrixCommandKey.Factory.asKey(exchange.getFromRouteId())));

            this.exchange = exchange;
        }

        /**
         * Execute <code>process()</code> of the enclosed Camel processor
         *
         * @return the response received from the underlying processor
         */
        @Override
        protected Message run() {
            return process(actualProcessor);
        }

        /**
         * Get the fallback response in case of open circuit
         * if fallback Camel Processor is supplied
         *
         * @return the response from the fallback Camel processor
         */
        @Override
        protected Message getFallback() {
            Message fallback;
            if (fallbackProcessor != null) {
                fallback = process(fallbackProcessor);
            } else {
                fallback = super.getFallback();
            }

            return fallback;
        }

        private Message process(Processor processor) {
            Message result;
            try {
                processor.process(exchange);
            } catch (Exception ex) {
                throw new RuntimeCamelException(ex);
            }

            result = exchange.getIn();

            return result;
        }
    }

    /**
     * DSL method to instantiate Synchronous Histrix Processor with Fallback processor
     *
     * @param actualProcessor the actual Camel processor that will be used to process the exchange
     * @param fallbackProcessor the fallback Camel processor that will be used in case of open circuit
     * @return the Synchronous Histrix Processor
     */
    public static Processor sync(Processor actualProcessor, Processor fallbackProcessor) {
        return new SyncHystrixCommandProcessor(actualProcessor, fallbackProcessor);
    }

    /**
     * DSL method to instantiate Synchronous Histrix Processor without Fallback processor
     *
     * @param actualProcessor the actual Camel processor that will be used to process the exchange
     * @return the Synchronous Histrix Processor
     */
    public static Processor sync(Processor actualProcessor) {
        return new SyncHystrixCommandProcessor(actualProcessor, null);
    }
}
