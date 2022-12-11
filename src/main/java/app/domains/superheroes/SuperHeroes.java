package app.domains.superheroes;

import io.IO;
import app.domains.superheroes.SuperheroError.SuperheroUnavailable;
import app.domains.superheroes.SuperheroError.SuperheroUnknown;
import io.vavr.control.Option;
import org.springframework.stereotype.Component;

@Component
public class SuperHeroes {

    private final SuperheroRepository superheroRepository;

    public SuperHeroes(SuperheroRepository superheroRepository) {
        this.superheroRepository = superheroRepository;
    }

    public IO<SuperheroError, Superhero> lookForSuperhero(String name) {
        return IO.<SuperheroError, Option<Superhero>>fromMono(superheroRepository.findByName(name))
                .flatMap(mayBeSuperHero ->
                        IO.fromOption(mayBeSuperHero, () -> new SuperheroUnknown(name))
                )
                .filter(hero -> hero.isAvailable, () -> new SuperheroUnavailable(name));
    }
}
