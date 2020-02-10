package app.domains.weakness;

import app.error.AppError;
import io.vavr.collection.List;
import lombok.Value;

import java.text.MessageFormat;

public interface WeaknessesError extends AppError {

    @Value
    class WeaknessMatchError implements WeaknessesError {

        public final List<Weakness> weaknesses;

        @Override
        public String message() {
            return MessageFormat.format("Weaknesses found : {0}", weaknesses.mkString("[", "","]"));
        }
    }

}
