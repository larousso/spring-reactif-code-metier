package app.domains.abilities;

import app.error.AppError;

public interface AbilitiesError extends AppError {

    class AbilityUnmatch implements AbilitiesError {
        @Override
        public String message() {
            return "Couldn't find the required ability for your problem";
        }
    }
}
