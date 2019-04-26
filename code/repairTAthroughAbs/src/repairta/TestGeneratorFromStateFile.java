package repairta;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import parser.TraceParserFromStatespace;
import ta.TA;

public class TestGeneratorFromStateFile extends TestGeneratorJava {
	
	public static final TestGeneratorFromStateFile instance = new TestGeneratorFromStateFile();
	
	protected TestGeneratorFromStateFile() {
		mode = Mode.MINUS1_EQUAL_PLUS1_MIDDLE;
	}

	@Override
	public Set<String> generateTests(TA ta, int depth) throws Exception {
		return super.generateTests(ta, depth);
	}
	
	@Override
	public Set<String> generateTests(String taPath, String taPathMomut, String taPathTransformed, int step, Collection<String> previosGeneratedTests, int depth, boolean determinize, String[] automatonNames) throws Exception {
		System.out.println(taPathTransformed);
		TA ta = TraceParserFromStatespace.getTA(new File(taPathTransformed));
		return generateTests(ta, depth);
	};

}
