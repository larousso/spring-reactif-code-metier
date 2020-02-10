package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class Router {
    private final static Logger LOGGER = LoggerFactory.getLogger(Router.class);

    @Bean
    RouterFunction<ServerResponse> monoRouterFunction(HelpApi helpApi) {
        LOGGER.info("Inititalizing routes !");
        return route(POST("/api/helps/_command").and(accept(APPLICATION_JSON)), helpApi::findHelp);
    }
}
