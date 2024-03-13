package uMAF;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class Branching extends IloCplex.Goal {
    IloNumVar[] _vars;

    public Branching(IloNumVar[] vars) {
            _vars = vars;
        }

    // Branch on var with largest objective coefficient
    // among those with largest infeasibility
    public IloCplex.Goal execute(IloCplex cplex) throws IloException {
        double[] x   = getValues  (_vars);
        double[] obj = getObjCoefs(_vars);
        IloCplex.IntegerFeasibilityStatus[] feas = getFeasibilities(_vars);

        double maxinf = 0.0;
        double maxobj = 0.0;
        int    bestj  = -1;
        int    cols   = _vars.length;
        for (int j = 0; j < cols; ++j) {
            if ( feas[j].equals(IloCplex.IntegerFeasibilityStatus.Infeasible) ) {
                double xj_inf = x[j] - Math.floor(x[j]);
                if ( xj_inf > 0.5 )
                    xj_inf = 1.0 - xj_inf;
                if ( xj_inf >= maxinf                               &&
                        (xj_inf > maxinf || Math.abs(obj[j]) >= maxobj)  ) {
                    bestj  = j;
                    maxinf = xj_inf;
                    maxobj = Math.abs(obj[j]);
                }
            }
        }

        if ( bestj >= 0 ) {
            return cplex.and(
                    cplex.or(cplex.geGoal(_vars[bestj], Math.floor(x[bestj])+1),
                            cplex.leGoal(_vars[bestj], Math.floor(x[bestj]))),
                    this);
        }
        else
            return null;
    }
}

