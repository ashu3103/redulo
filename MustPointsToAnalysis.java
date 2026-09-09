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

public class MustPointsToAnalysis extends ForwardFlowAnalysis<Unit, Set<Edge>> {
    protected final Set<Edge> emptySet;

    public MustPointsToAnalysis(DirectedGraph<Unit> dg) {
        super(dg);
        // Universe of all field references that appear anywhere in the body.
        LinkedHashSet<Value> fieldRefs = new LinkedHashSet<>();
        /* we need a universe of all field references */
        final UnitGraph g = (UnitGraph) dg;
        for (Unit u : g.getBody().getUnits()) {
            for (ValueBox box : u.getUseAndDefBoxes()) {
                Value v = box.getValue();
                if (v instanceof InstanceFieldRef) {
                    InstanceFieldRef ref = (InstanceFieldRef) v;
                    fieldRefs.add(ref);
                }
            }
        }

        /* initialize the emptySet */
        this.emptySet = new HashSet<>();

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

    public boolean IsConstructor(InvokeStmt stmt) {
        InvokeExpr expr  = stmt.getInvokeExpr();
        SootMethod m = expr.getMethod();
        if ("<init>".equals(m.getName())) {
            return true;
        }
        return false;
    }

    @Override
    protected Set<Edge> newInitialFlow() {
        Set<Edge> out = new HashSet<>(this.emptySet);
        return out;
    }

    @Override
    protected Set<Edge> entryInitialFlow() {
        Set<Edge> out = new HashSet<>(this.emptySet);
        return out;
    }

    @Override
    protected void flowThrough(Set<Edge> in, Unit u, Set<Edge> out) {
        this.copy(in, out);

        Stmt stmt = (Stmt) u;
        if (stmt instanceof InvokeStmt) {
            // Assume everything that goes inside a function is modified and so is everything reachable by the arguments
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (IsConstructor((InvokeStmt)stmt)) {
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

            killAllReachableObjects(out, arguments);
        } else if (stmt instanceof AssignStmt) {
            AssignStmt assign = (AssignStmt) stmt;
            Value lhs = assign.getLeftOp();
            Value rhs = assign.getRightOp();

            if (rhs instanceof NewExpr && lhs instanceof Local) {    // a = new A
                HeapObject obj = new HeapObject(u);
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                killLocalPointsTo(out, l);
                // add a pointer from l to new object
                LocalNode ln = new LocalNode(l);
                out.add(new Edge(ln, null, obj));
            } else if (rhs instanceof Local && lhs instanceof Local) {  // r1 = r2
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                Local r = (Local) rhs;
                killLocalPointsTo(out, l);
                // add an edge from l to object pointed to by rhs
                LocalNode ln = new LocalNode(l);
                HeapObject obj = objectPointedToByLocal(out, r);
                if (obj != null)
                    out.add(new Edge(ln, null, obj));
            } else if (rhs instanceof InstanceFieldRef && lhs instanceof Local) { // r1 = r2.f
                // remove all the reference of lhs from graph
                Local l = (Local) lhs;
                InstanceFieldRef r = (InstanceFieldRef) rhs;
                killLocalPointsTo(out, l);
                // add an edge from l to object pointed to by r2.f
                LocalNode ln = new LocalNode(l);
                SootField f = r.getField();
                Local base = (Local)r.getBase();
                HeapObject obj = objectPointedToByLocal(out, base);
                HeapObject obj2 = objectPointedToByField(out, obj, f);
                if (obj2 != null)
                    out.add(new Edge(ln, null, obj2));
            } else if (rhs instanceof Local && lhs instanceof InstanceFieldRef) {  // r1.f = r2
                // remove all the reference of the lhs from graph
                Local r = (Local) rhs;
                InstanceFieldRef ref = (InstanceFieldRef) lhs;
                SootField f = ref.getField();
                Local base = (Local) ref.getBase();
                HeapObject obj1 = objectPointedToByLocal(out, base);
                killLocalPointsToByField(out, obj1, f);
                // add the object pointed to by r2
                HeapObject obj2 = objectPointedToByLocal(out, r);
                if (obj1 != null && obj2 != null)
                    out.add(new Edge(obj1, f, obj2));
            } else {
                // Do nothing
            }
        }
    }

    @Override
    protected void merge(Set<Edge> in1, Set<Edge> in2, Set<Edge> out) {
        out.clear();
        out.addAll(in1);
        out.retainAll(in2);
    }

    @Override
    protected void copy(Set<Edge> in, Set<Edge> out) {
        out.clear();
        out.addAll(in);
    }
}
