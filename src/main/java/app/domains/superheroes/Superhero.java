package app.domains.superheroes;

import app.domains.abilities.Ability;
import app.domains.weakness.Weakness;
import io.vavr.collection.List;
import lombok.*;

@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Superhero {

    public final String id;
    public final String name;
    public final Boolean isAvailable;

    public final List<Ability> abilities;
    public final List<Weakness> weaknesses;

    public Superhero(String id, String name) {
        this.id = id;
        this.name = name;
        this.isAvailable = true;
        this.abilities = List.empty();
        this.weaknesses = List.empty();
    }
}
