package core;

import java.util.ArrayList;
import java.util.HashMap;

public class Scope {
	private static int scopeCount = 0;
	protected int scopeId = 0;
	protected String name;
	protected Scope parentScope = null;
	private ArrayList<Scope> childScopes = new ArrayList<Scope>();

	protected HashMap<String, Atom> environment;

	protected Scope(String name, Scope parentScope) {
		this.scopeId = scopeCount++;
		this.name = name;
		this.parentScope = parentScope;
		this.environment = new HashMap<String, Atom>();

		if (this.parentScope != null) {
			this.parentScope.childScopes.add(this);
		}
	}

	/**
	 * Derive a new scope as child of the current.
	 *
	 * @return A new child scope.
	 */
	public Scope deriveNew(String name) {
		return new Scope(name, this);
	}

	public void addEnv(HashMap<String, Atom> env) {
		this.environment.putAll(env);
	}

	public HashMap<String, Atom> getEnv() {
		return this.environment;
	}

	/**
	 * Allows searching child scopes for variables.
	 */
	// public void searchChildScopes(boolean allowed) {
	// this.searchChildScopes = allowed;
	// }

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
		if (parentScope != null) {
			return parentScope.get(name, sourceScopeId, true);
		}
		return null;
	}

	public Atom get(String name, int sourceScopeId) {
		return get(name, sourceScopeId, false);
	}

	/**
	 * Assume the get method is called on the current scope.
	 *
	 * @param name
	 * @return
	 */
	public Atom get(String name) {
		return get(name, scopeId);
	}

	/**
	 * Get or find a variable from the current scope or its parent scopes by
	 * providing an identifier of type Atom.Ident or Atom.IdentList.
	 *
	 * @param identifier Identifier Atom.Ident or Atom.IdentList.
	 * @return The variable if found, null otherwise.
	 * @throws Exception If the identifier is not an Atom.Ident or Atom.IdentList.
	 */
	public Atom getByIdent(Atom identifier, int sourceScopeId) throws Exception {
		if (identifier instanceof Atom.Ident) {
			return get(((Atom.Ident) identifier).name, sourceScopeId);
		} else if (identifier instanceof Atom.IdentList) {
			return find(((Atom.IdentList) identifier).getIdentifiers(), sourceScopeId);
		} else {
			throw new RuntimeException("Cannot get variable using non Ident or IdentList identifier atom argument");
		}
	}

	public Atom getByIdent(Atom identifier) throws Exception {
		return getByIdent(identifier, scopeId);
	}

	/**
	 * Set a variable in the current scope.
	 *
	 * @param name  Name of the variable.
	 * @param value Value of the variable.
	 */
	public void set(String name, Atom value) {
		environment.put(name, value);
	}

	/**
	 * Follow the module path described by the identifier names and return the
	 * variable located in the deepest scope.
	 *
	 * @param identifierNames The list of identifiers to follow.
	 * @return The variable if found, null otherwise.
	 * @throws Exception If the module path cannot be followed.
	 */
	public Atom find(String[] identifierNames, int sourceScopeId) throws Exception {
		Scope scopePath = this; // Temporary scope to follow the identifier names path to the deepest module.
		Atom module = null;
		if (identifierNames.length == 0) {
			throw new Exception("Identifier name cannot be empty.");
		}
		String deepestIdentifier = identifierNames[identifierNames.length - 1];
		for (int i = 0; i < identifierNames.length - 1; i++) {
			String name = identifierNames[i];
			module = scopePath.get(name, sourceScopeId);
			if (module == null) {
				throw new Exception(String.format("Tried to access nonexistent module %s", name));
			} else if (module instanceof Atom.Module) {
				scopePath = ((Atom.Module) module).getModuleScope();
			} else {
				throw new Exception(String.format("Tried to access property on non-module %s", name));
			}
		}

		return scopePath.get(deepestIdentifier, sourceScopeId);
	}

	public Atom find(String[] identifierNames) throws Exception {
		return find(identifierNames, scopeId);
	}

	/**
	 * Check if a variable is defined in the current scope or a parent scope.
	 *
	 * @param name Name of the variable.
	 * @return True if the variable is defined, false otherwise.
	 */
	public boolean has(String name) {
		return environment.containsKey(name) || parentScope.has(name);
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

	public int getID() {
		return this.scopeId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Format the current scope as text.
	 */
	public String toString() {
		return String.format("Scope[%s] { id: %s, size: %s }", name, scopeId, environment.size());
	}
}
