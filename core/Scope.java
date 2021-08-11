package core;

import java.util.ArrayList;
import java.util.HashMap;

public class Scope {
	private static int scopeCount = 0;
	private int scopeId = 0;
	private String name;
	private Scope parentScope = null;
	private Scope sharedScope = null;
	private boolean searchChildScopes = false;
	private ArrayList<Scope> childScopes = new ArrayList<Scope>();

	protected HashMap<String, Atom> environment;

	protected Scope(String name, Scope parentScope) {
		this.scopeId = scopeCount++;
		this.name = name;
		this.parentScope = parentScope;
		this.environment = new HashMap<String, Atom>();
	}

	/**
	 * Derive a new scope as child of the current.
	 *
	 * @return A new child scope.
	 */
	public Scope deriveNew(String name) {
		Scope child = new Scope(name, this);
		childScopes.add(child);
		return child;
	}

	/**
	 * Set a shared scope. Used in modules for sharing public and private variables.
	 *
	 * @param sharedScope Scope to be shared.
	 */
	public void setSharedScope(Scope sharedScope) {
		this.sharedScope = sharedScope;
	}

	/**
	 * Allows searching child scopes for variables.
	 */
	public void searchChildScopes(boolean allowed) {
		this.searchChildScopes = allowed;
	}

	/**
	 * Get a variable from the current scope or its parent scopes.
	 *
	 * @param name The name of the variable to find.
	 * @return The variable if found, null otherwise.
	 */
	public Atom get(String name) {
		if (environment.containsKey(name)) {
			return environment.get(name);
		}
		if (sharedScope != null) {
			Atom result = sharedScope.get(name);
			if (result != null) {
				return result;
			}
			// Else try child scopes.
		}
		if (searchChildScopes) {
			for (Scope child : childScopes) {
				Atom result = child.get(name);
				if (result != null) {
					return result;
				}
			}
			// Else try parent scope.
		}
		if (parentScope != null) {
			return parentScope.get(name);
		}
		return null;
	}

	/**
	 * Get or find a variable from the current scope or its parent scopes by
	 * providing an identifier of type Atom.Ident or Atom.IdentList.
	 *
	 * @param identifier Identifier Atom.Ident or Atom.IdentList.
	 * @return The variable if found, null otherwise.
	 * @throws Exception If the identifier is not an Atom.Ident or Atom.IdentList.
	 */
	public Atom getByIdent(Atom identifier) throws Exception {
		if (identifier instanceof Atom.Ident) {
			return get(((Atom.Ident) identifier).name);
		} else if (identifier instanceof Atom.IdentList) {
			return find(((Atom.IdentList) identifier).getIdentifiers());
		} else {
			throw new RuntimeException("Cannot get variable using non Ident or IdentList identifier atom argument");
		}
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
	public Atom find(String[] identifierNames) throws Exception {
		Scope scopePath = this; // Temporary scope to follow the identifier names path to the deepest module.
		Atom module = null;
		if (identifierNames.length == 0) {
			throw new Exception("Identifier name cannot be empty.");
		}
		String deepestIdentifier = identifierNames[identifierNames.length - 1];
		for (int i = 0; i < identifierNames.length - 1; i++) {
			String name = identifierNames[i];
			module = scopePath.get(name);
			if (module == null) {
				throw new Exception(String.format("Tried to access nonexistent module %s", name));
			} else if (module instanceof Atom.Module) {
				scopePath = ((Atom.Module) module).getPublicScope();
			} else {
				throw new Exception(String.format("Tried to access property on non-module %s", name));
			}
		}

		return scopePath.get(deepestIdentifier);
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

	public String getName() {
		return this.name;
	}

	/**
	 * Format the current scope as text.
	 */
	public String toString() {
		return String.format("Scope[%s] { id: %s, size: %s }", name, scopeId, environment.size());
	}
}
