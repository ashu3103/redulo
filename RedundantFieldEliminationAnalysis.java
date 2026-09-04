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
import soot.jimple.AssignStmt;
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

    protected final Map<String, String> valueToLocalMap;

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

        this.valueToLocalMap = new HashMap<>(g.size(), 0.7f);
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
            } else if (stmt instanceof AssignStmt) {
                /* Field references */
                Value use = u.getUseBoxes().get(0).getValue();
                Value def = u.getDefBoxes().get(0).getValue();
                if (def instanceof Local) {
                    use = u.getUseBoxes().get(0).getValue();
                } else {
                    use = u.getUseBoxes().get(1).getValue();
                }
                
                if (use instanceof InstanceFieldRef && def instanceof Local) {
                    InstanceFieldRef useRef = (InstanceFieldRef)use;
                    Local defL = (Local)def;
                    
                    FieldRefKey frk = new FieldRefKey(useRef.getBase(), useRef.getField());
                    /* if base of use points to something make an indirection from  */
                    if (valueToLocalMap.get(useRef.getBase().toString()) != null) {
                        String p = valueToLocalMap.get(useRef.getBase().toString());
                        if (!IsStackLocal(defL)) {
                            frk.setLocalAndValue(p + "." + useRef.getField().getSignature(), defL.toString());
                        }
                    } else if (valueToLocalMap.get(useRef.toString()) != null) {
                        String p = valueToLocalMap.get(useRef.toString());
                        frk.setLocalAndValue(defL.toString(), p);
                    } else {
                        if (!IsStackLocal(defL)) {
                            frk.setLocalAndValue(useRef.toString(), defL.toString());
                        }
                    }
                    
                    gen.add(frk);
                } else if (def instanceof InstanceFieldRef && use instanceof Local) {
                    InstanceFieldRef defRef = (InstanceFieldRef)def;
                    Local useL = (Local)use;
                    // System.out.println(useL.toString());
                    // System.out.println(valueToLocalMap.get(useL.toString()));
                    FieldRefKey frk = new FieldRefKey(defRef.getBase(), defRef.getField());
                    if (valueToLocalMap.get(useL.toString()) != null) {
                        String p = valueToLocalMap.get(useL.toString());
                        frk.setLocalAndValue(defRef.toString(), p);
                        gen.add(frk);
                    } else {
                        kill.add(frk);
                    }
                } else {
                    // Do nothing
                }
            } else {
                // do nothing
            }

            unitToKillSet.put(u, kill);
            unitToGenerateSet.put(u, gen);
            System.out.printf("%-60s | %-30s | %-30s%n",
                u.toString(),
                kill.toString(),
                gen.toString());
        }
    }

    private boolean IsStackLocal(Value v) {
        if (v instanceof Local) {
            Local l = (Local) v;
            return l.isStackLocal();
        }
        return false;
    }

    private boolean IsConstructor(InvokeStmt stmt) {
        InvokeExpr expr  = stmt.getInvokeExpr();
        SootMethod m = expr.getMethod();
        if ("<init>".equals(m.getName())) {
            return true;
        }
        return false;
    }

    private FieldRefKey getFieldRefKeyFromFlowSet(FlowSet<FieldRefKey> s, FieldRefKey k) {
        for (FieldRefKey ks: s) {
            if (ks.equals(k)) return ks;
        }
        return null;
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
        FlowSet<FieldRefKey> unionSet = unitToGenerateSet.get(u);
        for (FieldRefKey k : unionSet) {
            out.remove(k);
            out.add(k);
        }
    }

    @Override
    protected void merge(FlowSet<FieldRefKey> out1, FlowSet<FieldRefKey> out2, FlowSet<FieldRefKey> in) {
        out1.intersection(out2, in);  // references should have been loaded along all paths to program point

        for (FieldRefKey k: in) {
            String base = k.toStringBase();
            String ref = k.toString();

            FieldRefKey out1Frk = getFieldRefKeyFromFlowSet(out1, k);
            FieldRefKey out2Frk = getFieldRefKeyFromFlowSet(out2, k);

            if (out1Frk.getLocalFromValue(base) == out2Frk.getLocalFromValue(base) || out1Frk.getLocalFromValue(ref) == out2Frk.getLocalFromValue(ref)) {
                valueToLocalMap.putAll(k.localValueToLocalMap);
            }
        }

    }

    @Override
    protected void copy(FlowSet<FieldRefKey> out, FlowSet<FieldRefKey> in) {
        in = emptySet.clone();
        for (FieldRefKey k: out) {
            valueToLocalMap.putAll(k.localValueToLocalMap);
            in.add(k);
        }
    }
}
