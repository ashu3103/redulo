class Node {
    Node f1;
    int f2;
}

public class Test9 {
    public static void main(String[] args) {
        Node a = new Node();        
        a.f1 = new Node();
        Node b = a.f1; // 1st Store
        if (args.length > 0) {
            int k = 1;
        } else {
            int k = 2;
        }
        Node c = a.f1; // Redundant
         
        System.err.println("Test9 completed: "+ a + " " + b + " " + c);
    }

}