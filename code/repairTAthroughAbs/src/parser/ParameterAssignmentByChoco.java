package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import tgtlib.definitions.expression.Expression;
import tgtlib.definitions.expression.IdExpression;
import tgtlib.definitions.expression.parser.ExpressionParser;
import tgtlib.definitions.expression.type.EnumConstCreator;
import tgtlib.definitions.expression.visitors.IDExprCollector;

public class ParameterAssignmentByChoco {
	
	static boolean USE_INTEGER=false;
	
	public static Map<String,Double> getParameterValues(Collection<String> constraints, Map<String, Double> previousValues) throws Exception {
		Map<String,Double> res = new HashMap<>(previousValues);
		getParameterValues(constraints, res, true);	
		return res;
	}
	
	public static boolean getParameterValues(Collection<String> constraints, Map<String, Double> previousValues, boolean overwriteSolution) throws Exception {
		// logger.log(Level.INFO, "Constraint before parsing by ATGT " + constraints);
		List<Expression> exprs = new ArrayList<>();
		Set<IdExpression> ids = new HashSet<>();
		EnumConstCreator ecc = new EnumConstCreator();
		for (String c : constraints) {
			try {
				Expression e = ExpressionParser.parse(c, ecc);
				exprs.add(e);
				// logger.log(Level.INFO, "Constraint " + e.toString());
				ids.addAll(e.accept(IDExprCollector.instance));
			} catch (Exception ex) { // 2019-03-28 MR: added for debuggin purpose
				System.out.println("Trying to parse: " + c);
				System.out.println("in... " + constraints);
				ex.printStackTrace();
				throw ex;
			}
		}
		System.out.println("Constraints: "+exprs);

		Model model = new Model("ParameterAssignmentByChoco");
		Map<IdExpression, IntVar> idYices = new HashMap<>();
		for (IdExpression id : ids) {
			String idStr = id.getIdString().trim();
			// logger.log(Level.INFO, "idStr: " + idStr);
			char c = idStr.charAt(0);
			if (!(c >= 48 && c <= 57)) {
				System.out.println(idStr);
				IntVar x = model.intVar(idStr,0,1000);
				idYices.put(id, x);
			} else {
				idYices.put(id, model.intVar((int) Double.parseDouble(id.getIdString())));
			}
		}

		List<ArExpression> ptrs = new ArrayList<>();
		for (Expression e : exprs) {
			System.out.println("Add expression: "+e);
			ArExpression ptr = e.accept(new ExprToChoco(model, idYices));
			System.out.println("Converted expression: "+ptr);
			ptrs.add(ptr);
		}

		Map<String, Double> res = solve(model, ptrs, idYices, previousValues);
		if (res == null) {
			return false; // UNSAT!!
		}
		if (overwriteSolution) {
			if (previousValues == null) {
				previousValues = new HashMap<>(); // very strange case!!
			}
			previousValues.putAll(res);
		}
		return true;
	}
	
	public static Map<String,Double> solve(Model model, List<ArExpression> constraints, Map<IdExpression,IntVar> idYices, Map<String,Double> previousValues) throws ContradictionException {
		// 2. Create variables
		int size = 0;
		for (IntVar v : idYices.values()) if (previousValues.containsKey(v.getName())) size++;		
		IntVar[] vars = new IntVar[size];
		int i=0;
		for (IntVar v : idYices.values()) if (previousValues.containsKey(v.getName())) vars[i++]=v;
		IntVar func = model.intVar("func", 0, 1000);
		// 3. Constraints
		for (ArExpression e : constraints) {
//			System.out.println("Expression: "+e);
			try {
				((ReExpression)e).post();
			} catch (Exception ex) {
				System.err.println("Skipping true constraint..."+e+" - "+ex.getLocalizedMessage());
			}
		}
		ArExpression sumDiff = null;
		System.out.println("idYices: "+idYices);
		for (IntVar v : vars) {
			System.out.println(v);
			if (v==null) {
				System.err.println("Shouldn't be null");
				continue;
			}
			ArExpression ar = v.sub((int)Math.round(previousValues.get(v.getName()))).abs();
			if (sumDiff==null) sumDiff = ar;
			else sumDiff = sumDiff.add(ar);
		}
		func.eq(sumDiff).post();
		// 4. Get the solver
		Solver solver = model.getSolver();
		model.setObjective(Model.MINIMIZE, func);
		// 5. Define the search strategy
		//IntVar[] vars = idYices.values().toArray(new IntVar[idYices.size()]);
		System.out.println(Arrays.toString(vars));
		//solver.setSearch(Search.inputOrderLBSearch(vars));
		//solver.setSearch(Search.minDomLBSearch(vars));
		solver.setSearch(Search.activityBasedSearch(vars));
		// 6. Launch the resolution process
		int step=0;
		Map<String,Double> res = new HashMap<>();
		while (solver.solve()) {
			step++;
//			System.out.println("p1: " + vars[0]);
			for (IntVar v : vars) res.put(v.getName(),(double)v.getValue());
		}
		System.out.println("Steps: "+step);
		System.out.println(res);
		return res;
	}
}
