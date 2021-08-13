// Public and private module with public and private properties.
// All modules are exported by default, code that is not meant
// to be exported can be private inside the module or located
// outside the module in the same file.
// All functions in a module are static by default, meaning
// when referencing to a private field from a public one
// requires the module name as prefix.


let square = fn(x) => x * x; // square is not accessible from outside this file.
let breakMul = fn() => Math.mul(1, 1);

mod Math {
	pub let PI = 3.14159265359;
	pub let E  = 2.71828182846;
	pub let TAU = 2 * PI;
	pub let EPSILON = 0.000001;

	let mul = fn(x, y) => x * y; // Not accessible from outside this module.
	pub let cube = fn(x) => mul(square(x), x); // Can access mul from withing the same module (or nested module).
}

println("square 2:", square(2));
println("Pi is:", Math.PI);
println("Pi cubed is:", Math.cube(Math.PI));

// These two should fail as mul is private.
println("breakMul: ", breakMul());
println("Pi * 10 is:", Math.mul(Math.PI, 10));
