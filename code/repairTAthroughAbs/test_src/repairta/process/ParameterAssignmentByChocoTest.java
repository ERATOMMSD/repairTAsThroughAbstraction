package repairta.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.ParameterAssignmentByChoco;

public class ParameterAssignmentByChocoTest {
	@org.junit.Test
	public void test() throws Exception {
		List<String> constraints = new ArrayList<>();
		constraints.add("(p1>1 or (p1>=0 and p1<1)) and p1<7");
		Map<String,Double> previousValues = new HashMap<>();
		previousValues.put("p1", 8.0);
		print(constraints,previousValues);
	}
	
	@org.junit.Test
	public void test2() throws Exception {
		List<String> constraints = new ArrayList<>();
		constraints.add("(p1>1 or (p1>=0 and p1<1)) and p1<7");
		Map<String,Double> previousValues = new HashMap<>();
		previousValues.put("p1", 1.0);
		print(constraints,previousValues);
	}
	
	public static void print(List<String> constraints, Map<String,Double> prevValues) throws Exception {
		System.out.println("Test with "+prevValues+" - "+constraints);
		boolean exists = ParameterAssignmentByChoco.getParameterValues(constraints, prevValues, true);
		System.out.println(exists);
		System.out.println(prevValues);		
	}
}
