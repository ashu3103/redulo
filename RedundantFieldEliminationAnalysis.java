
import java.util.*;
import soot.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ArrayFlowUniverse;
import soot.toolkits.scalar.ArrayPackedSet;
import soot.toolkits.scalar.BoundedFlowSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.FlowUniverse;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;

public class RedundantFieldEliminationAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<FieldRefKey>> {
    protected final Integer precision;
    protected final Map<Unit, FlowSet<FieldRefKey>> unitToKillSet;
    protected final Map<Unit, FlowSet<FieldRefKey>> unitToGenerateSet;
    protected final FlowSet<FieldRefKey> emptySet;
    private final List<FieldRefKey> universe;

    protected final Map<FieldRefKey, Value> loadedValueMap;

    public RedundantFieldEliminationAnalysis(DirectedGraph<Unit> dg, int precision) {
        super(dg);

        this.precision = Integer.valueOf(precision);
        // Universe of all field references that appear anywhere in the body.
        LinkedHashSet<FieldRefKey> fieldRefs = new LinkedHashSet<>();
        /* we need a universe of all field references */
        final UnitGraph g = (UnitGraph) dg;
        for (Unit u : g.getBody().getUnits()) {
            for (ValueBox box : u.getUseAndDefBoxes()) {
                Value v = box.getValue();
                if (v instanceof InstanceFieldRef) {
                    InstanceFieldRef ref = (InstanceFieldRef) v;
                    fieldRefs.add(new FieldRefKey(ref.getBase(), ref.getField()));
                }
            }
        }

        /* initialize the emptySet */
        this.universe = new ArrayList<>(fieldRefs);
        FlowUniverse<FieldRefKey> fieldRefsUniv = new ArrayFlowUniverse<>(universe.toArray(new FieldRefKey[0]));
        this.emptySet = new ArrayPackedSet<>(fieldRefsUniv);

        this.loadedValueMap = new HashMap<>(g.size(), 0.7f);
        this.unitToKillSet = new HashMap<>(g.size() * 2 + 1, 0.7f);
        this.unitToGenerateSet = new HashMap<>(g.size() * 2 + 1, 0.7f);

        /* create gen set for the CFG */
        switch (this.precision.intValue()) {
            case 1:
                buildKillGenSets(g);
                break;
            default:
                buildKillGenSets(g); // by default go with lowest precision
                break;
        }

        doAnalysis();
    }

    private boolean IsConstructor(InvokeStmt stmt) {
        InvokeExpr expr  = stmt.getInvokeExpr();
        SootMethod m = expr.getMethod();
        if ("<init>".equals(m.getName())) {
            return true;
        }
        return false;
    }

    private void buildKillGenSets(UnitGraph g) {
        System.out.println("-".repeat(126));
        System.out.printf("%-60s | %-30s | %-30s%n",
        "Unit", "Kill", "Gen");

        System.out.println("-".repeat(126));
        for (Unit u: (g.getBody().getUnits())) {
            FlowSet<FieldRefKey> kill = this.emptySet.clone();
            FlowSet<FieldRefKey> gen  = this.emptySet.clone();

            Stmt stmt = (Stmt) u;
            boolean hasInvoke = stmt.containsInvokeExpr();

            if (hasInvoke && !IsConstructor((InvokeStmt)stmt)) {
                // skip the constructors and methods that take no arguments
                for (FieldRefKey k : universe) {
                    kill.add(k);
                }
            } else {
                // Field reads (uses) become available after this unit.
                for (ValueBox b : u.getUseBoxes()) {
                    Value v = b.getValue();
                    if (v instanceof InstanceFieldRef) {
                        InstanceFieldRef ref = (InstanceFieldRef) v;
                        gen.add(new FieldRefKey(ref.getBase(), ref.getField()));
                    }
                }
            }

            // Defs: field writes and local reassignments.
            for (ValueBox b : u.getDefBoxes()) {
                Value v = b.getValue();
                if (v instanceof InstanceFieldRef) {
                    InstanceFieldRef ref = (InstanceFieldRef) v;
                    for (FieldRefKey k : universe) {
                        // Conservative: The base may alias to other locals, for example:
                        // b = a
                        // b.f1 = ...
                        // c = a.f1
                        if (k.field.equals(ref.getField())) {
                            kill.add(k);
                        }
                    }
                    gen.add(new FieldRefKey(ref.getBase(), ref.getField()));
                } else if (v instanceof Local) {
                    for (FieldRefKey k : universe) {
                        if (k.base.equivTo(v)) {
                            kill.add(k);
                        }
                    }
                }
            }

            unitToKillSet.put(u, kill);
            unitToGenerateSet.put(u, gen);
            System.out.printf("%-60s | %-30s | %-30s%n",
                u.toString(),
                kill.toString(),
                gen.toString());
        }
    }

    @Override
    protected FlowSet<FieldRefKey> newInitialFlow() {
        BoundedFlowSet<FieldRefKey> out = (BoundedFlowSet<FieldRefKey>) emptySet.clone();
        out.complement(out);    // universal set for maximum precision (based on our confluence operator)
        return out;
    }

    @Override
    protected FlowSet<FieldRefKey> entryInitialFlow() {
        return emptySet.clone();   // initially no reference is loaded
    }

    @Override
    protected void flowThrough(FlowSet<FieldRefKey> out, Unit u, FlowSet<FieldRefKey> in) {
        // remove killed references from the in
        in.intersection(unitToKillSet.get(u), out);
        // add generated references to the out set
        out.union(unitToGenerateSet.get(u));
    }

    @Override
    protected void merge(FlowSet<FieldRefKey> out1, FlowSet<FieldRefKey> out2, FlowSet<FieldRefKey> in) {
        out1.intersection(out2, in);  // references should have been loaded along all paths to program point
    }

    @Override
    protected void copy(FlowSet<FieldRefKey> out, FlowSet<FieldRefKey> in) {
        out.copy(in);
    }
}
