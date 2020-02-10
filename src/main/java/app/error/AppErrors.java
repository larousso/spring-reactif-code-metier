package app.error;

import io.vavr.collection.List;

public class AppErrors<E extends AppError> {

    public final List<E> errors;

    public AppErrors(List<E> errors) {
        this.errors = errors;
    }

    public static <Err extends AppError> AppErrors<Err> empty() {
        return new AppErrors<>(List.empty());
    }

    public AppErrors<E> combine(AppErrors<E> other) {
        return new AppErrors<>(errors.appendAll(other.errors));
    }

    public List<ErrorDto> dtoErrors() {
        return errors.map(err -> new ErrorDto(err.message(), err.path()));
    }
}
