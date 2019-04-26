package repairta.process;

import java.lang.reflect.Array;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

public class ChocoMinimizeTest {

	@org.junit.Test
	public void test() throws ContradictionException {
		 tryChocoMin();
		//try2();
	}

	public static void try2() {
		int TIME_LIMIT = 60000;
		int[] p = new int[] { 1, 0 };
		Model model = new Model("my first problem");
		// decision variables x[i][j] = k means cell (i,j) takes color k
//        IntVar[][] x = model.intVarMatrix("x", n, m, 1, Math.min(n,m));
		// flat representation of x
		IntVar[] x = model.intVarArray(2, p); // ArrayUtils.flatten(x);
		IntVar[] diff = model.intVarArray(2, new int[] { 0, 0 }); // ArrayUtils.flatten(x);
		// objective variable
		IntVar objective = model.intVar(0);
		// minimize the objective variable
		model.setObjective(Model.MINIMIZE, objective);

		for (int i = 0; i < x.length; i++) {
			diff[i].eq(x[i].min(p[i]).abs()).post();
			//model.arithm(diff[i], "=", model.arithm(x[i], "-", p[i])).post();
		}
		model.sum(diff, "<=", objective).post();
		// objective function : number of colors that are used (colors are symmetrical)
		//model.sum(objective, x).post();

		// grid constraints
		model.or(model.arithm(x[0], ">", 1), model.and(model.arithm(x[0], ">", 0), model.arithm(x[0], "<", 1))).post();

		// tuning search strategy
		Solver s = model.getSolver();
		s.limitTime(TIME_LIMIT + "s");
		// use search strategy given in the minizinc model (first fail)
		//s.setSearch(Search.minDomLBSearch(x));
		// use activity-based search (classical black box search)
		 s.setSearch(Search.activityBasedSearch(concatenate(x, diff)));
//        return model;
		while (s.solve()){
		}
		for (int i = 0; i < x.length; i++) {
			System.out.println(x[i]);
			System.out.println(diff);
			System.out.println(objective);
		}
		
	}

	public static void tryChocoMin() throws ContradictionException {
		// 1. Create a Model
		Model model = new Model("my first problem");
		// 2. Create variables
		IntVar p1 = model.intVar("p1", 0, 1000);
		// p1.instantiateTo(1, Cause.Null);
		IntVar p2 = model.intVar("p2", 0, 1000);
		// p2.instantiateTo(2, Cause.Null);
		IntVar func = model.intVar("func", 0, 1000);
		// func.instantiateTo(1000, Cause.Null);
		// 3. Constraints
		p1.gt(1).or(p1.ge(0).and(p1.lt(1))).post();
		func.eq(p1.sub(10).abs()).post();
		// 4. Get the solver
		Solver solver = model.getSolver();
		model.setObjective(Model.MINIMIZE, func);
		// Solution s = solver.findOptimalSolution(func, false, (Criterion[]) null);
		// 5. Define the search strategy
		solver.setSearch(Search.inputOrderLBSearch(p1, p2));
		//solver.setSearch(Search.minDomLBSearch(p1,p2));
		//solver.setSearch(Search.activityBasedSearch(p1,p2));
		// 6. Launch the resolution process
		// solver.solve();
		// 7. Print search statistics
		// solver.printStatistics();
		while (solver.solve()) {
			System.out.println("p1: " + p1);
			System.out.println("p2: " + p2);
		}
		System.out.println("p1: " + p1);
		System.out.println("p2: " + p2);
		// solver.printStatistics();
		// Solution s = solver.findSolution((Criterion[])null);
		// System.out.println("p1: "+s.getIntVal(p1));
		// System.out.println("p2: "+s.getIntVal(p2));
	}
	
	/** From: https://stackoverflow.com/a/80503/5538923 */
	public static <T> T concatenate(T a, T b) {
	    if (!a.getClass().isArray() || !b.getClass().isArray()) {
	        throw new IllegalArgumentException();
	    }

	    Class<?> resCompType;
	    Class<?> aCompType = a.getClass().getComponentType();
	    Class<?> bCompType = b.getClass().getComponentType();

	    if (aCompType.isAssignableFrom(bCompType)) {
	        resCompType = aCompType;
	    } else if (bCompType.isAssignableFrom(aCompType)) {
	        resCompType = bCompType;
	    } else {
	        throw new IllegalArgumentException();
	    }

	    int aLen = Array.getLength(a);
	    int bLen = Array.getLength(b);

	    @SuppressWarnings("unchecked")
	    T result = (T) Array.newInstance(resCompType, aLen + bLen);
	    System.arraycopy(a, 0, result, 0, aLen);
	    System.arraycopy(b, 0, result, aLen, bLen);        

	    return result;
	}
}
