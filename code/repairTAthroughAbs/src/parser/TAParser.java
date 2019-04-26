package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ta.TA;

public abstract class TAParser {
	
	public abstract TA loadModel(String model) throws Exception;
	
	public TA loadModel(File imiFile) throws Exception {
		StringBuilder sb = new StringBuilder();
		BufferedReader fin = new BufferedReader(new FileReader(imiFile));
		String line = null;
		while ((line = fin.readLine())!=null) {
			sb.append(line+"\n");
		}
		fin.close();
		return loadModel(sb.toString());
	}
}
