import core.Interpreter;

public class Bench {
	public static void main(String[] args) throws Exception {
		Interpreter i = new Interpreter();
		try {
			i.execute(
					"let ack = fn (m, n) => if (m == 0) then (n + 1) else (if (n == 0) then (ack(m - 1, 1)) else (ack(m - 1, ack(m, n - 1))))");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		long t1 = System.currentTimeMillis();
		try {
			i.execute("ack(3, 8)");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		long t2 = System.currentTimeMillis();
		System.out.println(String.format("Time: %d ms", t2 - t1));
	}
}
