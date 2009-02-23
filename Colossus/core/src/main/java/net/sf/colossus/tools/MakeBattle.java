package net.sf.colossus.tools;


// DOM classes.
import org.w3c.dom.*;
//JAXP 1.1
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

public class MakeBattle {

    static void addCreature(Document doc, String legion, String creature) {
        Element root = doc.getDocumentElement();
        NodeList l;
        int n;

        /* take from caretaker */
        l = root.getElementsByTagName("Caretaker");
        n = l.getLength();
        for (int i = 0; i < n; i++) {
            Element c = (Element) l.item(i);

            NodeList l2 = c.getElementsByTagName("Creature");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++) {
                Element C = (Element) l2.item(i2);
                if (C.getAttribute("name").equals(creature)) {
                    System.err.println("Taking one " + C.getAttribute("name"));
                    String R = C.getAttribute("remaining");
                    int r = Integer.parseInt(R);
                    C.setAttribute("remaining", "" + (r - 1));
                } else {
                    //System.err.println("Ignoring " + C.getAttribute("name"));
                }
            }
        }
        /* put in Player's Legion */
        l = root.getElementsByTagName("Player");
        n = l.getLength();
        for (int i = 0; i < n; i++) {
            Element c = (Element) l.item(i);

            NodeList l2 = c.getElementsByTagName("Legion");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++) {
                Element C = (Element) l2.item(i2);
                if (C.getAttribute("name").equals(legion)) {
                    Element nc = doc.createElement("Creature");
                    nc.setAttribute("name", creature);
                    C.appendChild(nc);
                } else {
                }
            }
        }
        /* put in History */
        l = root.getElementsByTagName("History");
        n = l.getLength();
        for (int i = 0; i < n; i++) {
            Element c = (Element) l.item(i);

            NodeList l2 = c.getElementsByTagName("Reveal");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++) {
                Element C = (Element) l2.item(i2);
                if (C.getAttribute("markerId").equals(legion)) {
                    Element nc = doc.createElement("creature");
                    Element C2 = (Element) C.getElementsByTagName("creatures").item(0);
                    C2.appendChild(nc);
                    nc.setTextContent(creature);
                } else {
                }
            }
        }
    }

    static void doTheJob(Document doc, String[] aca, String[] dca) {

        for (int i = 0; i < aca.length; i++) {
            addCreature(doc, "Rd01", aca[i]);
        }
        for (int i = 0; i < dca.length; i++) {
            addCreature(doc, "Br01", dca[i]);
        }
    }

    public static void main(String[] arg) {
        Document doc = null;


        if (arg.length < 2) {
            System.err.println("usage:\n\t <whatever_to_launch>" +
                    "<comma separated list of attacking creature> " +
                    "<comma separated list of defending creature>");
            System.exit(-1);
        }

        String[] aca = arg[0].split(",");
        String[] dca = arg[1].split(",");

        try {
            javax.xml.parsers.DocumentBuilder db =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();

            doc = db.parse("MakeBattle.xml");
        } catch (java.io.FileNotFoundException fnfe) {
            System.err.println("Open Failed (FileNotFoundException): " + fnfe);
        } catch (java.io.IOException ie) {
            System.err.println("Open failed (IOException): " + ie);
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            System.err.println("Open failed (ParserConfigurationException): " + pce);
        } catch (org.xml.sax.SAXException se) {
            System.err.println("Open failed (SAXException): " + se);
        }

        doTheJob(doc, aca, dca);


        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(System.out);
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer serializer = tf.newTransformer();
            //        serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
            //        serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        } catch (javax.xml.transform.TransformerConfigurationException tce) {
            System.err.println("Write failed (TransformerConfigurationException): " + tce);
        } catch (javax.xml.transform.TransformerException te) {
            System.err.println("Write failed (TransformerException): " + te);
        }
    }
}