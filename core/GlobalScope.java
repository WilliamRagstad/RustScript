package core;

import java.util.HashMap;

public class GlobalScope extends Scope {
	private HashMap<String, ProgramFunction> program; // Built in system functions

	public GlobalScope() {
		super(null);
		this.program = new HashMap<String, ProgramFunction>();
	}

	public void addProgramFunction(String name, ProgramFunction function) {
		this.program.put(name, function);
	}

	public ProgramFunction getProgramFunction(String name) {
		return program.get(name);
	}
}
