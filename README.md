<div>
    <h1>
        <img src="assets/logo.png" height="90">
    </h1>
    <p>
        RustScript is a scripting language with as much relation to Rust as JavaScript has to Java.
    </p>
</div>

![GitHub all releases](https://img.shields.io/github/downloads/WilliamRagstad/RustScript/total?style=flat-square) <!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->



## Usage

RustScript is an expression based language; everything is an expression.
There are global variables though.



> ## 📥 Install
>
> [Download](https://github.com/WilliamRagstad/RustScript/releases/latest/download/RustScript.jar) the latest version, or [view all releases](https://github.com/WilliamRagstad/RustScript/releases).



### 🔌Run

Run a script using the following command.

```shell
java -jar RustScript.jar <script>
```

### 🔨 Build

If you want to build RustScript on your own, run the command below to build the `Runner.java` class into a standalone executable.

```shell
jar -cvmf manifest.txt RustScript.jar *.class core/*.class
```



### Examples

The following examples are created using the interactive REPL.

#### Basic Arithmetic

```rust
4 * -3 + 12 - -3 + 4 * 15
// 63
```

### Variables

```rust
let x = 5
x * 15 // 75
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
	got val and has(val) then println(val * 2)
	got _ then println("Could not parse input number!")
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

### Project Euler

A few project euler problems

#### Problem 1

> If we list all the natural numbers below 10 that are multiples of 3 or 5, we get 3, 5, 6 and 9. The sum of these multiples is 23.
> Find the sum of all the multiples of 3 or 5 below 1000.

```rust
sum([x for x in [0..1000] if x % 3 == 0 || x % 5 == 0])
// 233168
```

#### Problem 2

> Each new term in the Fibonacci sequence is generated by adding the previous two terms. By starting with 1 and 2, the first 10 terms will be:

> 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, ...

>By considering the terms in the Fibonacci sequence whose values do not exceed four million, find the sum of the even-valued terms.


```rust
let fib = fn (n) => ^$fold(fn (ls, i) => [^$ls, ^ls + ^$ls], [1, 1], [0..n])
let fibs = [fib(n) for n in [1..35]]
sum([f for f in fibs if f < 4000000 && f % 2 == 0])
// 4613732
```

#### Problem 3

> The prime factors of 13195 are 5, 7, 13 and 29.
> What is the largest prime factor of the number 600851475143 ?

```haskell
let find = fn (f, ls) => if (ls) then (if (f(^ls)) then (^ls) else (find(f, $ls))) else (false)
let factor = fn (n) => find(fn (i) => n % i == 0, [2..n / 2])
```

600851475143 is larger than `int` allows so I'd have to make it support longs to do this one.

It's worth noting that `factor` doubles as `is_prime`, since it returns false for prime numbers.

#### Problem 5

> 2520 is the smallest number that can be divided by each of the numbers from 1 to 10 without any remainder.
> What is the smallest positive number that is evenly divisible by all of the numbers from 1 to 20?

```haskell
let gcd = fn (a, b) => if (b == 0)  then (a) else (gcd(b, (a % b)))
let lcm = fn (a, b) => (a * b) / (gcd(a, b))
fold(lcm, 1, [1..20])
// 232792560
```

#### Problem 6

> The sum of the squares of the first ten natural numbers is,
> 1^2 + 2^2 + ... + 10^2 = 385$$
> 
> The square of the sum of the first ten natural numbers is,
> (1 + 2 + ... + 10)^2 = 55^2 = 3025$$
> 
>Hence the difference between the sum of the squares of the first ten natural numbers and the square of the sum is $3025 - 385 = 2640$.
>Find the difference between the sum of the squares of the first one hundred natural numbers and the square of the sum.

```haskell
let square = fn (x) => x * x
square(sum([1..100])) - (sum(fmap(square, [1..100])))
```
> Once again the numbers are too big



## Logo Credits

The logo is a modified image by [Smashicons](https://smashicons.com/) from [Flaticon](https://www.flaticon.com). The original images can be found [here](https://www.flaticon.com/search?word=rusty&style=all&order_by=4&type=icon) or in the [assets](https://github.com/WilliamRagstad/RustScript/tree/main/assets) folder.




## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://www.williamragstad.com/"><img src="https://avatars.githubusercontent.com/u/41281398?v=4?s=100" width="100px;" alt=""/><br /><sub><b>William Rågstad</b></sub></a><br /><a href="#maintenance-WilliamRagstad" title="Maintenance">🚧</a> <a href="https://github.com/WilliamRagstad/RustScript/commits?author=WilliamRagstad" title="Tests">⚠️</a> <a href="https://github.com/WilliamRagstad/RustScript/commits?author=WilliamRagstad" title="Code">💻</a> <a href="#design-WilliamRagstad" title="Design">🎨</a></td>
    <td align="center"><a href="https://mikail-khan.com"><img src="https://avatars.githubusercontent.com/u/24574272?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Mikail Khan</b></sub></a><br /><a href="https://github.com/WilliamRagstad/RustScript/commits?author=mkhan45" title="Documentation">📖</a> <a href="https://github.com/WilliamRagstad/RustScript/commits?author=mkhan45" title="Tests">⚠️</a> <a href="https://github.com/WilliamRagstad/RustScript/commits?author=mkhan45" title="Code">💻</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
