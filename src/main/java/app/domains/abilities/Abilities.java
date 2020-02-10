package app.domains.abilities;

import io.IO;
import app.domains.superheroes.Superhero;
import app.entities.Problem;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.springframework.stereotype.Component;

@Component
public class Abilities {

    private final Map<Problem, List<Ability>> IA = HashMap.of(
            Problem.BanditInTown, List.of(Ability.DistanceFight, Ability.MeleeFight, Ability.MidDistanceFight, Ability.IronBody),
            Problem.CarAccident, List.of(Ability.Fly, Ability.Strength),
            Problem.FellIntoWater, List.of(Ability.Dive),
            Problem.SuperVilain, List.of(Ability.LazerEyes, Ability.IronBody, Ability.ElasticBody)
    );

    public IO<AbilitiesError, List<Ability>> checkAbilities(Superhero superhero, Problem problem) {

        List<Ability> requiredAbilities = IA.getOrElse(problem, List.empty());
        List<Ability> superheroAbilities = superhero.abilities;

        List<Ability> usableAbilities = superheroAbilities.filter(requiredAbilities::contains);
        if (usableAbilities.isEmpty()) {
            return IO.error(new AbilitiesError.AbilityUnmatch());
        } else {
            return IO.succeed(usableAbilities);
        }
    }

}
