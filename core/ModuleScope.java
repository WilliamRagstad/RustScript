package core;

import java.util.HashMap;

public class ModuleScope extends Scope {
	private boolean setToPrivateEnv = false;
	private HashMap<String, Atom> privateEnvironment; // Built in system functions

	public ModuleScope(String name, Scope parentScope) {
		super("Module " + name, parentScope);
		this.privateEnvironment = new HashMap<String, Atom>();
	}

	/**
	 * Get a variable from the current scope or its parent scopes.
	 *
	 * @param name The name of the variable to find.
	 * @return The variable if found, null otherwise.
	 */
	public Atom get(String name, int sourceScopeId, boolean callFromChild) {
		if (environment.containsKey(name)) {
			return environment.get(name);
		}
		if (sourceScopeId == this.getID() || callFromChild) {
			// If fetching variable from within the module, private env is accessible.
			if (privateEnvironment.containsKey(name)) {
				return privateEnvironment.get(name);
			}
			// Else try child scopes.
		}
		if (parentScope != null) {
			return parentScope.get(name, sourceScopeId);
		}
		return null;
	}

	/**
	 * Set a variable in the current scope.
	 *
	 * @param name  Name of the variable.
	 * @param value Value of the variable.
	 * @return The unit atom.
	 */
	public Atom set(String name, Atom value) {
		if (setToPrivateEnv) {
			privateEnvironment.put(name, value);
		} else {
			environment.put(name, value);
		}
		return new Atom.Unit();
	}

	/**
	 * Toggle set variable declarations to private environment.
	 */
	public void setToPrivateEnv(boolean setToPrivateEnv) {
		this.setToPrivateEnv = setToPrivateEnv;
	}

	/**
	 * Format the current scope as text.
	 */
	public String toString() {
		return String.format("Scope[%s] { id: %s, public: %s, private: %s }", name, scopeId, environment.size(),
				privateEnvironment.size());
	}
}
