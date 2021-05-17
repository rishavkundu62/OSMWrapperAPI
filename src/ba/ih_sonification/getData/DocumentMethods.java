package ba.ih_sonification.getData;

import ba.ih_sonification.ba.ih_sonification.getData;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class DocumentMethods {
	/**
	 * Given an XML Document it returns a String containing the Document Method is
	 * from
	 * http://www.programcreek.com/java-api-examples/index.php?api=org.w3c.dom.Document
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public static String xmlDocToString(Document doc)
			throws IllegalArgumentException, TransformerException, TransformerFactoryConfigurationError {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		String xmlString = result.getWriter().toString();
		return xmlString;
	}
}