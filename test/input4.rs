let getBool = fn(s) => if (^lower(s) == 't') then substr(s, 0, 4) else substr(s, 0, 5)
println(getBool("truethis"))
println(getBool("falsethis"))
