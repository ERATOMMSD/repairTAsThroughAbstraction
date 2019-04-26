package repairta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import parser.TAParserFromImitator;
import ta.TA;
import ta.Trace;
import ta.Transition;

public class ConformanceGenerator {
	public static Logger logger = Logger.getLogger("ConformanceGenerator");
	
	public static void main(String[] args) throws Exception {
		TA ta = TAParserFromImitator.instance.loadModel(new File("files/ta.imi"));
		List<Trace> untimedTraces = ta.getUntimedTracesUpToLength(3);
		System.out.println(untimedTraces);
		List<Trace> timedTraces = generateTimedTraces(untimedTraces, 0.5, 0.0, 5.0);
		System.out.println("There are "+timedTraces.size()+" timed traces: "+timedTraces);
		System.out.println("Conformance: "+computeConformance("files/ta.imi", "files/tao.imi", timedTraces));
	}
	
	public static double computeConformance(String taPath, String taoPath, double precision, double minTime, double maxTime) throws Exception {
		TA ta = TAParserFromImitator.instance.loadModel(new File(taPath));
		List<Trace> timedTraces = generateTimedTraces(ta, 0.5, 0.0, 5.0, 3);
		return computeConformance(taPath, taoPath, timedTraces);
	}
	
	public static double computeConformance(String taPath, String taoPath, List<Trace> timedTraces) throws IOException, InterruptedException {
		int count = 0;
		for (Trace trace : timedTraces) {
			if (ClassifyTests.getConformance(taPath, taoPath, trace.toString())<0) count++;
		}
		return ((double)count/(double)timedTraces.size());
	}
	
	/**TODO use iterator
	 * @return all the timed traces, separated by the precision, ranging from minTime to maxTime,
	 * containing exactly the specified number of steps
	 * NB! It can be very expensive
	 */
	public static List<Trace> generateTimedTraces(TA ta, double precision, double minTime, double maxTime, int maxSteps) {
		List<Trace> untimedTraces = ta.getUntimedTracesUpToLength(maxSteps);
		return generateTimedTraces(untimedTraces, precision, minTime, maxTime);
	}
	
	public static List<Trace> generateTimedTraces(List<Trace> untimedTraces, double precision, double minTime, double maxTime) {
		List<Trace> timedTraces = new ArrayList<>();
		for (Trace trace : untimedTraces) {
			timedTraces.addAll(generateTimedTraces(trace, precision, minTime, maxTime));
		}
		return timedTraces;
	}
	
	public static List<Trace> generateTimedTraces(Trace trace, double precision, double minTime, double maxTime) {
		List<Trace> allTraces = generatePartialTraces(null, trace.get(0), precision, minTime, maxTime);
		List<Trace> prevTraces = new ArrayList<>();
		for (int i=1; i<trace.size(); i++) {
			prevTraces = allTraces;
			allTraces = new ArrayList<>();
			Transition transition = trace.get(i);
			for (Trace t : prevTraces) {
				List<Trace> partialTraces = generatePartialTraces(t, transition, precision, t.times.get(t.times.size()-1), maxTime);
				allTraces.addAll(partialTraces);
			}
		}
		return allTraces;
	}
	
	public static List<Trace> generatePartialTraces(Trace traceSoFar, Transition transition, double precision, double minTime, double maxTime) {
		List<Trace> traces = new ArrayList<>();
		for (double time = minTime; time <=maxTime; time+=precision) {
			if (traceSoFar==null) traces.add(new Trace(transition, time));
			else {
				Trace t = new Trace(traceSoFar);
				t.add(transition, time);
				traces.add(t);
			}
		}
		return traces;
	}
}
