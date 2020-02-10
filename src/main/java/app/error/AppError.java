package app.error;

import io.vavr.control.Option;

public interface AppError {

    String message();

    default Option<String> path() {
        return Option.none();
    }

}
