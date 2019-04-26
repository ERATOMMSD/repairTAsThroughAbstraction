package repairta.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import parser.ParameterAssignmentBySMT;
import tgtlib.definitions.expression.parser.ParseException;

public class ParameterAssignmentBySMTtest {

	@Test
	public void testSat() throws ParseException {
		ParameterAssignmentBySMT.getParameterValues("a==1 && b==1", null);
	}

	@Test
	public void testUnsat() throws ParseException {
		ParameterAssignmentBySMT.getParameterValues("a==1 && a!=1", null);
	}

	@Test
	public void testCollectionSat() throws ParseException {
		List<String> constrs = Arrays.asList("a==1 || b==1", "b==2");
		ParameterAssignmentBySMT.getParameterValues(constrs, null);
	}

	@Test
	public void testCollectionUnsat() throws ParseException {
		List<String> constrs = Arrays.asList("a==1 && b==1", "b==2");
		ParameterAssignmentBySMT.getParameterValues(constrs, null);
	}

	@Test
	public void testCollectionSat2() throws ParseException {
		List<String> constrs = Arrays.asList("a==1 || b==1", "b==2 && c==3");
		ParameterAssignmentBySMT.getParameterValues(constrs, null);
	}

	@Test
	public void testPerformance() throws ParseException {
		List<String> constrs = new ArrayList<>();
		String allConstr = "";
		for (char c1 = 97; c1 <= 122; c1++) {
			for (char c2 = 97; c2 <= 122; c2++) {
				for (String op : new String[] { "<", "<=", ">", ">=", "==" }) {
					for (int i = 0; i < 10; i++) {
						String id = String.valueOf(c1) + String.valueOf(c2) + "_";
						String constr = id + op + i;
						//System.out.println(constr);
						constrs.add(constr);
						allConstr = allConstr + " && " + constr;
					}
				}
			}
		}
		allConstr = allConstr.substring(4);
		try {
			ParameterAssignmentBySMT.getParameterValues(constrs, null);
		}
		catch(Exception|Error e) {
			System.out.println("Error with collection of expressions!");
		}
		try {
		ParameterAssignmentBySMT.getParameterValues(allConstr, null);
		}
		catch(Exception|Error e) {
			System.out.println("Error with unique expression!");
		}
	}

}
