let R1 = seq([let d = "ThisIsNice", let d = substr(d, 6, 10), d + " weather"])
print(typeof(R1)); println(" R1 = " + R1)

let R2 = seq([
    let d = println("This"), // Now supporting comments!
    let d = "is", println(d),
    let d = ^d - 8, println(d), println("test"), d
])
print(typeof(R2)); println(" R2 = " + R2)

// Bool parsing
let getBool = fn(s) => if (^lower(s) == 't') then substr(s, 0, 4) else substr(s, 0, 5)
println(getBool("truethis")); println(getBool("falsethis"))