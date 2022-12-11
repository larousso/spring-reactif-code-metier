package io;

import io.vavr.API;
import io.vavr.Function3;
import io.vavr.Function4;
import io.vavr.Function5;
import io.vavr.Tuple;
import io.vavr.Tuple0;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Tuple4;
import io.vavr.Tuple5;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Left;
import static io.vavr.API.List;
import static io.vavr.API.Match;
import static io.vavr.API.Right;
import static io.vavr.API.Seq;
import static io.vavr.API.Tuple;
import static io.vavr.Patterns.$Left;
import static io.vavr.Patterns.$Right;
import static io.vavr.Patterns.$Tuple2;

public class IO<E, A> {

    private final Mono<Either<E, A>> underlying;

    private IO(Mono<Either<E, A>> underlying) {
        this.underlying = underlying;
    }

    public static <E, A> IO<E, A> succeed(A value) {
        return new IO<>(Mono.just(Either.right(value)));
    }

    public static <E, A> IO<E, A> suspend(Supplier<A> supplier) {
        return new IO<>(Mono.fromCallable(() -> Either.right(supplier.get())));
    }

    public static <E, A> IO<E, A> error(E value) {
        return new IO<>(Mono.just(Either.left(value)));
    }

    public static <E, A> IO<E, A> failed(Throwable value) {
        return new IO<>(Mono.error(value));
    }

    public static <E, A> IO<E, A> fromMono(Mono<A> mono) {
        return new IO<>(mono.map(Either::right));
    }

    public static <E, A> IO<E, A> fromMono(Mono<A> mono, Class<E> errorClass) {
        return new IO<>(mono.map(Either::right));
    }

    public static <E, A> IO<E, A> fromMonoEither(Mono<Either<E, A>> mono) {
        return new IO<>(mono);
    }

    public static <E, A, Fail extends Throwable> IO<E, A> fromMono(Mono<A> mono, Class<Fail> clazz, Function<Fail, E> handleError) {
        Mono<Either<E, A>> underlying = mono
                .map(Either::<E, A>right)
                .onErrorResume(clazz, (Fail e) -> Mono.just(Either.left(handleError.apply(e))));
        return new IO<>(underlying);
    }

    public static <E, A> IO<E, A> fromEither(Either<E, A> either) {
        return new IO<>(Mono.just(either));
    }

    public static <E, A> IO<E, A> fromOption(Option<A> option, Supplier<E> ifEmpty) {
        return new IO<>(Mono.just(option.toEither(ifEmpty)));
    }

    public static <A> IO<Tuple0, A> fromOption(Option<A> option) {
        return new IO<>(Mono.just(option.toEither(() -> Tuple.empty())));
    }

    public static <E> IO<E, Tuple0> when(Boolean toCheck, Supplier<IO<E, ?>> errorIfTrue) {
        if (toCheck) {
            return errorIfTrue.get().map(__ -> Tuple.empty());
        } else {
            return IO.succeed(Tuple.empty());
        }
    }

    public static <E> IO<E, Tuple0> unit() {
        return IO.succeed(Tuple.empty());
    }

    public static <E, A> IO<E, Seq<A>> sequence(Seq<IO<E, A>> seq) {
        return seq.foldLeft(IO.<E, Seq<A>>succeed(List.empty()), (acc, elt) ->
           acc.flatMap(current -> elt.map(current::append))
        );
    }

    public static <E, A> IO<Seq<E>, Seq<A>> parSequence(Seq<IO<E, A>> seq) {
        return seq.foldLeft(IO.<Seq<E>, Seq<A>>succeed(List.empty()), (acc, elt) -> {
                    Mono<Either<Seq<E>, Seq<A>>> rMono = acc.underlying.flatMap( (Either<Seq<E>, Seq<A>> eitherAcc) ->
                            elt.underlying.map( (Either<E, A> either) -> {
                                Either<Seq<E>, Seq<A>> res = Match(Tuple(eitherAcc, either)).of(
                                        Case($Tuple2($Left($()), $Left($())), (Either.Left<Seq<E>, Seq<A>> accL, Either.Left<E, A> l) ->
                                                Left(accL.getLeft().append(l.getLeft()))
                                        ),
                                        Case($Tuple2($Right($()), $Left($())), (accL, l) ->
                                                Left(List(l.getLeft()))
                                        ),
                                        Case($Tuple2($Left($()), $Right($())), (accL, l) ->
                                                Left(accL.getLeft())
                                        ),
                                        Case($Tuple2($Right($()), $Right($())), (Either.Right<Seq<E>, Seq<A>> accR, Either.Right<E, A> r) ->
                                                Right(accR.get().append(r.get()))
                                        )
                                );
                                return res;
                            })
                    );
                    return new IO<>(rMono);
        });
    }


    public static class ValidateBuilder<E> {

