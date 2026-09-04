import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.HashMap;
import java.util.Map;

/**
 * Value-numbering based redundant field load analysis.
 *
 * Two maps are carried as the flow fact:
 *   valueOf     : Local -> FieldRefKey       ("this local currently equals this symbolic expr")
 *   valueSource : FieldRefKey -> Value       ("this symbolic expr's value currently lives here")
 *
 * valueSource is the useful new piece over the earlier available-set design:
 * once you know a load is redundant, valueSource.get(key) tells you WHICH
 * Value (almost always a Local) to rewrite the load to, not just THAT it's
 * redundant.
 *
 * Every lookup goes through ONE canonicalization step -- substitute the
 * base through valueOf if it's itself a known expression, THEN look the
 * resulting key up in valueSource. There is no separate "raw" key space, so
 * $r17 = a.f1 / $r19 = a.f1 always land on the exact same key regardless of
 * which concrete local produced it.
 */
public class RedundantFieldEliminationAnalysisV3
        extends ForwardFlowAnalysis<Unit, RedundantFieldEliminationAnalysisV3.FlowFact> {

    /** A symbolic field-access expression: symbolicBase.field. */
    public static final class FieldRefKey {
        final Object symbolicBase; // Value or FieldRefKey
        final Local concreteBase;  // the real Local used as base in the Jimple ref (for alias checks only)
        final SootField field;

        FieldRefKey(Object symbolicBase, Local concreteBase, SootField field) {
            this.symbolicBase = symbolicBase;
            this.concreteBase = concreteBase;
            this.field = field;
        }

        private static boolean baseEquiv(Object a, Object b) {
            if (a instanceof FieldRefKey && b instanceof FieldRefKey) return a.equals(b);
            if (a instanceof Value && b instanceof Value) return ((Value) a).equivTo(b);
            return false;
        }

        private static int baseHash(Object a) {
            return (a instanceof FieldRefKey) ? a.hashCode() : ((Value) a).equivHashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FieldRefKey)) return false;
            FieldRefKey other = (FieldRefKey) o;
            return baseEquiv(symbolicBase, other.symbolicBase) && field.equals(other.field);
        }

        @Override
        public int hashCode() {
            return 31 * baseHash(symbolicBase) + field.hashCode();
        }

        @Override
        public String toString() {
            return symbolicBase + "." + field.getSignature();
        }
    }

    public static final class FlowFact {
        final Map<Local, FieldRefKey> valueOf;
        final Map<FieldRefKey, Value> valueSource;

        FlowFact(Map<Local, FieldRefKey> valueOf, Map<FieldRefKey, Value> valueSource) {
            this.valueOf = valueOf;
            this.valueSource = valueSource;
        }

        static FlowFact bottom() {
            return new FlowFact(new HashMap<>(), new HashMap<>());
        }
    }

    private final PointsToAnalysis pta; // null -> fully conservative aliasing
    private final Map<Local, PointsToSet> ptsCache = new HashMap<>();

    public RedundantFieldEliminationAnalysisV3(DirectedGraph<Unit> dg) {
        this(dg, null);
    }

    public RedundantFieldEliminationAnalysisV3(DirectedGraph<Unit> dg, PointsToAnalysis pta) {
        super(dg);
        this.pta = pta;
        doAnalysis();
    }

    // ---- aliasing --------------------------------------------------------

    private boolean mayAlias(Local a, Local b) {
        if (a.equivTo(b)) return true;
        if (pta == null) return true;
        PointsToSet pa = ptsCache.computeIfAbsent(a, pta::reachingObjects);
        PointsToSet pb = ptsCache.computeIfAbsent(b, pta::reachingObjects);
        return pa.hasNonEmptyIntersection(pb);
    }

    // ---- recursive dependency checks used for killing ---------------------

    private boolean dependsOnLocal(FieldRefKey k, Local local) {
        if (k.concreteBase.equivTo(local)) return true;
        return (k.symbolicBase instanceof FieldRefKey)
                && dependsOnLocal((FieldRefKey) k.symbolicBase, local);
    }

    private boolean dependsOnFieldWrite(FieldRefKey k, SootField writtenField, Local writeBase) {
        if (k.field.equals(writtenField) && mayAlias(k.concreteBase, writeBase)) return true;
        return (k.symbolicBase instanceof FieldRefKey)
                && dependsOnFieldWrite((FieldRefKey) k.symbolicBase, writtenField, writeBase);
    }

    // ---- flow analysis plumbing -------------------------------------------

    @Override
    protected FlowFact newInitialFlow() {
        return FlowFact.bottom(); // sound; may cost extra iterations on loop back-edges
    }

    @Override
    protected FlowFact entryInitialFlow() {
        return FlowFact.bottom();
    }

    @Override
    protected void merge(FlowFact in1, FlowFact in2, FlowFact out) {
        out.valueOf.clear();
        for (Map.Entry<Local, FieldRefKey> e : in1.valueOf.entrySet()) {
            FieldRefKey v2 = in2.valueOf.get(e.getKey());
            if (e.getValue().equals(v2)) {
                out.valueOf.put(e.getKey(), e.getValue());
            }
        }

        out.valueSource.clear();
        for (Map.Entry<FieldRefKey, Value> e : in1.valueSource.entrySet()) {
            Value v2 = in2.valueSource.get(e.getKey());
            if (v2 != null && e.getValue().equivTo(v2)) {
                out.valueSource.put(e.getKey(), e.getValue());
            }
        }
    }

    @Override
    protected void copy(FlowFact source, FlowFact dest) {
        dest.valueOf.clear();
        dest.valueOf.putAll(source.valueOf);
        dest.valueSource.clear();
        dest.valueSource.putAll(source.valueSource);
    }

    @Override
    protected void flowThrough(FlowFact in, Unit u, FlowFact out) {
        copy(in, out);

        if (u.containsInvokeExpr()) {
            // Conservative: a call can write arbitrary fields through
            // arbitrary aliases; we lose all numbering across it.
            out.valueOf.clear();
            out.valueSource.clear();
            return;
        }

        if (!(u instanceof AssignStmt)) {
            return; // nothing this analysis cares about (branches, returns, ...)
        }

        AssignStmt as = (AssignStmt) u;
        Value lhs = as.getLeftOp();
        Value rhs = as.getRightOp();

        if (lhs instanceof Local && rhs instanceof InstanceFieldRef) {
            handleFieldLoad(in, out, (Local) lhs, (InstanceFieldRef) rhs);
        } else if (lhs instanceof InstanceFieldRef) {
            handleFieldStore(in, out, (InstanceFieldRef) lhs, rhs);
        } else if (lhs instanceof Local) {
            handleLocalRedefinition(out, (Local) lhs);
        }
    }

    /** l = base.field */
    private void handleFieldLoad(FlowFact in, FlowFact out, Local l, InstanceFieldRef ref) {
        Local base = (Local) ref.getBase();
        Object canonicalBase = in.valueOf.getOrDefault(base, base); // the ONE canonicalization step
        FieldRefKey key = new FieldRefKey(canonicalBase, base, ref.getField());

        if (!in.valueSource.containsKey(key)) {
            // First time we've seen this value: l becomes its home.
            out.valueSource.put(key, l);
        }
        // else: already available -- leave the existing representative alone,
        // per your rule ("map[b] = map[a.f]"); l is just a new alias for it.

        // Either way, l now denotes this symbolic value going forward.
        out.valueOf.put(l, key);
    }

    /** base.field = rhs */
    private void handleFieldStore(FlowFact in, FlowFact out, InstanceFieldRef ref, Value rhs) {
        Local writeBase = (Local) ref.getBase();
        SootField field = ref.getField();

        // Kill: anything -- at any nesting level -- that reads this field off
        // a base that may alias writeBase.
        out.valueSource.keySet().removeIf(k -> dependsOnFieldWrite(k, field, writeBase));
        out.valueOf.entrySet().removeIf(e -> dependsOnFieldWrite(e.getValue(), field, writeBase));

        // Gen: base.field is now definitively rhs.
        Object canonicalBase = in.valueOf.getOrDefault(writeBase, writeBase);
        FieldRefKey key = new FieldRefKey(canonicalBase, writeBase, field);
        out.valueSource.put(key, rhs); // unconditional overwrite: this is a fresh, authoritative fact
    }

    /** l = <anything not a field ref> */
    private void handleLocalRedefinition(FlowFact out, Local l) {
        out.valueOf.remove(l);
        out.valueOf.entrySet().removeIf(e -> dependsOnLocal(e.getValue(), l));
        out.valueSource.keySet().removeIf(k -> dependsOnLocal(k, l));
        // l may itself have been serving as some field's "living" value --
        // that fact dies too, since l no longer holds that value.
        out.valueSource.entrySet().removeIf(
                e -> (e.getValue() instanceof Local) && ((Local) e.getValue()).equivTo(l));
    }

    // ---- queries -----------------------------------------------------------

    public boolean isRedundantLoad(Unit u, InstanceFieldRef ref) {
        FlowFact in = getFlowBefore(u);
        Local base = (Local) ref.getBase();
        Object canonicalBase = in.valueOf.getOrDefault(base, base);
        return in.valueSource.containsKey(new FieldRefKey(canonicalBase, base, ref.getField()));
    }

    /** The Value this load could be rewritten to reuse, or null if not redundant. */
    public Value getRewriteTarget(Unit u, InstanceFieldRef ref) {
        FlowFact in = getFlowBefore(u);
        Local base = (Local) ref.getBase();
        Object canonicalBase = in.valueOf.getOrDefault(base, base);
        return in.valueSource.get(new FieldRefKey(canonicalBase, base, ref.getField()));
    }
}