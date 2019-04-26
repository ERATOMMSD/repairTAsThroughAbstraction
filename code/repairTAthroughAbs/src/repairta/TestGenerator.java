package repairta;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import parser.TAParserFromImitator;
import repairta.TestGeneratorJava.Mode;
import ta.TA;

/**
 * @author marcoradavelli
 * Test generator from untimed traces, entirely in Java
 */
public abstract class TestGenerator {
	
	public Mode mode;
	
	public abstract Set<String> generateTests(TA ta, int depth) throws Exception;
	
	public Set<String> generateTests(String taPath, String taPathMomut, String taPathTransformed, int step, Collection<String> previosGeneratedTests, int depth, boolean determinize, String[] automatonNames) throws Exception {
		TA ta = TAParserFromImitator.instance.loadModel(new File(taPath));
		return generateTests(ta, depth);
	};		
	
}
