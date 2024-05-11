package uMAF1.colgen;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;
import uMAF1.model.MAF;

public class MAST extends AbstractPricingProblem<MAF> {

	public MAST(MAF modelData, String name) {
        super(modelData, name);
    }

}