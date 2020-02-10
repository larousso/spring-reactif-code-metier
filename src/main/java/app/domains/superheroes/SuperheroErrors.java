package app.domains.superheroes;

import app.error.AppErrors;
import io.vavr.collection.List;

public class SuperheroErrors extends AppErrors<SuperheroError> {
    public SuperheroErrors(List<SuperheroError> errors) {
        super(errors);
    }

    public static SuperheroErrors of(SuperheroError e) {
        return new SuperheroErrors(List.of(e));
    }
}