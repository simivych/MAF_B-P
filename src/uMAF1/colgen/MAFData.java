package uMAF1.colgen;

import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;
import uMAF1.misc.IloNumVarArray;
import uMAF1.model.MAF;

import java.util.Map;


public final class MAFData extends MasterData<MAF, Leafset, MAST, IloNumVar> {
    public final IloCplex cplex;
    public IloObjective obj; //Objective function
    public IloRange[] rng; //Constraint
    public IloNumVarArray var;
    Duals duals;

    public MAFData(IloCplex cplex, IloObjective obj, IloRange[] rng, IloNumVarArray var, Map<MAST, OrderedBiMap<Leafset, IloNumVar>> varMap) {
        super(varMap);
        this.cplex = cplex;
        this.obj = obj;
        this.rng = rng;
        this.var = var;
    }
}
