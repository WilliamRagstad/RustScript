let R1 = ^reverse([let d = "ThisIsNice", let d = substr(d, 6, 10), d + " weather"]);

let R2 = ^reverse([
    let d = println("This"),
    let d = "is",
    println(d),
    let d = ^d - 9,
    println(d),
    println("test")
])

let getBool = fn(s) => if (^lower(s) == 't') then substr(s, 0, 4) else substr(s, 0, 5)
println(getBool("truethis"))
println(getBool("falsethis"))
