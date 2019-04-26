package repairta;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import ta.TA;

public class ClassifyTests {
	public static Logger logger = Logger.getLogger("ClassifyTests");
	public static boolean USE_ALSO_IMITATOR = false;

	public static Set<String> mp = new HashSet<>(), mnp = new HashSet<>();
	public static Set<String> mpNew, mnpNew;
	
	public static int classifyTests(TA taParsed, TA taoParsed, Collection<String> newTests, String taPath, String taoPath, boolean keepPreviousTests) throws IOException, InterruptedException {
		if (!keepPreviousTests) {
			mp = new HashSet<>();
			mnp = new HashSet<>();
		}
		mpNew = new HashSet<>();
		mnpNew = new HashSet<>();
		int unconformantCount=0;
		for (String trace : newTests) {
			// sometimes we don't need to call imitator, because we know already the answer
			boolean tao = false;
			String test = trace.toString();
			if (isTraceContainingAShorterTrace(test, mnp)) {
				tao=false;
				continue;
			} else if (isTraceContainedInALongerTrace(test, mp)) {
				//System.out.println("Shorter than a longer trace.");
				tao=true;
				continue;
			} else {
				tao = ConformanceChecker.isTraceAllowed(taoParsed, test);
				if (USE_ALSO_IMITATOR) {
					boolean tao2 = getImitatorCheck(taoPath, test);
					if (tao!=tao2) {
						System.err.println("Error tao! "+tao+" "+tao2+" "+test);
						tao=tao2;
					}
				}	
			}
			//System.out.println(tao+" "+test);
			//int count=0;
			if (tao) {
				//count=
				removeTraces(test, mp, true);
				//if (count>0) System.out.println("Removed shorter: "+count+" "+test);
				removeTraces(test, mpNew, true);
				mp.add(test);
				mpNew.add(test);
			} else {
				//count=
				removeTraces(test, mnp, false);
				//if (count>0) System.out.println("Removed longer: "+count+" "+test);
				removeTraces(test, mnpNew, false);				
				mnp.add(test);
				mnpNew.add(test);
			}
			
			if (!USE_ALSO_IMITATOR || unconformantCount==0) {
				boolean ta = ConformanceChecker.isTraceAllowed(taParsed, test);
				if (USE_ALSO_IMITATOR) {
					boolean ta2 = getImitatorCheck(taPath, test);
					if (ta!=ta2) {
						System.err.println("Error ta! "+ta+" "+ta2+" "+test);
						ta=ta2;
					}
				}
				//System.out.println(ta);
				
				if (ta!=tao) unconformantCount++;
					//if (tao && !ta) mp.add(line);
					//else if (!tao && ta) mnp.add(line);
				//}
				/*else {
					mpNew.remove(test);
					mnpNew.remove(test);
				}*/
			}	
		}
//		fin.close();
//		if (keepPreviousTests) Imitator.parseTraces(mp, mnp);
//		mp.addAll(mpNew);
//		mnp.addAll(mnpNew);
		PrintWriter fout = new PrintWriter(new FileWriter("files/temp/classifiedTests.tatrace"));
		if (mp!=null && mp.size()>0) {
			fout.println("mp:");
			for (String trace : mp) fout.println(trace);
			fout.println();
		}
		if (mnp!=null && mnp.size()>0) {
			fout.println("mnp:");
			for (String trace : mnp) fout.println(trace);
		}
		fout.close();
		return unconformantCount;
	}
	
	public static boolean isTraceContainedInALongerTrace(String trace, Collection<String> traces) {
		for (String t : traces) if (t.startsWith(trace)) return true;
		return false;
	}
	
	public static boolean isTraceContainingAShorterTrace(String trace, Collection<String> traces) {
		for (String t : traces) if (trace.startsWith(t)) return true;
		return false;
	}
	
	public static int removeTraces(String trace, Collection<String> traces, boolean shorter) {
		boolean removed=true;
		int count=0;
		while (removed) {
			removed=false;
			for (String t : traces) {
				if ((shorter && trace.startsWith(t)) || (!shorter && t.startsWith(trace))) {
					traces.remove(t);
					removed = true;
					count++;
					break;
				}
			}
		}
		return count;
	}
	
	@org.junit.Test
	public void testX() {
		List<String> l = new ArrayList<>(Arrays.asList(new String[] {"a 10 b 20","a 10", "a 10 a 12", "a 10 a 11 b 20"}));
		System.out.println(removeTraces("a 10 a 11 b 20", l, true));
		System.out.println(l);
	}
	
	/** @return >0 if conformant, <0 if non conformant
	 *  1: both false; 2: both true; -1: first false second true; -2: first true second false 
	 * @throws InterruptedException 
	 * @throws IOException */
	public static int getConformance(String taPath, String taoPath, String trace) throws IOException, InterruptedException {
		boolean ta = getImitatorCheck(taPath, trace);
		boolean tao = getImitatorCheck(taoPath, trace);
		if (ta && tao) return 2;
		if (!ta && !tao) return 1;
		if (!ta && tao) return -1;
		if (ta && !tao) return -2;
		return 0;
	}
	
	public static boolean getImitatorCheck(String ptaPath, String trace) throws IOException, InterruptedException {
		Imitator.mp = new HashSet<>();
		Imitator.mp.add(trace);
		Imitator.createImitatorInput(ptaPath, 0);
		String result = Imitator.getImitatorOutput();
		//logger.log(Level.INFO,ptaPath+" - "+trace + " - " + result);
		return result.contains("False"); // it is the negated value
	}
		
}
