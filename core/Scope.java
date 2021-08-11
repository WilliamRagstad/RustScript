package core;

import java.util.ArrayList;
import java.util.HashMap;

public class Scope {
	public static int scopeCount = 0;
	public int scopeId = 0;
	public String name = null;
	public Scope parentScope = null;
	public ArrayList<Scope> childScopes = new ArrayList<Scope>();

	protected HashMap<String, Atom> environment;

	protected Scope(String name, Scope parentScope) {
		this.scopeId = scopeCount++;
		this.name = name;
		this.parentScope = parentScope;
		this.environment = new HashMap<String, Atom>();
	}

	protected Scope(Scope parentScope) {
		this("Anonymous", parentScope);
	}

	/**
	 * Derive a new scope as child of the current.
	 *
	 * @param name Name of the new child scope.
	 * @return A new child scope.
	 */
	public Scope deriveNew(String name) {
		Scope child = new Scope(name, this);
		childScopes.add(child);
		return child;
	}

	/**
	 * Derive a new scope as child of the current.
	 *
	 * @return A new child scope.
	 */
	public Scope deriveNew() {
		return deriveNew(null);
	}

	/**
	 * Get a variable from the current scope or its parent scopes.
	 *
	 * @param name The name of the variable to find.
	 * @return The variable if found, null otherwise.
	 */
	public Atom get(String name) {
		if (has(name)) {
			return environment.get(name);
		}
		if (parentScope != null) {
			return parentScope.get(name);
		}
		return null;
	}

	public void set(String name, Atom value) {
		environment.put(name, value);
	}

	public boolean has(String name) {
		return environment.containsKey(name);
	}

	/**
	 * Get a builtin program function from the global scope.
	 *
	 * @param name The name of the builtin function to find.
	 * @return The function if found, null otherwise.
	 */
	public ProgramFunction getProgramFunction(String name) {
		return parentScope.getProgramFunction(name);
	}

	/**
	 * Clear the current scope with all scoped variables and all its child scopes.
	 */
	public void clear() {
		this.environment.clear();
		childScopes.forEach(Scope::clear);
		childScopes.clear();
	}

	/**
	 * Format the current scope as text.
	 */
	public String toString() {
		return "Scope " + scopeId + ": " + name + " (" + environment.size() + " variables)";
	}
}
