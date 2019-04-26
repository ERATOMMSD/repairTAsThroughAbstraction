package ta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TA {
	
	/** automata names with respective initial location */
	public List<Location> initialLocations = new ArrayList<>();
	/** transitions exiting from that location */
	public LinkedHashMap<Location,List<Transition>> transitions = new LinkedHashMap<>();
	/** transition names exiting from that location (useful for cache) */
	public LinkedHashMap<Location,Set<String>> transitionNamesExitingFromLocation = new LinkedHashMap<>();
	/** associates location name, and location object */
	public Map<String,Location> locations = new HashMap<>();
	/** associates a transition name, with the list of the transition objects */
	public Map<String,List<Transition>> transitionNames = new HashMap<>();
	public Set<String> clockNames = new HashSet<>();
	/** automata for each the transition name is present: automata -> list of transitions */
	public Map<String,Set<String>> transitionsInAutomata = new LinkedHashMap<>();
	
	public void addClock(String clockName) {
		clockNames.add(clockName);
	}

	public void addLocation(String automatonName, Location location, boolean initial) {
		if (initial) {
			initialLocations.add(location);
		}
		if (!transitions.containsKey(location)) transitions.put(location, new ArrayList<>());
		if (!transitionNamesExitingFromLocation.containsKey(location)) transitionNamesExitingFromLocation.put(location, new HashSet<>());
		if (!locations.containsKey(location.name)) locations.put(location.name, location);
	}
	
	public void addTransition(Transition t) {
		List<Transition> tt = transitions.containsKey(t.origin) ? transitions.get(t.origin) : new ArrayList<>();
		tt.add(t);
		transitions.put(t.origin,tt);
		
		Set<String> tnames = transitionNamesExitingFromLocation.containsKey(t.origin) ? transitionNamesExitingFromLocation.get(t.origin) : new HashSet<>();
		tnames.add(t.name);
		transitionNamesExitingFromLocation.put(t.origin,tnames);

		List<Transition> ts = transitionNames.get(t.name);
		if (ts==null) ts = new ArrayList<>();
		ts.add(t);
		transitionNames.put(t.name, ts);
		
		String automataName = t.origin.automata;
		Set<String> transitions = transitionsInAutomata.get(t.name);
		if (transitions==null) transitions = new HashSet<>();
		transitions.add(t.name);
		transitionsInAutomata.put(automataName,transitions);
	}
	
	public List<Trace> getUntimedTracesUpToLength(int length) {
		List<Trace> traces = new ArrayList<>();
		for (int i=1; i<=length; i++) {
			traces.addAll(getUntimedTracesOfExactLength(i));
		}
		return traces;
	}
		
	public Set<String> getUniqueUntimedTraces(List<Trace> traces) {
		Set<String> in = new HashSet<>();
		for (Trace t : traces) in.add(t.toString());
		return in;
	}
		
	/** @return all the untimed possible traces up to the leaf or that length */
	public List<Trace> getUntimedTracesUpToLeafOrDepth(int depth) {
		return getUntimedTracesUpToLeafOrDepth(new Trace(), depth);
	}
	
	private List<Trace> getUntimedTracesUpToLeafOrDepth(Trace currentTrace, int depth) {
		List<Trace> traces = new ArrayList<>();
		if (currentTrace!=null && depth==currentTrace.size()) {
			traces.add(currentTrace);
			return traces;
		}
		Collection<Location> lastLocations = currentTrace.getFinalDestination()==null ? this.initialLocations : Arrays.asList(new Location[] {currentTrace.getFinalDestination()});
		for (Location lastLocation : lastLocations) {
			if (this.transitions.get(lastLocation)==null || this.transitions.get(lastLocation).size()==0) {
				traces.add(currentTrace);
			}
			else for (Transition t : this.transitions.get(lastLocation)) {
				Trace trace = new Trace(currentTrace);
				trace.add(t);
				traces.addAll(getUntimedTracesUpToLeafOrDepth(trace,depth));
			}
		}
		return traces;
	}
	
	
	/** @return all the untimed possible traces of that exact length */
	public List<Trace> getUntimedTracesOfExactLength(int length) {
		List<Trace> res = new ArrayList<>();
		for (int i=0; i<initialLocations.size(); i++) {
			res.addAll(getUntimedTracesOfExactLength(length, i));
		}
		return res;
	}
	
	/** @return all the untimed possible traces of that exact length */
	public List<Trace> getUntimedTracesOfExactLength(int length, int automatonIndex) {
		if (length<0) throw new RuntimeException("Length should be at least 0");
		if (length==0) {
			return new ArrayList<>();
		}
		if (length==1) return addOneStep(initialLocations.get(automatonIndex));
		else {
			List<Trace> currentTraces = getUntimedTracesOfExactLength(length-1);
			List<Trace> traces = new ArrayList<>();
			for (Trace trace : currentTraces) {
				traces.addAll(addOneStep(trace));
			}
			return traces;			
		}
	}
			
	public List<Trace> addOneStep(Location initialLocation) {
		List<Trace> traces = new ArrayList<Trace>();
		List<Transition> outerTransitions = transitions.get(initialLocation);
		for (Transition transition : outerTransitions) {
			Trace t = new Trace();
			t.add(transition);
			traces.add(t);
		}
		return traces;
	}
	
	public List<Trace> addOneStep(Trace trace) {
		List<Trace> traces = new ArrayList<Trace>();
		List<Transition> outerTransitions = transitions.get(trace.getFinalDestination());
		for (Transition transition : outerTransitions) {
			Trace t = new Trace(trace);
			t.add(transition);
			traces.add(t);
		}
		return traces;
	}
	
	@Override
	public String toString() {
		String locs = "";
		for (Location l : locations.values()) locs=locs+", "+l.toStringComplete();
		locs = "{"+locs.substring(1)+"}";
		return "TA InitialLocations: "+initialLocations
				+"\nClocks: "+clockNames
				+"\nLocations: "+locs
				+"\nTransitions: "+transitions;
	}
}
