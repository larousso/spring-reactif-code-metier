package app.domains.superheroes;

import io.IO;
import app.domains.superheroes.SuperheroError.SuperheroUnavailable;
import app.domains.superheroes.SuperheroError.SuperheroUnknown;
import org.springframework.stereotype.Component;

@Component
public class SuperHeroes {

    private final SuperheroRepository superheroRepository;

    public SuperHeroes(SuperheroRepository superheroRepository) {
        this.superheroRepository = superheroRepository;
    }

    public IO<SuperheroErrors, Superhero> lookForSuperhero(String name) {
        return IO.fromMono(superheroRepository.findByName(name), SuperheroErrors.class)
                .flatMap(mayBeSuperHero ->
                        IO.fromOption(mayBeSuperHero, () -> SuperheroErrors.of(new SuperheroUnknown(name)))
                )
                .filter(hero -> hero.isAvailable, () -> SuperheroErrors.of(new SuperheroUnavailable(name)));
    }
}
