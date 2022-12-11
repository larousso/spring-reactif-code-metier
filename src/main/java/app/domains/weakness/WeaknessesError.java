package app.domains.weakness;

import app.error.AppError;
import io.vavr.collection.List;
import lombok.Value;

import java.text.MessageFormat;

public sealed interface WeaknessesError extends AppError {

    record WeaknessMatchError(String message, List<Weakness> weaknesses) implements WeaknessesError {
        public WeaknessMatchError(List<Weakness> weaknesses) {
            this(MessageFormat.format("Weaknesses found : {0}", weaknesses.mkString("[", "","]")), weaknesses);
        }
    }

}
