// Test case expressions
let text = input("Enter number: ")
match parseInt(text)
	| val and has(val) then println(val * 2)
	| _ then println("Could not parse input number!")
