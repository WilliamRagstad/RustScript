# Examples

RustScript is an expression based language; **everything is an expression**.

The following examples are created using the interactive REPL.

### Basic Arithmetic

```rust
4 * -3 + 12 - -3 + 4 * 15
// 63
```

### Variables

```rust
let x = 5
x * 15 // 75
```

### Booleans

```rust
true != false // true
-true // false
-((true && false) || true) // false
```

### Lists

```rust
let ls = [1, 2, 9, 4, 5]
ls // [1, 2, 9, 4, 5]

let ls = ls + [2, 4, 6, 8]
ls // [1, 2, 9, 4, 5, 2, 4, 6, 8]
^ls // 1
$ls // [2, 9, 4, 5, 2, 4, 6, 8]
```

### Characters and Strings

```rust
let capC = 'C'
"Cool!" == [capC, 'o', 'o', 'l', '!'] // true

'A' + 2 // 'C'
'B' - 1 // 'A'
'A' > 'B' // false

"Hello" + '!' // "Hello!"

"Hello, " + "world!" // "Hello, world!"

"Hi" + [65, 10] // ['H', 'i', 65, 10]
```

### Special Characters

```rust
"\u0007" == "\a" // true
"\t" + "Hi" + '\n' //	Hi
'\u0049' - 8 == 'A' // true
```

[Foramtting logic](https://github.com/WilliamRagstad/RustScript/blob/main/core/formatting/EscapeSequence.java).

### Ranges

```rust
[5..12]
// [5, 6, 7, 8, 9, 10, 11]
```

### List Comprehensions

```rust
[x * x for x in [0..15]]
// [0, 1, 4, 9, 16, 25, 36, 49, 64, 81, 100, 121, 144, 169, 196]
```

### Lambdas

```rust
let f = fn (x) => x * 2
f(30) // 60

let apply_twice = fn (f, x) => f(f(x))
apply_twice(f, 5) // 20
```

### Lambda variations

```rust
let x = fn() => 2
x
// Lambda [
//         {argNames: [], expr: 2}
// ]

var x = fn(y) => y
x
// Lambda [
//         {argNames: [], expr: 2}
//         {argNames: [y], expr: "y"}
// ]

x() // 2
x(6) // 6

var x = fn(y) => y
// Error: Lambda already has a variation with arity 1
```

### Conditionals

```rust
if (3 < 5) then (4) else (3)
// 4

[if (x % 3 == 0) then (x / 3) else (x * 2) for x in [0..10]]
// [0, 2, 4, 1, 8, 10, 2, 14, 16, 3]

let fib = fn (n) => if (n < 2) then (1) else (fib(n - 1) + fib(n - 2))
fib(15) // 987
```

### Pattern matching

```rust
let text = input("Enter number: ")
match parseVal(text)
	| val and has(val) then println(val * 2)
	| _ then println("Could not parse input number!")
```

### Modules

```rust
mod Math {
	pub mod Constants {
		pub let PI = 3.1415;
	}
}

println("Pi is:", Math.Constants.PI); // Pi is: 3.1415
```

### Imports/Exports
`file1.rs`:
```rust
let priv_add = fn(a, b) => a + b;
let priv_sub = fn(a, b) => a - b;
let priv_mul = fn(a, b) => a * b;
let priv_div = fn(a, b) => a / b;

pub let mod = fn(a, b) => floor((a/(b*1.0)-floor(a/(b*1.0)))*b);

pub mod Calc {
	pub let add = priv_add;
	pub let sub = priv_sub;
	pub let mul = priv_mul;
	pub let div = priv_div;
}
```
`file2.rs`:
```rust
imp mod, Calc from "file1.rs"

mod(5, 3) // 2
Calc.sub(6, 1) // 5
```

### Small Standard Library

```rust
range(3, 5)
// [3, 4]

fmap(fib, [5..10] + [3, 2])
// [8, 13, 21, 34, 55, 3, 2]

> filter(fn (n) => n % 3 == 0, [0..20])
// [0, 3, 6, 9, 12, 15, 18]

> fold(fn (a, b) => a + b, 0, [0..20])
// 190

> sum([0..20])
// 190

> product([1..10])
// 362880
```

