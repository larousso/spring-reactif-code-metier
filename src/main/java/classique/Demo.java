package classique;

import app.domains.superheroes.Superhero;
import io.IO;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class Demo {

    static class PersonDto {
        public String name;
        public String email;
    }

//    static public Person createPerson(String name, String email) throws EmailFormatInvalid, NameFormatInvalid {
//        return null;
//    }

    interface PersonValidationError {
        class EmailFormatInvalid implements PersonValidationError {
        }

        class NameFormatInvalid implements PersonValidationError {
        }
    }

    static public Either<PersonValidationError, Person> createPerson(String name, String email) {
        return null;
    }

    static class EmailFormatInvalid extends Exception {
    }

    static class NameFormatInvalid extends Exception {
    }


    @Value
    static class Person {
        String name;
        String email;
    }
//
//    @PostMapping(path = "/api/persons")
//    public Mono<ResponseEntity<?>> postPerson(@RequestBody PersonDto personDto) {
//        Mono<Person> person = Mono.fromCallable(() -> createPerson(personDto.name, personDto.email));
//        return person
//                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(person))
//                .onErrorResume(EmailFormatInvalid.class, emailFormatInvalid ->
//                        Mono.just(ResponseEntity.badRequest().body(emailFormatInvalid))
//                )
//                .onErrorResume(NameFormatInvalid.class, emailFormatInvalid ->
//                        Mono.just(ResponseEntity.badRequest().body(emailFormatInvalid))
//                );
//    }

//    public Mono<ServerResponse> postPerson(ServerRequest request) {
//        return request.bodyToMono(PersonDto.class).flatMap(personDto -> {
//            // Lève une checked exception
//            Mono<Person> person = Mono.fromCallable(() -> createPerson(personDto.name, personDto.email));
//            return person
//                    .flatMap(p -> ServerResponse.ok().bodyValue(person))
//                    .onErrorResume(EmailFormatInvalid.class, emailFormatInvalid ->
//                            ServerResponse.badRequest().bodyValue(emailFormatInvalid)
//                    )
//                    .onErrorResume(NameFormatInvalid.class, nameFormatInvalid ->
//                            ServerResponse.badRequest().bodyValue(nameFormatInvalid)
//                    );
//        });
//    }

    public Mono<ServerResponse> postPerson(ServerRequest request) {
        return request.bodyToMono(PersonDto.class).flatMap(personDto -> {
            Mono<Either<PersonValidationError, Person>> person = Mono.just(createPerson(personDto.name, personDto.email));
            return person
                    .flatMap(personOrError ->
                            personOrError.fold(
                                error -> ServerResponse.badRequest().bodyValue(error),
                                p -> ServerResponse.ok().bodyValue(p)
                        )
                    );
        });
    }

    @Value
    static class Error {
        String message;
    }

    static IO<Error, Person> getById(String id) {
        return IO.failed(new RuntimeException());
    }

    public static void main(String[] args) {

        IO<Error, Seq<Person>> persons = IO.traverse(List.of("1", "2", "3"), id -> getById(id));

        IO<Error, Person> auntMay = IO.succeed(new Person("May", "may@gmail.com"));
        IO<Error, Superhero> spiderman = IO.succeed(new Superhero("1", "spiderman"));
        IO<Error, Superhero> batman = IO.succeed(new Superhero("2", "batman"));
        IO<Error, Superhero> superman = IO.succeed(new Superhero("3", "superman"));

        IO<Error, Seq<Superhero>> all = IO.sequence(List.of(spiderman, batman, superman));

        IO<Seq<Error>, Tuple2<Person, Superhero>> mayAndspiderman = IO.parZip(auntMay, spiderman);

//        // Checked exception
//        Mono<Person> person = Mono.fromCallable(() -> createPerson("John Doe", "johndoe@gmail.com"));
//        person.onErrorResume(EmailFormatInvalid.class, e -> { /* Gérer l'erreur */
//            return Mono.just(new Person());
//        });
//
//        Mono<Person> personFailed = Mono.just(createPerson("John Doe", "johndoe@gmail.com"));
//
//
    }
}
