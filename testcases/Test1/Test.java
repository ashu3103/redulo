class Node {
	Node f1;
	Node f2;
	Node g;
	Node() {}
}

public class Test {
	Test f;
	public static void main(String[] args) {
		Test a1 = new Test();
		Test a2 = new Test();
		Test a3 = new Test();
		a2.f = new Test();
		Test c = a2.f;
		a1.f = a2.f;
		a3.f = a1.f;
		

	}
}
