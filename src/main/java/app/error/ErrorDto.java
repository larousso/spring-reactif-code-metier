package app.error;
import io.vavr.control.Option;
import lombok.Value;

@Value
public class ErrorDto {

    public final String message;
    public final Option<String> path;
}
