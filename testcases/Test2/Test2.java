public class Test2 {
	int f1;
	public static void main(String[] args) {
		Test2 a = new Test2(); // O4
		a.f1 = 10;
		int b = a.f1;
		int c = a.f1; // Redundant
		a.foo();
		int d = a.f1;
	}
	void foo() {
		Test2 o1 = new Test2(); // O12
		int x;
		o1.f1 = 20;
		Test2 o2 = o1;
		x = o1.f1;
		int y = o2.f1; // Redundant
	}
}
