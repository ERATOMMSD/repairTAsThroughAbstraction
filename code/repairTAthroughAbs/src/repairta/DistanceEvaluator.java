package repairta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.ParameterAssignmentBySMT;
import tgtlib.definitions.expression.parser.ParseException;

/**
 * A class to evaluate the solution quality
 * 
 * Evaluates the distance between two TAs
 * and also a PTA (the inferred one) from a TA (representing the oracle), 
 * by computing the average distance of some of its TAs
 * @author marcoradavelli
 *
 */
public class DistanceEvaluator {
	
	public static DistanceEvaluator instance = new DistanceEvaluator();
	protected DistanceEvaluator() {}
	
	public static final int MAX_MODELS_FOR_AVG = 100;
	
	/**
	 * 
	 * @param ta the assignment to the parameters defining the first TA
	 * @param ta2 the assignment to the parameters defining the second TA
	 * @return the euclidean distance between the values to the various parameters
	 */
	public double euclideanDistance(Map<String,Double> ta, Map<String,Double> ta2) {
		double distance =0;
		for (String param : ta.keySet()) {
			distance += Math.abs(ta.get(param)-ta2.get(param));
		}
		return distance;
	}
	
	/**
	 * 
	 * @param ta the oracle ta
	 * @param constraints the found constraints among parameters (representing a PTA)
	 * @param previousValues the original ta (the default assignment for "don't care" values)
	 * @return
	 * @throws ParseException in case the constraints cannot be parsed correctly
	 */
	public List<Double> euclideanDistance(Map<String,Double> ta, Collection<String> constraints, Map<String,Double> previousValues) throws ParseException {
		boolean prev = ParameterAssignmentBySMT.USE_INTEGER;
		ParameterAssignmentBySMT.USE_INTEGER=true;
		//double sum = 0.0;
		Set<Map<String,Double>> foundTAs = new HashSet<>();
		List<Double> res = new ArrayList<>();
		for (int i=0; i < MAX_MODELS_FOR_AVG; i++) {
			Map<String,Double> foundTA = new HashMap<>(previousValues);
			boolean isSAT = ParameterAssignmentBySMT.isSAT(constraints, foundTA, true);
			if (!isSAT || foundTAs.contains(foundTA)) {
				System.out.println("UNSAT: Found TA");
				break;
			}
			constraints.add("not ("+Utils.printAssignmentAsConstraint(foundTA)+")");
			foundTAs.add(foundTA);
			double dist = euclideanDistance(foundTA, ta);
			res.add(dist);
		//	sum += dist;
		}
		ParameterAssignmentBySMT.USE_INTEGER=prev;
		//Map<String,Double> avgVar = new HashMap<>();
		//avgVar.put("avg", sum / (double) foundTAs.size());
		//avgVar.put("var", )
		return res;
	}
	
	public static Map<String,Double> getAvgVar(Collection<Double> values) {
		Map<String,Double> res = new HashMap<>();
		double sum = 0;
		for (double d : values) {
			sum += d;
		}
		double avg = sum / values.size();
		sum = 0;
		for (double d : values) {
			sum += (d-avg)*(d-avg);
		}
		res.put("avg", avg);
		res.put("var", sum / values.size());
		return res;
	}
	
	/**
	 * 
	 * @param oracleTA
	 * @param constraints
	 * @param previousValues
	 * @return if the obtained constraints admit a solution
	 * @throws ParseException 
	 */
	public boolean containsSolution(Map<String,Double> oracleTA, Collection<String> constraints, Map<String,Double> previousValues) throws ParseException {
		boolean prev = ParameterAssignmentBySMT.USE_INTEGER;
		ParameterAssignmentBySMT.USE_INTEGER=false;
		List<String> tempConstraints = new ArrayList<>(constraints);
		tempConstraints.add(Utils.printAssignmentAsConstraint(oracleTA));
		//Map<String,Double> foundTA = ParameterAssignmentBySMT.getParameterValues(constraints, previousValues);
		boolean isSAT = ParameterAssignmentBySMT.isSAT(tempConstraints, previousValues, false);
		ParameterAssignmentBySMT.USE_INTEGER=prev;
		return isSAT;
	}
	
}
