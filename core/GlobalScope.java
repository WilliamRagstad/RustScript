package core;

import java.util.HashMap;

public class GlobalScope extends Scope {
	private String sourceFileDir;
	private HashMap<String, ProgramFunction> program; // Built in system functions
	private HashMap<String, Atom> exports; // Publicly exported variables

	public GlobalScope() {
		super("Global", null);
		this.program = new HashMap<String, ProgramFunction>();
		this.exports = new HashMap<String, Atom>();
	}

	public void addProgramFunction(String name, ProgramFunction function) {
		this.program.put(name, function);
	}

	public ProgramFunction getProgramFunction(String name) {
		return program.get(name);
	}

	public void setSourceFileDirectory(String sourceFileDir) {
		this.sourceFileDir = sourceFileDir;
	}

	public String getSourceFileDirectory() {
		return sourceFileDir;
	}

	/**
	 * Set a variable in the current scope.
	 *
	 * @param name  Name of the variable.
	 * @param value Value of the variable.
	 */
	public void export(String name, Atom value) {
		exports.put(name, value);
	}

	public HashMap<String, Atom> getExports() {
		return exports;
	}
}
