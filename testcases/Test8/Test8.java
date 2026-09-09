class Node {
    Node f1;
    int f2;
}

public class Test8 {
    public static void main(String[] args) {
        Node a = new Node();
        a.f1 = new Node();
        Node b = a.f1; // 1st Store
        if(a.f1 instanceof Node) { // Redundant
            a.f1 = new Node();
        }
        Node c = a.f1; 
        System.err.println("Test8 completed: "+ a + " " + b + " " + c);
    }

}