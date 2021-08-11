// Public and private module with public and private properties.
// All modules are exported by default, code that is not meant
// to be exported can be private inside the module or located
// outside the module in the same file.
// All functions in a module are static by default, meaning
// when referencing to a private field from a public one
// requires the module name as prefix.


let square = fn(x) => Math.mul(x, x); // Not accessible from outside this file.

mod Math {
	pub let PI = 3.1415;
	pub let TAU = 2 * PI;
	pub let E = 2.7183;
	pub let EPSILON = 0.000001;

	let mul = fn(x, y) => x * y; // Not accessible from outside this module.
	pub let cube = fn(x) => Math.mul(square(x), x);
}

println("Pi is:", Math.PI);
println("Pi cubed is:", Math.cube(Math.PI));
println("Pi multiplied by 10 is:", Math.mul(Math.PI, 10)); // Should fail as mul is private.
