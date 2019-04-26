package repairta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ta.Trace;
import tgtlib.definitions.expression.BinaryExpression;
import tgtlib.definitions.expression.Expression;
import tgtlib.definitions.expression.IdExpression;
import tgtlib.definitions.expression.parser.ExpressionParser;
import tgtlib.definitions.expression.parser.ParseException;
import tgtlib.definitions.expression.visitors.IDExprCollector;

/**
 * Utility functions
 * @author marcoradavelli
 *
 */
public class Utils {
	
	public static final String[] OPERATORS = new String[] {">=","<=","==","=",">","<"};
	
	public static final List<String> ASSIGNMENT_OPERATORS = Arrays.asList(new String[] {":=","==","="} );
	
	/**
	 * 
	 * @param minMax
	 * @param guard
	 * @param clocksSoFar
	 * @param forcedAssignmentsOnDestination
	 * @param absX the absolute time so far: it is the one used in the constraints, between min and max
	 */
	public static void updateMinMax(Map<String,Double> minMax, String guard, Map<String,Double> clocksSoFar, Map<String,Double> forcedAssignmentsOnDestination, double absX) {
		//System.out.println(guard);
		if (guard!=null && guard.length()>0) {
			// parse the guard
			String[] guards = guard.contains("&") ? guard.split("&") : new String[] {guard};
			for (int i=0; i<guards.length; i++) {
				String singleGuard = guards[i];
				if (singleGuard.equals("True")) continue;
				if (singleGuard.equals("False")) { 
					// everything is impossible!
					// should not happen
					System.err.println("Warning: everything is impossible in this guard: "+guard);
					minMax.put("max",-1.0);
				}
				String operator = containedString(singleGuard,OPERATORS);
				String[] cv = getClockAndValue(singleGuard,operator);
				if (forcedAssignmentsOnDestination!=null && forcedAssignmentsOnDestination.containsKey(cv[0])) {
					// it doesn't contribute to min max
					// (but we can assume that it normally is accepted by the final destination)
					continue;
				}
				//System.out.println("CVs: "+Arrays.toString(cv)+" "+operator); //+" "+t+" "+operator+" "+Arrays.toString(OPERATORS)+" "+k+" "+guard.contains("<=")+" "+guard.contains(OPERATORS[1]));
				double lastClockValue = clocksSoFar.get(cv[0]);
				double guardValue = Double.parseDouble(cv[1]);
				double delta = guardValue-lastClockValue + absX;
				boolean isMin = 
						((operator.startsWith("<")||operator.equals("==")||operator.equals("="))&&!singleGuard.trim().startsWith(cv[0])) 
						|| 
						((operator.startsWith(">")||operator.equals("==")||operator.equals("="))&&singleGuard.trim().startsWith(cv[0]));
				boolean isMax = !isMin || operator.equals("==") || operator.equals("=");
				if (isMin) {
					if (minMax.get("min")==null || delta>=minMax.get("min")) {
						minMax.put("min", delta);
						if (!minMax.containsKey("minIncluded") || minMax.get("minIncluded")==0.0) {
							minMax.put("minIncluded", operator.contains("=")?1.0:0.0);
						}
					}
				}
				if (isMax) {
					if (minMax.get("max")==null || delta<=minMax.get("max")) {
						minMax.put("max",delta);
						if (!minMax.containsKey("maxIncluded") || minMax.get("maxIncluded")==0.0) {
							minMax.put("maxIncluded", operator.contains("=")?1.0:0.0);
						}
					}
				}
			}
		}
	}
	
	
	public static void addIfPositive(Set<Double> set, double d) {
		if (d>=0) set.add(d);
		else set.add(0.0);
	}
	
	public static Trace trimToTimedTransitions(Trace trace) {
		Trace t = new Trace(trace);
		for (int i=t.times.size(); i<t.size(); ) t.remove(i);
		return t;
	}
	
	public static void updateClocks(Map<String,Double> clocksSoFarTemp, double delta, String assignment) {
		for (String clock : clocksSoFarTemp.keySet()) clocksSoFarTemp.put(clock, clocksSoFarTemp.get(clock)+delta);
		if (assignment!=null && assignment.length()>0) for (String operator : ASSIGNMENT_OPERATORS) {
			if (assignment.contains(operator)) {
				String[] cv = getClockAndValue(assignment, operator);
				clocksSoFarTemp.put(cv[0], Double.parseDouble(cv[1]));
				break;
			}
		}
	}
	
	public static Map<String,Double> getAssignments(String assignment) {
		Map<String,Double> assignments = new HashMap<>();
		if (assignment!=null && assignment.length()>0) for (String operator : ASSIGNMENT_OPERATORS) {
			if (assignment.contains(operator)) {
				String[] cv = getClockAndValue(assignment, operator);
				assignments.put(cv[0], Double.parseDouble(cv[1]));
				break;
			}
		}
		return assignments;
	}
	
	@org.junit.Test
	public void testAtgt() {
		withAtgt("e==(3)");
	}

	private static void withAtgt(String guard) {
		try {
			Expression e = ExpressionParser.parseAsNewBooleanExpression(guard);
			if(e instanceof BinaryExpression) {
				BinaryExpression be = (BinaryExpression)e;
				Set<IdExpression> varsLeft = be.getFirstOperand().accept(IDExprCollector.instance);
				Set<IdExpression> varsRight = be.getSecondOperand().accept(IDExprCollector.instance);
				for(IdExpression l: varsLeft) {
					System.out.println(l);
				}
				for(IdExpression r: varsRight) {
					System.out.println(r);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public static String[] getClockAndValue(String guard, String operator) {
		String[] cv = guard.split(operator);
		for (int i=0; i<cv.length; i++) cv[i]=cv[i].replace("(", "").replace(")","").trim();
		if (cv[0].matches("-?\\d+(\\.\\d+)?")) {
			String temp = cv[0];
			cv[0]=cv[1];
			cv[1]=temp;
		}
		return cv;
	}
	
	public static Set<String> convertTestsToString(Collection<Trace> tests) throws Exception {
		Set<String> res = new HashSet<>();
		for (Trace t : tests) {
			res.add(t.toString());
		}
		return res;
	}

	public static String containedString(String s, String... list) {
		for (String operator : list) if (s.contains(operator)) return operator;
		return "";
	}
	
	public static String printAssignmentAsConstraint(Map<String,Double> assignment) {
		String res = "";
		for (Entry<String, Double> a : assignment.entrySet()) {
			res += " and "+a.getKey()+"=="+a.getValue();
		}
		return res.substring(5);
	}
	
	public static List<String> printAssignmentAsConstraintList(Map<String,Double> assignment) {
		List<String> res = new ArrayList<>();
		for (Entry<String, Double> a : assignment.entrySet()) {
			res.add(a.getKey()+"=="+a.getValue());
		}
		return res;
	}
	
	public static List<Integer> makeListBetweenAndIncluded(int min, int max) {
		List<Integer> res = new ArrayList<>();
		for (int i=min; i<=max; i++) {
			res.add(i);
		}
		return res;
	}
}
