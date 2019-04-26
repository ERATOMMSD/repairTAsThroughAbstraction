package parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

import atgt.yices2.generator.ExprToYicesPtr;
import repairta.Combination;
import repairta.Utils;
import repairta.process.RepairTimedAutomata;
import tgtlib.definitions.expression.Expression;
import tgtlib.definitions.expression.IdExpression;
import tgtlib.definitions.expression.parser.ExpressionParser;
import tgtlib.definitions.expression.parser.ParseException;
import tgtlib.definitions.expression.type.EnumConstCreator;
import tgtlib.definitions.expression.visitors.IDExprCollector;
import yices2.Yices2Library;

public class ParameterAssignmentBySMT {
	public static Logger logger = Logger.getLogger("ParameterAssignmentBySMT");
	// DO NOT USE THE INSTANCE
	private static Yices2Library lib = null;// (Yices2Library) Yices2Library.INSTANCE;
	public static boolean USE_INTEGER = false;

	static {
		System.setProperty("jna.library.path", ".");
		lib = (Yices2Library) Native.loadLibrary("Yices2", Yices2Library.class);
	}

	public static void main(String[] args) throws ParseException {
		getParameterValues("p1>1.1 and p1<1.3", null);
	}

	public static Map<String, Double> getParameterValues(String constraints, Map<String, Double> previousValues) throws ParseException {
		return getParameterValues(Collections.singleton(constraints), previousValues);
	}
	
	/** The "intelligent" method */
	public static Map<String, Double> getParameterValues(Collection<String> constraints, Map<String, Double> previousValues) throws ParseException {
		List<String> paramAssignments = Utils.printAssignmentAsConstraintList(previousValues);
		for (int fixedParams = paramAssignments.size(); fixedParams >= 0; fixedParams--) {
			Combination c = new Combination(paramAssignments.size(), fixedParams);
			for (boolean[] selectedParams : c) {
				List<String> tempConstraints = new ArrayList<>(constraints);
				for (int i=0; i<selectedParams.length; i++) {
					if (selectedParams[i]) {
						tempConstraints.add(paramAssignments.get(i));
					}
				}
				Map<String, Double> tempValues = new HashMap<>(previousValues);
				boolean sat = isSAT(tempConstraints, tempValues, true);
				if (sat) return tempValues;
			}
		}
		System.err.println("getParameterValuesIntelligent: UNSAT!!");
		return null;
	}

	/** Obs! It updates the previous values, and returns if it was SAT or not */
	public static boolean isSAT(Collection<String> constraints, Map<String, Double> previousValues, boolean overwriteSolution) throws ParseException {
		lib.yices_init();
		//logger.log(Level.INFO, "Constraint before parsing by ATGT " + constraints);
		List<Expression> exprs = new ArrayList<>();
		Set<IdExpression> ids = new HashSet<>();
		EnumConstCreator ecc = new EnumConstCreator();
		for (String c : constraints) {
			try {
				Expression e = ExpressionParser.parse(c, ecc);
				exprs.add(e);
				//logger.log(Level.INFO, "Constraint " + e.toString());
				ids.addAll(e.accept(IDExprCollector.instance));
			} catch (Exception ex) { // 2019-03-28 MR: added for debuggin purpose
				System.out.println("Trying to parse: "+c);
				System.out.println("in... "+constraints);
				ex.printStackTrace();
				throw ex;
			}
		}

		Map<IdExpression, Integer> idYices = new HashMap<>();
		int type = USE_INTEGER ? lib.yices_int_type() : lib.yices_real_type();
		for (IdExpression id : ids) {
			String idStr = id.getIdString();
			//logger.log(Level.INFO, "idStr: " + idStr);
			char c = idStr.toCharArray()[0];
			if (!(c >= 48 && c <= 57)) {
				int x = lib.yices_new_uninterpreted_term(type);
				lib.yices_set_term_name(x, idStr);
				// idYices.put(id, counter++);
				idYices.put(id, x);
			} else {
				idYices.put(id, USE_INTEGER ? lib.yices_int32((int)Double.parseDouble(id.getIdString())) // MR 28.3.2019: I don't parse it as Integer, because sometimes it gives error, since there is 200.0
						: lib.yices_parse_float(idStr));
			}
		}

		List<Integer> ptrs = new ArrayList<>();
		for (Expression e : exprs) {
			Integer ptr = e.accept(new ExprToYicesPtr(lib, idYices));
			ptrs.add(ptr);
		}

		Map<String, Double> model = solve(lib, ptrs, idYices);
		if (model==null) return false; // UNSAT!!
		//if (previousValues != null && previousValues.size() > 0) {
		if (overwriteSolution) {
			if (previousValues==null) previousValues = new HashMap<>(); // very strange case!!
			previousValues.putAll(model);
		}
//			Map<String, Double> res = new HashMap<>(previousValues);
//			res.putAll(model);
			return true;
		//}
		//return model;
	}

	/*private static Map<String, Double> solve(Yices2Library lib, Integer ptr, Map<IdExpression, Integer> idYices) {
		return solve(lib, Collections.singleton(ptr), idYices);
	}*/

	private static Map<String, Double> solve(Yices2Library lib, Collection<Integer> ptrs,
			Map<IdExpression, Integer> idYices) {
		PointerByReference config = lib.yices_new_config();
		lib.yices_default_config_for_logic(config, "AUTO");
		PointerByReference ctx = lib.yices_new_context(config);
		for (Integer ptr : ptrs) {
			int code = lib.yices_assert_formula(ctx, ptr);
			if (code < 0) {
				new Error("error " + lib.yices_error_code());
			}
		}
		int result = lib.yices_check_context(ctx, null);

		Map<String, Double> assign = new HashMap<>();
		switch (result) {
		case Yices2Library.smt_status.STATUS_SAT:
			//logger.log(Level.INFO, "SAT");
			PointerByReference model = lib.yices_get_model(ctx, 1);
			String modelStr = lib.yices_model_to_string(model, 10, 10, 10).getString(0).replaceAll("\n", "")
					.replaceAll("\\) +\\(", ")(");
			//logger.log(Level.INFO, modelStr);
			while (modelStr.contains("  "))
				modelStr = modelStr.replace("  ", " "); // Marco: important
			Pattern pattern = Pattern
					.compile("\\(= ([a-zA-Z0-9]+) (([1-9][0-9]*(\\.[0-9]+)?)|([1-9][0-9]*/[1-9][0-9]*))\\)");
			Matcher matcher = pattern.matcher(modelStr);
			while (matcher.find()) {
				String var = matcher.group(1);
				String value = matcher.group(2);
				Double valueDouble = null;
				if (value.contains("/")) {
					String[] s = value.split("/");
					valueDouble = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);
				} else {
					valueDouble = Double.parseDouble(value);
				}
				assign.put(var, valueDouble);
			}
			break;
		case Yices2Library.smt_status.STATUS_UNSAT:
			if (RepairTimedAutomata.LOG_PARAMETER_ASSIGNMENT) logger.log(Level.INFO, "UNSAT");
			return null;
		case Yices2Library.smt_status.STATUS_UNKNOWN:
			logger.log(Level.INFO, "UNKNOWN");
			break;
		default:
			logger.log(Level.INFO, "Error in check_context");
			break;
		}
		return assign;
	}

}
