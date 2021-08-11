let blockFn = fn() => {
	let x = 1;
	let y = 2;
	let z = 3;
	x + y + z;
}

println(blockFn());

{
	// Block scoped variables
	let a = 12;
}
println(a);
