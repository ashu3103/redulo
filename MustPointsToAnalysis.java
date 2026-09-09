import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

abstract class NodePA {
    public String toString() { return "#Node"; }
}

class LocalNode extends NodePA {
    private final Local local;

    LocalNode(Local local) {
        this.local = local;
    }

    public Local getLocal() {
        return this.local;
    }

    @Override
    public String toString() {
        return local.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LocalNode) {
            LocalNode l = (LocalNode)o;
            return this.local.equals(l.local);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.local.hashCode();
    }
}

class HeapObject extends NodePA {
    private final Unit allocationSite;

    HeapObject(Unit u) {
        this.allocationSite = u;
    }

    @Override
    public String toString() {
        LineNumberTag ln = (LineNumberTag)this.allocationSite.getTag("LineNumberTag");
        return "O" + ln.getLineNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
    
        if (o instanceof HeapObject) {
            HeapObject other = (HeapObject)o;
            return this.allocationSite.equals(other.allocationSite);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.allocationSite.hashCode();
    }
}

class Edge {
    private final NodePA src;
    private final SootField field;  // null means point-to edge
    private final NodePA dst;

    Edge(NodePA src, SootField f, NodePA dst) {
        this.src = src;
        this.field = f;
        this.dst = dst;
    }

    public NodePA getSrc() { return this.src; }
    public NodePA getDst() { return this.dst; }
    public SootField getField() { return this.field; }

    @Override
    public String toString() {
        if (this.field == null) {
            return src + "-->" + dst;
        }
        return src + "--" + field.getName() + "->" + dst;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o instanceof Edge) {
            Edge other = (Edge)o;
            return src.equals(other.src) &&
                    Objects.equals(field, other.field) &&
                    dst.equals(other.dst);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, field, dst);
    }
}

class PTState {
    Set<Edge> edges = new HashSet<>();
    boolean top = true;
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PTState)) {
            return false;
        }

        PTState other = (PTState) o;

        return this.top == other.top && this.edges.equals(other.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(top, edges);
    }
}

public class MustPointsToAnalysis extends ForwardFlowAnalysis<Unit, PTState> {
    public MustPointsToAnalysis(DirectedGraph<Unit> dg) {
        super(dg);
        // Do analysis
        doAnalysis();
    }

    private void killAllReachableObjects(Set<Edge> edges, Set<Local> arguments) {
        Set<NodePA> worklist = new HashSet<>();
        Set<NodePA> visited = new HashSet<>();

        for (Edge edge : edges) {
            if (edge.getField() == null &&
                edge.getSrc() instanceof LocalNode) {

                LocalNode src = (LocalNode) edge.getSrc();
                if (arguments.contains(src.getLocal())) {
                    worklist.add(edge.getDst());
                }
            }
        }

        while (!worklist.isEmpty()) {
            NodePA object = worklist.iterator().next();
            worklist.remove(object);
            if (!visited.add(object))
                continue;

            for (Edge edge : edges) {
                if (edge.getField() != null &&
                    edge.getSrc().equals(object)) {
                    worklist.add(edge.getDst());
                }
            }
        }

        edges.removeIf(edge ->
            edge.getField() != null &&
            visited.contains(edge.getSrc())
        );
    }

    private void killLocalPointsTo(Set<Edge> edges, Local a) {
        LocalNode other = new LocalNode(a);

        edges.removeIf(edge ->
            edge.getField() == null &&
            edge.getSrc().equals(other)
        );
    }

    private void killLocalPointsToByField(Set<Edge> edges, HeapObject obj, SootField f) {
        if (obj == null || f == null)
            return;

        edges.removeIf(edge ->
            edge.getField() != null &&
            edge.getField().equals(f) &&
            edge.getSrc().equals(obj)
        );
    }

    public HeapObject objectPointedToByLocal(Set<Edge> edges, Local a) {
        LocalNode ln = new LocalNode(a);
        for (Edge edge : edges) {
            if (edge.getField() == null && edge.getSrc().equals(ln)) {
                return (HeapObject)edge.getDst();
            }
        }
        return null;
    }

