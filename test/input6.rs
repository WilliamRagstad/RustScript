// Test case expressions
let text = input("Enter number: ")
match parseVal(text)
	got [val, val2] and has(val) then println(val * 2)
	got _ then println("Could not parse input number!")