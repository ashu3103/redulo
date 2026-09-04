import soot.Value;
import soot.SootField;
import java.util.HashMap;
import java.util.Map;

public class FieldRefKey {
    final Value base;
    final SootField field;

    protected final Map<String, String> localValueToLocalMap;

    FieldRefKey(Value base, SootField field) {
        this.base = base;
        this.field = field;
        this.localValueToLocalMap = new HashMap<>(1 * 2, 0.7f);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FieldRefKey)) {
            return false;
        }
        FieldRefKey other = (FieldRefKey) o;
        return base.equivTo(other.base) && field.equals(other.field);
    }

    @Override
    public int hashCode() {
        // We can't derive a hash consistent with equivTo() for an
        // arbitrary Value, so we only hash on the field. This is still
        // correct (equal objects hash equal), just coarser-grained.
        return 31 * field.hashCode();
    }

    @Override
    public String toString() {
        return base + "." + field.getName();
    }

    public String getLocalFromValue(String val) {
        return this.localValueToLocalMap.get(val);
    }

    public String setLocalAndValue(String l, String v) {
        return this.localValueToLocalMap.put(l, v);
    }

    public String toStringBase() {
        return base.toString();
    }

    public String toStringSignature() {
        return field.getSignature();
    }
}