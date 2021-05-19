package ba.ih_sonification.getData;

/**
 * 
 * This software is public domain
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

//import org.osm.lights.diff.OSMNode;
//import org.osm.lights.upload.BasicAuthenticator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 */
public class OSMWrapperAPI {

	private static final String OVERPASS_API = "http://www.overpass-api.de/api/interpreter";
	private static final String OPENSTREETMAP_API_06 = "http://www.openstreetmap.org/api/0.6/";

//	public static OSMNode getNode(String nodeId) throws IOException, ParserConfigurationException, SAXException {
//		String string = "http://www.openstreetmap.org/api/0.6/node/" + nodeId;
//		URL osm = new URL(string);
//		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
//
//		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
//		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
//		Document document = docBuilder.parse(connection.getInputStream());
//		HashMap<String, OSMNode> nodes = getNodes(document);
//		if (!nodes.isEmpty()) {
//			return nodes.iterator().next();
//		}
//		return null;
//	}

	/**
	 * 
	 * @param lat the latitude
	 * @param lon the longitude
	 * @param vicinityRange bounding box in this range
	 * @return the xml document containing the queries nodes
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@SuppressWarnings("nls")
	private static Document getXML(double lat, double lon, double vicinityRange) throws IOException, SAXException,
			ParserConfigurationException {

		DecimalFormat format = new DecimalFormat("##0.0000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH)); //$NON-NLS-1$
		String left = format.format(lon - vicinityRange);
		String bottom = format.format(lat - vicinityRange);
		String right = format.format(lon + vicinityRange);
		String top = format.format(lat + vicinityRange);

		String string = OPENSTREETMAP_API_06 + "map?bbox=" + left + "," + bottom + "," + right + ","
				+ top;
		URL osm = new URL(string);
		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();

		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document tempDoc = docBuilder.parse(connection.getInputStream());
		
//		System.out.print(tempDoc.toString());
		return tempDoc;
	}

	public static Document getXMLFile(String location) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		return docBuilder.parse(location);
	}
	
	/**
	 * 
	 * @param xmlDocument 
	 * @return a list of ways, representing a building or a part of a building
	 */
	@SuppressWarnings("nls")
	public static HashMap<String, OSMWay> getMultipolygons(Document xmlDocument) {
		
		// create a List on OSMWays
		HashMap<String, OSMWay> osmWays = new HashMap<String, OSMWay>();

		// Document xml = getXML(8.32, 49.001);
		Node osmRoot = xmlDocument.getFirstChild();
		
		NodeList osmXMLNodes = osmRoot.getChildNodes();
		
		// Check, objects  Element ...
		for (int i = 1; i < osmXMLNodes.getLength(); i++) {
			
			Node item = osmXMLNodes.item(i);
			if (item.getNodeName().equals("way")) {
				
				boolean hasBuildingTag = false;
				
				NodeList ndOrTagXMLNodes = item.getChildNodes();
				
				ArrayList<String> refNodesIDs = new ArrayList<String>();
				Map<String, String> tags = new HashMap<String, String>();
				
				for (int j = 1; j < ndOrTagXMLNodes.getLength(); j++) {
					
					Node ndOrTagItem = ndOrTagXMLNodes.item(j);
					
					NamedNodeMap ndOrTagAttributes = ndOrTagItem.getAttributes();
					if (ndOrTagAttributes != null) {
						if (ndOrTagItem.getNodeName().equals("tag")) {
							tags.put(ndOrTagAttributes.getNamedItem("k").getNodeValue(), ndOrTagAttributes.getNamedItem("v")
								.getNodeValue());
							
							// Check,the attribute for building
							if (ndOrTagAttributes.getNamedItem("k").getNodeValue().startsWith("building")) {
								hasBuildingTag = true;
							}
							
						} else if (ndOrTagItem.getNodeName().equals("nd")) {
							refNodesIDs.add(ndOrTagAttributes.getNamedItem("ref").getNodeValue());
						}
						
					}
				}
				
				NamedNodeMap attributes = item.getAttributes(); // Attribute get on way
				Node namedItemID = attributes.getNamedItem("id");
				Node namedItemVersion = attributes.getNamedItem("version");

				String id = namedItemID.getNodeValue();
				String version = "0";
				if (namedItemVersion != null) {
					version = namedItemVersion.getNodeValue();
				}

				if (hasBuildingTag) {
					osmWays.put(id, new OSMWay(id, refNodesIDs, tags, version));
				}
			}

		}
		return osmWays;
	}

