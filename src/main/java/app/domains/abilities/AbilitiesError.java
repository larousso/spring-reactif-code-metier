package app.domains.abilities;

import app.error.AppError;

public sealed interface AbilitiesError extends AppError {
    record AbilityUnmatch(String message) implements AbilitiesError {
        public AbilityUnmatch() {
            this("Couldn't find the required ability for your problem");
        }
    }
}
