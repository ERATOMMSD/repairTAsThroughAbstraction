package parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ta.Location;
import ta.TA;
import ta.Transition;

public class TAParserFromUppaal extends TAParser {

	public static TAParserFromUppaal instance = new TAParserFromUppaal();
	
	@org.junit.Test
	public void testParsing() throws Exception {
		TA ta = instance.loadModel(new File("files/carAlarmSystem.xml"));
		System.out.println(ta);
	}

	@Override
	public TA loadModel(String model) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	
		dbf.setValidating(false);
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(new ByteArrayInputStream(model.getBytes("UTF-8")));
		return loadModel(d);
	}
	
	public static TA loadModel(Document d) {
		TA ta = new TA();
		NodeList names = d.getElementsByTagName("name");
		List<String> automatonNames = new ArrayList<>();
		for (int it=0; it<names.getLength(); it++) {
			Node name = names.item(it);
			automatonNames.add(name.getTextContent());
		}
		List<String> initialLocationsIds = new ArrayList<>();
		NodeList nodes = d.getElementsByTagName("init");
		for (int i=0; i<nodes.getLength(); i++) initialLocationsIds.add(nodes.item(i).getAttributes().getNamedItem("ref").getNodeValue());
		
		nodes = d.getElementsByTagName("declaration");
		for (int i=0; i<nodes.getLength(); i++) { // add clocks
			String[] stmts = nodes.item(i).getTextContent().split(";");
			for (String stmt : stmts) {
				if (stmt.contains("clock ")) ta.addClock(stmt.split("clock ")[1].trim());
			}
		}		
		
		int templateIndex = 0;
		NodeList locations = d.getElementsByTagName("location");
		for (int i=0; i<locations.getLength(); i++) {
			Node node = locations.item(i);
			String id = node.getAttributes().getNamedItem("id").getNodeValue();
			String invariant = "";
			nodes = node.getChildNodes();
			for (int j=0; j<nodes.getLength(); j++) {
				Node child = nodes.item(j);
				if (!child.hasAttributes()) continue;
				String value = child.getAttributes().getNamedItem("kind").getNodeValue();
				if (value.equals("invariant")) {
					invariant = unescapeHtmlSimple(child.getTextContent());
				}
			}
			if (initialLocationsIds.contains(id)) templateIndex = initialLocationsIds.indexOf(id);
			String automatonName = automatonNames.get(templateIndex);
			Location l = new Location(id, invariant, automatonName);
			ta.addLocation(automatonName, l, initialLocationsIds.contains(id));
		}
		
		NodeList transitions = d.getElementsByTagName("transition");
		for (int i=0; i<transitions.getLength(); i++) {
			Node node = transitions.item(i);
			NodeList children = node.getChildNodes();
			String name="", guard="", assignment="";
			Location source=null, dest=null;
			for (int j=0; j<children.getLength(); j++) {
				Node n = children.item(j);
				if (n.getNodeName().equals("label")) {
					if (n.getAttributes().getNamedItem("kind").getNodeValue().equals("synchronisation")) name = n.getTextContent().replace("!", "").replace("?", "");
					else if (n.getAttributes().getNamedItem("kind").getNodeValue().equals("assignment")) assignment = n.getTextContent();
					else if (n.getAttributes().getNamedItem("kind").getNodeValue().equals("guard")) guard = unescapeHtmlSimple(n.getTextContent());
				}
				else if (n.getNodeName().equals("source")) source = ta.locations.get(n.getAttributes().getNamedItem("ref").getNodeValue());
				else if (n.getNodeName().equals("target")) dest = ta.locations.get(n.getAttributes().getNamedItem("ref").getNodeValue());
			}
			Transition t = new Transition(name, source, dest, guard, assignment);
			ta.addTransition(t);
		}
		return ta;
	}
	
	public static Document loadDocument(File file) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	
		dbf.setValidating(true);
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document d = db.parse(file);
		return d;
	}
	
	public static String unescapeHtmlSimple(String s) {
		return s.replace("&lt;", "<").replace("&gt;", ">");
	}
	
}
