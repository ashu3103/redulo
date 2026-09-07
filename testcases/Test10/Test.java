class Node {
    Node f1;
    int f2;
}

public class Test {
    public static void main(String[] args) {
        Node a = new Node();        
        a.f1 = new Node();
        Node b = a.f1;    // 1st Store
        int t = 0;  
        for (int i = 0; i < args.length ; i++) {
            t = b.f2 * 2;
        }
        Node c = a.f1;   // Redundant
        System.err.println("Test completed: "+ a + " " + b + " " + c + " " + t);
    }

}
