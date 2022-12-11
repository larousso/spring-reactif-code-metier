package app.domains.superheroes;

import app.error.AppError;
import lombok.Value;

import java.text.MessageFormat;

public sealed interface SuperheroError extends AppError {

    record SuperheroUnknown(String message, String name) implements SuperheroError {
        public SuperheroUnknown(String name) {
            this(MessageFormat.format("{0} is unknown", name), name);
        }
    }

    record SuperheroUnavailable(String message, String name) implements SuperheroError {
        public SuperheroUnavailable(String name) {
            this(MessageFormat.format("{0} is not available at the moment", name), name);
        }
    }
}
