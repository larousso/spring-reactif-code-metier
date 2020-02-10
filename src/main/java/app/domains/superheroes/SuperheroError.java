package app.domains.superheroes;

import app.error.AppError;
import lombok.Value;

import java.text.MessageFormat;

public interface SuperheroError extends AppError {

    @Value
    class SuperheroUnknown implements SuperheroError {
        public final String name;
        @Override
        public String message() {
            return MessageFormat.format("{0} is unknown", name);
        }
    }

    @Value
    class SuperheroUnavailable implements SuperheroError {
        public final String name;
        @Override
        public String message() {
            return MessageFormat.format("{0} is not available at the moment", name);
        }
    }
}
