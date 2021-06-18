let text = input("Enter number: ")
let val = parseVal(text)
println(typeof(val))
if typeof(val) == "Val" then println(val * 2) else println("Could not parse input number!")