        private final List<IO<E, Tuple0>> iOs;

        public ValidateBuilder(List<IO<E, ?>> iOs) {
            this.iOs = iOs.map(io -> io.map(___ -> Tuple.empty()));
        }

        public <A> IO<Seq<E>, A> andReturn(Supplier<A> res) {
            return IO.parSequence(this.iOs).map(__ -> res.get());
        }

        public <A> IO<Seq<E>, A> andReturn(A res) {
            return IO.parSequence(this.iOs).map(__ -> res);
        }
    }

    @SafeVarargs
    public static <E> ValidateBuilder<E> validate(IO<E, ?>... seq) {
        return new ValidateBuilder<E>(List.of(seq));
    }

    public static <E, A1, A2> IO<E, Seq<A2>> traverse(Seq<A1> seq, Function<A1, IO<E, A2>> func) {
        return sequence(seq.map(func));
    }

    public static <E, A1, A2> IO<Seq<E>, Seq<A2>> parTraverse(Seq<A1> seq, Function<A1, IO<E, A2>> func) {
        return parSequence(seq.map(func));
    }

    public static <E, A1, A2> IO<E, Tuple2<A1, A2>> zip(IO<E, A1> io1, IO<E, A2> io2) {
        return zip(io1, io2, API::Tuple);
    }

    public static <E, A1, A2, A> IO<E, A> zip(IO<E, A1> io1, IO<E, A2> io2, BiFunction<A1, A2, A> func) {
        return io1.flatMap(r1 -> io2.map(r2 -> func.apply(r1, r2)));
    }

    public static <E, A1, A2, A> IO<Seq<E>, A> parZip(IO<E, A1> io1, IO<E, A2> io2, BiFunction<A1, A2, A> func) {
        return new IO<Seq<E>, A>(io1.underlying.flatMap(either1 ->
            either1.fold(
                    err1 -> io2.underlying.map(either2 ->
                            either2.fold(
                                    err2 -> Either.left(Seq(err1, err2)),
                                    ok2 -> Either.left(Seq(err1))
                            )
                    ),
                    ok1 -> io2.underlying.map(either2 ->
                        either2.fold(
                            err2 -> Either.left(Seq(err2)),
                            ok2 ->  Either.right(func.apply(ok1, ok2))
                        )
                    )
            )
        ));
    }

    public static <E, A1, A2> IO<Seq<E>, Tuple2<A1, A2>> parZip(IO<E, A1> io1, IO<E, A2> io2) {
        return parZip(io1, io2, API::Tuple);
    }

