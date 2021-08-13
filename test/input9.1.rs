pub mod first {
	pub mod second {
		pub mod third {
			pub let nested = 42;
		}
	}
}

println("nested from withing the same file:", first.second.third.nested);
