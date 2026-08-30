
import java.util.*;
import soot.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer {
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        UnitGraph ug = new BriefUnitGraph(body);
        RedundantFieldEliminationAnalysis analysis = new RedundantFieldEliminationAnalysis(ug);
    }
}

