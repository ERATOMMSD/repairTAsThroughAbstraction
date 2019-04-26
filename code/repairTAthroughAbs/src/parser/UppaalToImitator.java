package parser;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class UppaalToImitator {

	public static void main(String[] args) throws Exception {
		UppaalToImitator instance = new UppaalToImitator();
		Document d = loadDocument(args[0]);
		instance.produceImitator(d);
	}
	
	public String produceImitator(Document d) {
		if (d != null) {
			
		}
		return "";
	}
	
	public static Document loadDocument(String path) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	
		dbf.setValidating(true);
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(new File(path));
		return d;
	}
	
}