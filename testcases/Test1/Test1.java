class Node {
	Node f1;
	Node f2;
	Node g;
	Node() {}
}

public class Test1 {
	public static void main(String[] args) {
		Node a = new Node(); // O10
		a.f1 = new Node(); // O11
		Node b = new Node(); // O12
		b.f1 = new Node(); // O13
		a.f2 = new Node(); // O14
		Node c = a.f1;
		a.f2 = a.f1; // Redundant
		b.f1 = a.f2; // Redundant
	}
}
