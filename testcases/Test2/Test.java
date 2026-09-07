public class Test {
	int f1;
	public static void main(String[] args) {
		Test a = new Test(); // O4
		a.f1 = 10;
		int b = a.f1;
		int c = a.f1; // Redundant
		a.foo();
		int d = a.f1;
	}
	void foo() {
		Test o1 = new Test(); // O12
		int x;
		o1.f1 = 20;
		Test o2 = o1;
		x = o1.f1;
		int y = o2.f1; // Redundant
	}
}
