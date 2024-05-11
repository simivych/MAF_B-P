package uMAF1.colgen;

import ilog.concert.IloNumVar;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;
import uMAF1.model.MAF;

import java.util.Map;


public final class MAFData extends MasterData<MAF, Leafset, MAST, IloNumVar> {

    public MAFData(Map<MAST, OrderedBiMap<Leafset, IloNumVar>> varMap) {
        super(varMap);
    }
}
