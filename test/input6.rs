// Test case expressions
let text = input("Enter number: ")
match parseInt(text)
	got val and has(val) then println(val * 2)
	got _ then println("Could not parse input number!")
