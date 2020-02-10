package app.domains.superheroes;

import io.vavr.control.Option;
import reactor.core.publisher.Mono;

public interface SuperheroRepository {

    Mono<Option<Superhero>> findByName(String name);

}
