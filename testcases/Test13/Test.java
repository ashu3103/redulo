class Node {
    Node f1;
    Node f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();        
        a.f1 = new Node();
        Node b = a.f1;    // 1st Store
        a.f2 = a.f1;  // Redundant
        Node c = a.f1; // Redundant
        System.err.println("Test completed: "+ a + " " + b + " " + c);
    }

}