    public HeapObject objectPointedToByField(Set<Edge> edges, HeapObject obj, SootField f) {
        if (obj == null || f == null)
            return null;

        for (Edge edge: edges) {
            if (edge.getField() != null && edge.getField().equals(f) && edge.getSrc().equals(obj)) {
                return (HeapObject)edge.getDst();
            }
        }
        return null;
    }

    public boolean IsConstructor(InvokeExpr expr) {
        SootMethod m = expr.getMethod();
        if ("<init>".equals(m.getName())) {
            return true;
        }
        return false;
    }

    @Override
    protected PTState newInitialFlow() {
        PTState pts = new PTState();
        pts.top = true;
        return pts;
    }

    @Override
    protected PTState entryInitialFlow() {
        PTState pts = new PTState();
        pts.top = false;
        return pts;
    }

    @Override
    protected void flowThrough(PTState in, Unit u, PTState out) {
        if (in.top) {
            out.top = true;
            out.edges.clear();
            return;
        }

        copy(in, out);

        Stmt stmt = (Stmt) u;
        if (stmt instanceof InvokeStmt) {
            // Assume everything that goes inside a function is modified and so is everything reachable by the arguments
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (IsConstructor(invokeExpr)) {
                return;
            }

            Set<Local> arguments = new HashSet<>();

            for (ValueBox box : invokeExpr.getUseBoxes()) {
                Value value = box.getValue();
                if (value instanceof Local) {
                    Local local = (Local) value;
                    arguments.add(local);
                }
            }

            killAllReachableObjects(out.edges, arguments);
        } else if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;
            Value lhs = assign.getLeftOp();
            Value rhs = assign.getRightOp();

            if (rhs instanceof NewExpr && lhs instanceof Local) {    // a = new A
                HeapObject obj = new HeapObject(u);
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                killLocalPointsTo(out.edges, l);
                // add a pointer from l to new object
                LocalNode ln = new LocalNode(l);
                out.edges.add(new Edge(ln, null, obj));
            } else if (rhs instanceof Local && lhs instanceof Local) {  // r1 = r2
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                Local r = (Local) rhs;
                killLocalPointsTo(out.edges, l);
                // add an edge from l to object pointed to by rhs
                LocalNode ln = new LocalNode(l);
                HeapObject obj = objectPointedToByLocal(out.edges, r);
                if (obj != null)
                    out.edges.add(new Edge(ln, null, obj));
            } else if (rhs instanceof InstanceFieldRef && lhs instanceof Local) { // r1 = r2.f
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                InstanceFieldRef r = (InstanceFieldRef) rhs;
                killLocalPointsTo(out.edges, l);
                // add an edge from l to object pointed to by r2.f
                LocalNode ln = new LocalNode(l);
                SootField f = r.getField();
                Local base = (Local)r.getBase();
                HeapObject obj = objectPointedToByLocal(out.edges, base);
                HeapObject obj2 = objectPointedToByField(out.edges, obj, f);
                if (obj2 != null)
                    out.edges.add(new Edge(ln, null, obj2));
            } else if (rhs instanceof Local && lhs instanceof InstanceFieldRef) {  // r1.f = r2
                // remove all the reference of the lhs from graph
                Local r = (Local) rhs;
                InstanceFieldRef ref = (InstanceFieldRef) lhs;
                SootField f = ref.getField();
                Local base = (Local) ref.getBase();
                HeapObject obj1 = objectPointedToByLocal(out.edges, base);
                killLocalPointsToByField(out.edges, obj1, f);
                // add the object pointed to by r2
                HeapObject obj2 = objectPointedToByLocal(out.edges, r);
                if (obj1 != null && obj2 != null)
                    out.edges.add(new Edge(obj1, f, obj2));
            } else {
                // Do nothing
            }
        }
    }

    @Override
    protected void merge(PTState in1, PTState in2, PTState out) {
        if (in1.top) {
            copy(in2, out);
            return;
        }

        if (in2.top) {
            copy(in1, out);
            return;
        }

        out.top = false;
        out.edges.clear();
        out.edges.addAll(in1.edges);
        out.edges.retainAll(in2.edges);
    }

    @Override
    protected void copy(PTState in, PTState out) {
        out.top = in.top;
        out.edges.clear();
        out.edges.addAll(in.edges);
    }
}