	/**
	 * 
	 * @param xmlDocument 
	 * @return a list of openseamap nodes extracted from xml
	 */
	@SuppressWarnings("nls")
	public static HashMap<String, OSMNode> getNodes(Document xmlDocument) {
		
		// createa List on OSMNodes
		HashMap<String, OSMNode> osmNodes = new HashMap<String, OSMNode>();


		Node osmRoot = xmlDocument.getFirstChild();
		NodeList osmXMLNodes = osmRoot.getChildNodes();
		
		// Check, objects Element ...
		for (int i = 1; i < osmXMLNodes.getLength(); i++) {
			
			Node item = osmXMLNodes.item(i);
			// ...  Node list
			if (item.getNodeName().equals("node")) {
				
				
				NodeList tagXMLNodes = item.getChildNodes();
				Map<String, String> tags = new HashMap<String, String>();
				for (int j = 1; j < tagXMLNodes.getLength(); j++) {
					Node tagItem = tagXMLNodes.item(j);
					NamedNodeMap tagAttributes = tagItem.getAttributes();
					if (tagAttributes != null) {
						tags.put(tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v")
								.getNodeValue());
					}
				}
				
				NamedNodeMap attributes = item.getAttributes();
				// get the attributes
				Node namedItemID = attributes.getNamedItem("id");
				Node namedItemLat = attributes.getNamedItem("lat");
				Node namedItemLon = attributes.getNamedItem("lon");
				Node namedItemVersion = attributes.getNamedItem("version");

			
				String id = namedItemID.getNodeValue();
				String latitude = namedItemLat.getNodeValue();
				String longitude = namedItemLon.getNodeValue();
				String version = "0";
				if (namedItemVersion != null) {
					version = namedItemVersion.getNodeValue();
				}
				
				// put the attribute in OSMNode
				osmNodes.put(id, new OSMNode(id, latitude, longitude, version, tags));
			}

		}
		return osmNodes;
	}

	public static HashMap<String, OSMNode> getOSMNodesInVicinity(double lat, double lon, double vicinityRange) throws IOException,
			SAXException, ParserConfigurationException { 
		return OSMWrapperAPI.getNodes(getXML(lat, lon, vicinityRange)); // lon and lat were swapped
	}

	/**
	 * 
	 * @param query the overpass query
	 * @return the nodes in the formulated query
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static Document getNodesViaOverpass(String query) throws IOException, ParserConfigurationException, SAXException {
		String hostname = OVERPASS_API;
		String queryString = readFileAsString(query);

		URL osm = new URL(hostname);
		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
		printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
		printout.flush();
		printout.close();

		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		return (testDoc = docBuilder.parse(connection.getInputStream()));
	}
	
	public static Document testDoc;

	/**
	 * 
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	private static String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
	
	private static void printNodesHeading() {
		System.out.println("----------------------");
		System.out.println("---------Nodes--------");
		System.out.println("----------------------");
	}
	
	private static void printWaysHeading() {
		System.out.println("----------------------");
		System.out.println("---------Ways---------");
		System.out.println("----------------------");
	}
	
	private static HashMap<String, OSMNode> osmNodesInVicinity;
	
	private static void printNodes(double lat, double lon, double radius)
			throws IOException, SAXException, ParserConfigurationException {
		printNodesHeading();
		
		osmNodesInVicinity = getOSMNodesInVicinity(lat, lon, radius);
		
		for (String osmNodeID : osmNodesInVicinity.keySet()) {
			System.out.println(osmNodesInVicinity.get(osmNodeID));
		}
	}
	
	private static HashMap<String, OSMWay> osmWaysInVicinity;

	private static void printWays(double lat, double lon, double radius)
			throws IOException, SAXException, ParserConfigurationException {
		printWaysHeading();
		
		osmWaysInVicinity = OSMWrapperAPI.getMultipolygons(getXML(lat, lon, radius));
		
		for (String osmWayID : osmWaysInVicinity.keySet()) {
			System.out.println(osmWaysInVicinity.get(osmWayID));
		}
	}

	/**
	 * main method that simply reads some nodes
	 * 
	 * @param args
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		
		
		double lat = 49.0118;
		double lon = 8.41;
		double radius = 0.0007;
		
		Authenticator.setDefault(new BasicAuthenticator("youruser", "yourpassword"));
		
		printWays(lat, lon, radius);
		
		printNodes(lat, lon, radius);
		
		int counter = 0;
		
		for (String OSMWayID: osmWaysInVicinity.keySet()) {
			
			OSMWay currentWay = osmWaysInVicinity.get(OSMWayID);
			
			
			LinkedHashMap<String, OSMNode> refNodes = new LinkedHashMap<String, OSMNode>();
			
			ArrayList<String> refNodesIDs = currentWay.getRefNodesIDs();
			
			
			for (String OSMNodeID: refNodesIDs) {
				
				OSMNode currentNode = osmNodesInVicinity.get(OSMNodeID);
				
				refNodes.put(OSMNodeID, currentNode);
				
				counter++;
			}
			
			currentWay.setRefNodes(refNodes);
			
			
		}
		
		System.out.println("\nAnzahl Ways = " + osmWaysInVicinity.size() + ",\n"
				+ "Anzahl Way-Nodes (Anfangsnodes doppelt) = " + counter + ", Anzahl Nodes = " + osmNodesInVicinity.size());

	}
	
}
