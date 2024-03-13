package uMAF;

import java.util.Map;

public class LPResult{

    public Map<String, Double> duals;
    public double value;

    public LPResult(Map<String, Double> duals, double solution){
        this.duals = duals;
        this.value = solution;
    }
}