    public static <E, A1, A2, A3, A> IO<Seq<E>, A> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, Function3<A1, A2, A3, A> func) {
        return parZip(parZip(io1, io2), io3.mapError(List::of))
            .map(t -> func.apply(t._1._1, t._1._2, t._2))
            .mapError(l -> l.flatMap(i -> i));
    }

    public static <E, A1, A2, A3, A4, A> IO<Seq<E>, A> parZipIO(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, IO<E, A4> io4, Function3<A1, A2, A3, IO<Seq<E>, A>> func) {
        return parZip(parZip(io1, io2), io3.mapError(List::of))
            .mapError(l -> l.flatMap(i -> i))
            .flatMap(t -> func.apply(t._1._1, t._1._2, t._2));
    }

    public static <E, A1, A2, A3, A4> IO<Seq<E>, Tuple4<A1, A2, A3, A4>> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, IO<E, A4> io4) {
        return parZip(io1, io2, io3, io4, (r1, r2, r3, r4) -> Tuple(r1, r2, r3, r4));
    }

    public static <E, A1, A2, A3, A4, A> IO<Seq<E>, A> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, IO<E, A4> io4, Function4<A1, A2, A3, A4, A> func) {
        return parZip(parZip(io1, io2, io3), io4.mapError(List::of))
                .map(t -> func.apply(t._1._1, t._1._2, t._1._3, t._2))
                .mapError(l -> l.flatMap(i -> i));
    }

    public static <E, A1, A2, A3, A4, A5> IO<Seq<E>, Tuple5<A1, A2, A3, A4, A5>> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, IO<E, A4> io4, IO<E, A5> io5) {
        return parZip(io1, io2, io3, io4, io5, (r1, r2, r3, r4, r5) -> Tuple(r1, r2, r3, r4, r5));
    }

    public static <E, A1, A2, A3, A4, A5, A> IO<Seq<E>, A> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, IO<E, A4> io4, IO<E, A5> io5, Function5<A1, A2, A3, A4, A5, A> func) {
        return parZip(parZip(io1, io2, io3, io4), io5.mapError(List::of))
            .map(t -> func.apply(t._1._1, t._1._2, t._1._3, t._1._4, t._2))
            .mapError(l -> l.flatMap(i -> i));
    }

    public static <E, A1, A2, A3, A> IO<Seq<E>, A> parZipIO(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3, Function3<A1, A2, A3, IO<Seq<E>, A>> func) {
        return parZip(parZip(io1, io2), io3.mapError(List::of))
                .mapError(l -> l.flatMap(i -> i))
                .flatMap(t -> func.apply(t._1._1, t._1._2, t._2));
    }

    public static <E, A1, A2, A3> IO<Seq<E>, Tuple3<A1, A2, A3>> parZip(IO<E, A1> io1, IO<E, A2> io2, IO<E, A3> io3) {
        return parZip(io1, io2, io3, (r1, r2, r3) -> Tuple(r1, r2, r3));
    }

    public IO<E, Tuple0> then() {
        return this.map(__ -> Tuple.empty());
    }

    public <E1> IO<E1, A> downcast() {
        return this.mapError(e -> (E1)e);
    }

    public <A1> IO<E, A1> map(Function<A, A1> function) {
        return new IO<>(this.underlying.map(either -> either.map(function)));
    }

    public <A1> IO<E, A1> flatMap(Function<A, IO<E, A1>> function) {
        return new IO<>(this.underlying.flatMap(either ->
                either.fold(
                        err -> Mono.just(Either.left(err)),
                        ok -> function.apply(ok).underlying
                )
        ));
    }

    public <E1> IO<E1, A> mapError(Function<E, E1> function) {
        return new IO<>(this.underlying.map(either -> either.mapLeft(function)));
    }

    public IO<E, A> doOnSuccess(Function<A, Mono<Tuple0>> consumer) {
        return new IO<>(this.underlying.flatMap(either ->
            either.fold(
                    err -> Mono.just(Either.left(err)),
                    ok ->  consumer.apply(ok).map(__  -> Either.right(ok))
            )
        ));
    }

    public IO<E, A> doOnError(Function<E, Mono<Tuple0>> consumer) {
        return new IO<>(this.underlying.flatMap(either ->
            either.fold(
                    err -> consumer.apply(err).map(__ -> Either.left(err)),
                    ok -> Mono.just(Either.right(ok))
            )
        ));
    }

    public  IO<E, A> onErrorReturn(A recover) {
        return new IO<>(this.underlying.map(either ->
            Either.right(either.getOrElse(recover))
        ));
    }

    public <T> Mono<T> fold(Function<E, T> handleError, Function<A, T> handleSuccess) {
        return underlying.map(either -> either.fold(handleError, handleSuccess));
    }

    public <T> Mono<T> foldMono(Function<E, Mono<T>> handleError, Function<A, Mono<T>> handleSuccess) {
        return underlying.flatMap(either -> either.fold(handleError, handleSuccess));
    }

    public Mono<Either<E, A>> unlift() {
        return underlying;
    }

    public IO<Throwable, Either<E, A>> attempt() {
        return new IO<>(underlying
                .map(either -> Either.<Throwable, Either<E, A>>right(either))
                .onErrorResume(Throwable.class, e -> Mono.just(Either.left(e)))
        );
    }

    public IO<E, A> failureToError(Function<Throwable, E> recover) {
        return new IO<>(underlying.onErrorResume(e -> Mono.just(Left(recover.apply(e)))));
    }

    public <T extends Throwable> IO<E, A> failureToError(Class<T> clazz, Function<T, E> recover) {
        return new IO<>(underlying.onErrorResume(clazz, e -> Mono.just(Left(recover.apply(e)))));
    }

    public IO<E, A> failureToError(Predicate<Throwable> test, Function<Throwable, E> recover) {
        return new IO<>(underlying.onErrorResume(test, e -> Mono.just(Left(recover.apply(e)))));
    }

    public Either<E, A> block() {
        return underlying.block();
    }

    public Either<E, A> block(Duration timeout) {
        return underlying.block(timeout);
    }

    public IO<E, A> filter(Predicate<A> predicate, Supplier<E> onInvalidPredicate){
        return new IO<E,A>(this.underlying.map(either ->
            either.flatMap( ok -> {
                if(predicate.test(ok)){
                    return Either.right(ok);
                } else {
                    return Either.left(onInvalidPredicate.get());
                }
            })
        ));
    }


    public static <E,A> IO<E,A> rules(boolean isValid, Supplier<A> onSuccess, Supplier<E> onError){
        if (isValid) {
            return IO.succeed(onSuccess.get());
        } else {
            return IO.error(onError.get());
        }
    }

    public static <E,A> IO<E,A> fromMonoOption(Mono<Option<A>> maybeA, Supplier<E> onEmpty){
        return new IO<E,A>(maybeA.map(option -> option.toEither(onEmpty)));
    }

    public IO<A,E> swap(){
        return new IO<A,E>(this.underlying.map( either -> either.swap()));
    }
}
