package repairta.process;

import java.util.Arrays;

import repairta.Combination;

public class CombinationTest {
	
	@org.junit.Test
	public void test() {
		print(new Combination(5,2));
		System.out.println();
		print(new Combination(5,0));
	}
	
	public void print(Combination c) {
		for (boolean[] b : c) {
			System.out.println(Arrays.toString(b));
		}		
	}
}
