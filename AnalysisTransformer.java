
import java.util.*;
import soot.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer {
    protected int precision;

    public AnalysisTransformer(int precision) {
        this.precision = precision;
    }

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        UnitGraph ug = new BriefUnitGraph(body);
        RedundantFieldEliminationAnalysis analysis = new RedundantFieldEliminationAnalysis(ug, this.precision);
    }
}

