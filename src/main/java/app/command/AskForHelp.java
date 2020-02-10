package app.command;

import app.entities.Problem;
import lombok.Value;

@Value
public class AskForHelp {

    public final String name;
    public final Problem problem;

}
