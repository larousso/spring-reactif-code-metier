package app;

import lombok.Data;

@Data
public class Unit {

    private static Unit UNIT = new Unit();

    public static Unit unit() {
        return UNIT;
    }
}
