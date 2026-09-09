class Node {
	Node f1;
	Node f2;
	Node g;
	Node() {}
}

public class Test1 {
	Test1 f;
	public static void main(String[] args) {
		Test1 a1 = new Test1();
		Test1 a2 = new Test1();
		Test1 a3 = new Test1();
		a2.f = new Test1();
		Test1 c = a2.f;
		a1.f = a2.f;
		a3.f = a1.f;
	}
}
