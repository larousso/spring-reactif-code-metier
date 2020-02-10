package app;

import app.command.AskForHelp;
import app.service.FindHelpService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class HelpApi {

    private final FindHelpService findHelpService;

    public HelpApi(FindHelpService findHelpService) {
        this.findHelpService = findHelpService;
    }

    public Mono<ServerResponse> findHelp(ServerRequest request) {
        return request
                .bodyToMono(AskForHelp.class)
                .flatMap(command ->
                    findHelpService.findHelp(command)
                        .foldMono(
                             helpErrors -> ServerResponse.badRequest().bodyValue(helpErrors.dtoErrors()),
                             ok -> ServerResponse.ok().bodyValue(ok)
                        )
                );
    }

}
