package app.service;

import io.IO;
import app.command.AskForHelp;
import app.domains.abilities.Abilities;
import app.domains.abilities.Ability;
import app.domains.superheroes.SuperHeroes;
import app.domains.superheroes.Superhero;
import app.domains.superheroes.SuperheroErrors;
import app.domains.weakness.Weaknesses;
import app.error.AppError;
import app.error.AppErrors;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class FindHelpService {

    private final SuperHeroes superHeroes;
    private final Abilities abilities;
    private final Weaknesses weaknesses;

    public FindHelpService(SuperHeroes superHeroes, Abilities abilities, Weaknesses weaknesses) {
        this.superHeroes = superHeroes;
        this.abilities = abilities;
        this.weaknesses = weaknesses;
    }

    public IO<HelpErrors, HelpResult> findHelp(AskForHelp askForHelp) {
        return this.superHeroes.lookForSuperhero(askForHelp.name)
            .mapError(HelpErrors::fromSuperheroErrors)
            .flatMap(superhero ->
                    IO.parZip(
                            abilities.checkAbilities(superhero, askForHelp.problem).<AppError>refine(),
                            weaknesses.checkWeaknesses(superhero, askForHelp.problem).<AppError>refine(),
                            (abilities, __) -> HelpResult.builder()
                                    .hero(superhero)
                                    .matchingAbilities(abilities)
                                    .build()
                    )
                    .mapError(HelpErrors::new)
            );

    }

    @Builder(toBuilder = true)
    @Value
    public static class HelpResult {
        Superhero hero;
        List<Ability> matchingAbilities;
    }

    public static class HelpErrors extends AppErrors<AppError> {
        public HelpErrors(Seq<AppError> errors) {
            super(errors.toList());
        }

        public static HelpErrors fromSuperheroErrors(SuperheroErrors errors) {
            return new HelpErrors(errors.errors.map(e -> (AppError) e));
        }
    }
}
