package com.backbase.camel.hystrix.test;

import org.apache.camel.main.Main;

import com.backbase.camel.hystrix.example.HystrixMetricsStreamRoute;

/**
 * A Camel Application
 *
 * TODO: Refactor to Unit Test
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.enableHangupSupport();
        main.addRouteBuilder(new GoogleGeoCodesRoutes());
        main.addRouteBuilder(new HystrixMetricsStreamRoute());
        main.run(args);
    }
}

