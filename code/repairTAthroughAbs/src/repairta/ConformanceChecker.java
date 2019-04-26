package repairta;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ta.Location;
import ta.TA;
import ta.Trace;
import ta.Transition;

/**
 * Checks if a trace is admitted by a certain timed automata.
 * 
 * @author marcoradavelli
 *
 */
public class ConformanceChecker {
	
	/**
	 * @return if the timed trace is allowed or not
	 */
	public static boolean isTraceAllowed(TA ta, String timedTrace) {
		//System.out.println("isTraceAllowed");
		//System.out.println("TA: "+ta);
		//System.out.println("Trace: "+trace);
		Map<String,Double> clocksSoFar = new HashMap<>();
		for (String clock : ta.clockNames) clocksSoFar.put(clock, 0.0);
		//ta.parseTimedTrace(timedTrace);
		return isTraceAllowed(ta, timedTrace, clocksSoFar, new Trace());
	}

	public static boolean admittedTransition(TA ta, Collection<Location> lastLocations, String transitionName) {
		for (Location l : lastLocations) {
			String automataName = l.automata;
			if (ta.transitionsInAutomata.get(automataName).contains(transitionName)) {
				if (!ta.transitionNamesExitingFromLocation.get(l).contains(transitionName)) {
					return false; // one automaton cannot proceed
				}
			}
		}
		return true; // all the automata that have that transition, can proceed from the current location
	}
	
	/** checks the current step
	 * 
	 * @param ta
	 * @param timedTrace
	 * @param clocksSoFar
	 * @param traceSoFar
	 * @return if from now on, there is at least one path for which the timedTrace is feasible
	 */
	protected static boolean isTraceAllowed(TA ta, String timedTrace, Map<String,Double> clocksSoFar, Trace traceSoFar) {
		String[] st = timedTrace.split(" ");
		String lastLocation = traceSoFar==null || traceSoFar.size()==0 ? ta.initialLocations.get(0).name : traceSoFar.get(traceSoFar.size()-1).destination.name;
		int i= traceSoFar==null || traceSoFar.size()==0 ? 0 : traceSoFar.size()*2;
		if (i==st.length) return true;
		String transitionName = st[i];
		List<Transition> possibleTransitions = ta.transitions.get(ta.locations.get(lastLocation));
		double previousTime = traceSoFar==null || traceSoFar.size()==0 ? 0.0 : traceSoFar.times.get(traceSoFar.times.size()-1);
		for (Transition tr : possibleTransitions) if (tr.name.equals(transitionName)) {
			Transition t = tr;
			
			double time = Double.parseDouble(st[i+1]);
			double delta = time - previousTime;
			
			// check origin invariants on previous clock values
			if (t==null || t.origin==null) {
				System.err.println("Null!!" + t+ " " + i);
			}
			//System.out.println("t: "+t);
			
			Map<String,Double> minMax=new HashMap<>();
			Utils.updateMinMax(minMax, t.origin.invariant, clocksSoFar, null, previousTime);
			//System.out.println("MinMax... "+minMax);
			Utils.updateMinMax(minMax, t.guard, clocksSoFar, null, previousTime);
			//System.out.println("MinMax... "+minMax);
			Utils.updateMinMax(minMax, t.destination.invariant, clocksSoFar, Utils.getAssignments(t.assignment), previousTime);
			//System.out.println("MinMax... "+t.destination.toStringComplete()+" "+Utils.getAssignments(t.assignment)+" "+minMax);
			if (evaluate(minMax,time)) {	
				Trace temp = new Trace(traceSoFar);
				temp.add(t, time);
				Map<String,Double> clocksSoFarTemp = new HashMap<>(clocksSoFar);
				Utils.updateClocks(clocksSoFarTemp, delta, t.assignment);
				boolean conformant = isTraceAllowed(ta, timedTrace, clocksSoFarTemp, temp);
				if (conformant) return true;
			}
		}
		// no paths are conformant
		return false;
	}
	
	public static boolean evaluate(Map<String,Double> minMax, double delta) {
		//System.out.println("Evaluate "+minMax+" "+delta);
		if (delta<0) return false; // in any case, not feasible for timed automata
		Double min = minMax.get("min");
		Double max = minMax.get("max");
		boolean minIncluded = minMax.get("minIncluded")!=null && minMax.get("minIncluded")>0;
		boolean maxIncluded = minMax.get("maxIncluded")!=null && minMax.get("maxIncluded")>0;
		if (min!=null && ( (minIncluded && delta<min) || (!minIncluded && delta<=min) )) return false;
		if (max!=null && ( (maxIncluded && delta>max) || (!maxIncluded && delta>=max) )) return false;
		return true;
	}
	
	public static boolean evaluate(double firstValue, String operator, double secondValue) {
		if ("==".equals(operator) || "=".equals(operator)) return firstValue==secondValue;
		if (">=".equals(operator)) return firstValue>=secondValue;
		if ("<=".equals(operator)) return firstValue<=secondValue;
		if (">".equals(operator)) return firstValue>secondValue;
		if ("<".equals(operator)) return firstValue<secondValue;
		return false;
	}
	
}
