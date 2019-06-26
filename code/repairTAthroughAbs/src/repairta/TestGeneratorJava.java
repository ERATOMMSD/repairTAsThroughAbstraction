package repairta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ta.TA;
import ta.Trace;
import ta.Transition;

public class TestGeneratorJava extends TestGenerator {
	
	private static final Random RANDOM = new Random();
	public static final boolean PRINT = false;

	public enum Mode {
		MINUS1_EQUAL_PLUS1,
		MINUS1_EQUAL_PLUS1_MIDDLE,
		MINUS1_EQUAL_PLUS1_QUARTER,
		RANDOM,
		
		// useless
		MIN_VAL,
		MAX_VAL,
		MINUS1_PLUS1,
		MIN_MAX_MIDDLE,
		MIN_MAX_QUARTER,
		MINUS1_PLUS1_MIDDLE,
		MINUS1_PLUS1_QUARTER,
		RANDOM_2,
		RANDOM_3,
		MINUS2_EQUAL_PLUS2,
	};
	
	public static final TestGeneratorJava instance = new TestGeneratorJava();
	
	protected TestGeneratorJava() {
		mode = Mode.MINUS1_EQUAL_PLUS1;
	}
	
	@Override
	public Set<String> generateTests(TA ta, int depth) throws Exception {
		return Utils.convertTestsToString(generateTestsReturnTrace(ta, depth));
	}
	
	protected List<Trace> generateTestsReturnTrace(TA ta, int depth) {
		List<Trace> traces = new ArrayList<>(ta.getUntimedTracesUpToLeafOrDepth(depth));
		//System.out.println("Initial untimed traces: "+traces.size()+" - "+traces);
		
		List<Trace> res = new ArrayList<>();
		for (Trace trace : traces) {
			Trace timedTrace = new Trace(trace);
			Map<String,Double> clocks = new HashMap<>();
			for (String clock : ta.clockNames) clocks.put(clock, 0.0);
			List<Trace> generatedTraces = addOneStep(timedTrace, 0, clocks);
			res.addAll(generatedTraces);
		}
		return res;
	}
	
