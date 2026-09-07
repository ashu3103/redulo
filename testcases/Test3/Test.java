class Node {
	Node f1;
	Node g;
	Node() {}
}

public class Test {
	public static void main(String[] args) {
		Node a = new Node();
		Node b = new Node();
		Node c = new Node();
		a.f1 = new Node();
		b.f1 = new Node();
		c.f1 = new Node();
		Node d = a.f1;
		Node e = b.f1;
		Node f = a.f1; // Redundant
		Node g = c.f1;
		System.err.println("Test completed: "+ d + " " + e + " " + f + " " + g);
	}
}