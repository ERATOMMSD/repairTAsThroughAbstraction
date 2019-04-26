package repairta.process;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import parser.TAParserFromImitator;
import repairta.ClassifyTests;
import repairta.ConformanceChecker;
import ta.TA;

public class ConformanceCheckerTest {

	@org.junit.Test
	public void checkConformance() throws Exception {
//		checkConformance(Benchmark.COFFEE, "coin 0.0 t 5.0 tea 12.0 coin 13.0");
//		checkConformance(Benchmark.COFFEE, "coin 0.0 c 2.0 coffee 9.0");
//		checkConformance(Benchmark.COFFEE, "coin 0.0 t 2.0 tea 10.0 coin 12.0");
//		checkConformance(Benchmark.COFFEE, "coin 2.0 t 2.0 tea 9.0 coin 9.0");
	}
	
	public void checkConformance(Benchmark b, String... traces) throws Exception {
		RepairTimedAutomata.createFolderIfNotExists("files/temp");
		Map<String, Double> paramValues=RepairTimedAutomata.getParameterAssignmentsFromPTA(b.ptaPath);
		String prevTaPath = "files/temp/"+b.modelName+"Initial.imi";
		RepairTimedAutomata.printToFile(prevTaPath, RepairTimedAutomata.instantiateTA(b.ptaPath, paramValues));
		//String prevTaPathMomut = "files/temp/"+b.modelName+"Initial.xml";
		//RepairTimedAutomata.printToFile(prevTaPathMomut, RepairTimedAutomata.instantiateTAMomut(b.ptaPathMomut, paramValues));
		String taoPath = "files/temp/"+b.modelName+"Oracle.imi";
		RepairTimedAutomata.printToFile(taoPath, RepairTimedAutomata.instantiateTA(b.ptaPath, RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(b.ptaPath)));
		TA taoParsed = TAParserFromImitator.instance.loadModel(RepairTimedAutomata.instantiateTA(b.ptaPath, RepairTimedAutomata.getOracleParameterAssignmentsFromPTA(b.ptaPath)));
		for (String trace : traces) {
			checkConformance(taoParsed, taoPath, trace);
		}
	}
	
	public void checkConformance(TA ta, String ptaPath, String timedTrace) throws IOException, InterruptedException {
		System.out.println("TA: "+ta);
		boolean myTa = ConformanceChecker.isTraceAllowed(ta, timedTrace);
		boolean imitatorTa = ClassifyTests.getImitatorCheck(ptaPath, timedTrace);
		System.out.println(imitatorTa+" "+timedTrace);
		assertEquals(imitatorTa, myTa);
	}
}