	protected List<Trace> addOneStep(Trace trace, int lengthSoFar, Map<String,Double> clocksSoFar) {
		List<Trace> res = new ArrayList<>();
		if (lengthSoFar==trace.size()) {
			return res;
		}
		Transition t = trace.get(lengthSoFar);
		double previousTime = (lengthSoFar==0 ? 0.0 : trace.times.get(trace.times.size()-1));
		
		// candidate times for the transition, as found in the invariant
		Set<Double> candidateTimes = new HashSet<>();
		
		Map<String,Double> minMax=new HashMap<>();

		Utils.updateMinMax(minMax, t.origin.invariant, clocksSoFar, null, previousTime);
		Utils.updateMinMax(minMax, t.guard, clocksSoFar, null, previousTime);
		Utils.updateMinMax(minMax, t.destination.invariant, clocksSoFar, Utils.getAssignments(t.assignment), previousTime);
		
		double min = minMax.get("min") == null ? 0.0 : minMax.get("min");
		if (min<=0 && (minMax.get("max")==null || minMax.get("max")>=0)) min=0; // min is guaranteed to be 0
		double max = minMax.get("max") == null ? -2.0 : minMax.get("max");
		
		if (PRINT) System.out.println(t+" Min: "+min+" max: "+max);
		
		if (mode == Mode.MINUS1_EQUAL_PLUS1 || mode == Mode.MINUS1_EQUAL_PLUS1_MIDDLE || mode == Mode.MINUS1_PLUS1 || mode == Mode.MINUS1_PLUS1_MIDDLE || mode == Mode.MINUS1_PLUS1_QUARTER || mode == Mode.MINUS1_EQUAL_PLUS1_QUARTER) {
			Utils.addIfPositive(candidateTimes, min-1);
			Utils.addIfPositive(candidateTimes, min+1);
			Utils.addIfPositive(candidateTimes, max-1);
			Utils.addIfPositive(candidateTimes, max+1);
		}
		if (mode == Mode.MINUS2_EQUAL_PLUS2) {
			Utils.addIfPositive(candidateTimes, min+2);
		}
		if (mode == Mode.MIN_VAL || mode == Mode.MIN_MAX_MIDDLE || mode == Mode.MIN_MAX_QUARTER || mode == Mode.MINUS1_EQUAL_PLUS1 || mode == Mode.MINUS1_EQUAL_PLUS1_MIDDLE || mode == Mode.MINUS1_EQUAL_PLUS1_QUARTER) {
			candidateTimes.add(min);
		}
		if (mode == Mode.MAX_VAL || mode == Mode.MIN_MAX_MIDDLE || mode == Mode.MIN_MAX_QUARTER || mode == Mode.MINUS1_EQUAL_PLUS1 || mode == Mode.MINUS1_EQUAL_PLUS1_MIDDLE || mode == Mode.MINUS1_EQUAL_PLUS1_QUARTER) {
			candidateTimes.add(max);
		}
		if (mode == Mode.MIN_MAX_MIDDLE || mode == Mode.MINUS1_EQUAL_PLUS1_MIDDLE || mode == Mode.MIN_MAX_QUARTER || mode == Mode.MINUS1_PLUS1_MIDDLE || mode == Mode.MINUS1_PLUS1_QUARTER || mode == Mode.MINUS1_EQUAL_PLUS1_QUARTER) {
			candidateTimes.add(min+ Math.round((max-min)/2.0));
		}
		if (mode == Mode.MIN_MAX_QUARTER || mode == Mode.MINUS1_PLUS1_QUARTER || mode == Mode.MINUS1_EQUAL_PLUS1_QUARTER) {
			candidateTimes.add(min+ Math.round((max-min)/4.0));
			candidateTimes.add(min+ Math.round(((max-min)/4.0)*3.0));			
		}
		if (mode == Mode.RANDOM || mode == Mode.RANDOM_2 || mode == Mode.RANDOM_3) {
			candidateTimes.addAll(getRandomValues(mode==Mode.RANDOM?1: (mode==Mode.RANDOM_2?2:3), (int)(min-1), (int)(max+2)));
		}
		if (PRINT) System.out.println("Test: "+t+" "+previousTime+" "+min+" "+max+" "+candidateTimes);
		for (double absTime : candidateTimes) {
			if (absTime<0) continue;
			Map<String,Double> clocksSoFarTemp = new HashMap<>(clocksSoFar);
			Utils.updateClocks(clocksSoFarTemp, absTime, t.assignment);
			Trace t2 = new Trace(trace);
			t2.times.add(previousTime+absTime);
			res.add(Utils.trimToTimedTransitions(t2)); // add the trace itself, up to this point
			res.addAll(addOneStep(t2, lengthSoFar+1, clocksSoFarTemp));
		}
		return res;
	}
	
	/**
	 * 
	 * @param n
	 * @param min
	 * @param max
	 * @return n random values between min and max, max excluded
	 */
	public static Set<Double> getRandomValues(int n, int min, int max) {
		if (min<0) min=0;
		if (max<min) max=min;
		System.out.println("Getting random values..."+n+" "+min+" "+max);
		int size= max - min;
		Set<Double> res = new HashSet<>();
		if (size<0) return res;
		if (size==0) {
			res.add((double)min);
			return res;
		}
		boolean[] b = new boolean[size];
		for (int i=0; i<b.length; i++) b[i]=false;
		int count = 0;
		while (count < n && count < size) {
			int i=0;
			while ( b[i=RANDOM.nextInt(size)] );
			b[i]=true;
			count++;
		}
		for (int i=0; i<b.length; i++) if (b[i]) res.add((double)min+i);
		System.out.println("Got "+res);
		return res;
	}

	
}
