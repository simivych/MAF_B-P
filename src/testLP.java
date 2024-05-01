import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class testLP {
    public void main(String[] a) {
        try(IloCplex cplex = new IloCplex()){

            cplex.importModel("lpex.lp");

            if ( cplex.solve() ) {

                IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
                double[] x     = cplex.getValues(lp);
                double[] pi    = cplex.getDuals(lp);
                IloNumVar[] vars = lp.getNumVars();
                IloRange[] rng = lp.getRanges();

                System.out.println("Solution status = " + cplex.getStatus());
                System.out.println("Solution value  = " + cplex.getObjValue());

                System.out.println();
                System.out.println("VALUES");
                for (int i = 0; i < x.length; ++i) {
                    System.out.println(STR."  Value \{vars[i].getName()} = \{x[i]}");
                }

                for (int i = 0; i < pi.length; ++i) {
                    System.out.println(STR."  Constraint \{rng[i].getName()} = \{pi[i]}");
                }
            }
        }
        catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
        }
    }
}
