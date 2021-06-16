package core;
import java.util.ArrayList;

public interface ProgramFunction {
    public Atom call(ArrayList<Atom> args) throws Exception;
}