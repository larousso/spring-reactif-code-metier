package app.domains.weakness;

import app.domains.superheroes.Superhero;
import app.entities.Problem;
import io.IO;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.springframework.stereotype.Component;

@Component
public class Weaknesses {

    private final Map<Problem, List<Weakness>> IA = HashMap.of(
            Problem.BanditInTown, List.of(Weakness.Fearfull),
            Problem.CarAccident,  List.of(Weakness.Dumb),
            Problem.FellIntoWater,List.of(Weakness.Water),
            Problem.SuperVilain,  List.of(Weakness.TooNice, Weakness.Cryptonic, Weakness.Borderline)
    );

    public IO<WeaknessesError, Tuple0> checkWeaknesses(Superhero superhero, Problem problem) {

        List<Weakness> requiredWeaknesses = IA.getOrElse(problem, List.empty());
        List<Weakness> superheroWeaknesses = superhero.weaknesses;

        List<Weakness> usableWeaknesses = superheroWeaknesses.filter(requiredWeaknesses::contains);
        if (usableWeaknesses.isEmpty()) {
            return IO.unit();
        } else {
            return IO.error(new WeaknessesError.WeaknessMatchError(usableWeaknesses));
        }
    }


}
