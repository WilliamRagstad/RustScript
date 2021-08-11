// Code blocks
let blockFn = fn(x) => {
	let y = 2;
	let z = 3;
	x + y + z;
}

println(blockFn(5));

{
	// Block scoped variables
	let a = 12;
}
println(a);
