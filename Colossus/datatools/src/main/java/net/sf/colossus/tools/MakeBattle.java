package net.sf.colossus.tools;


// DOM classes.
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class MakeBattle
{

    static void addCreature(Document doc, String legion, String creature)
    {
        Element root = doc.getDocumentElement();
        NodeList l;
        int n;

        /* take from caretaker */
        l = root.getElementsByTagName("Caretaker");
        n = l.getLength();
        for (int i = 0; i < n; i++)
        {
            Element c = (Element)l.item(i);

            NodeList l2 = c.getElementsByTagName("Creature");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++)
            {
                Element C = (Element)l2.item(i2);
                if (C.getAttribute("name").equals(creature))
                {
                    //System.err.println("Taking one " + C.getAttribute("name"));
                    String R = C.getAttribute("remaining");
                    int r = Integer.parseInt(R);
                    C.setAttribute("remaining", "" + (r - 1));
                }
                else
                {
                    //System.err.println("Ignoring " + C.getAttribute("name"));
                }
            }
        }
        /* put in Player's Legion */
        l = root.getElementsByTagName("Player");
        n = l.getLength();
        for (int i = 0; i < n; i++)
        {
            Element c = (Element)l.item(i);

            NodeList l2 = c.getElementsByTagName("Legion");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++)
            {
                Element C = (Element)l2.item(i2);
                if (C.getAttribute("name").equals(legion))
                {
                    Element nc = doc.createElement("Creature");
                    nc.setAttribute("name", creature);
                    C.appendChild(nc);
                }
                else
                {
                    // nothing to do
                }
            }
        }
        /* put in History */
        l = root.getElementsByTagName("History");
        n = l.getLength();
        for (int i = 0; i < n; i++)
        {
            Element c = (Element)l.item(i);

            NodeList l2 = c.getElementsByTagName("Reveal");
            int n2 = l2.getLength();

            for (int i2 = 0; i2 < n2; i2++)
            {
                Element C = (Element)l2.item(i2);
                if (C.getAttribute("markerId").equals(legion))
                {
                    Element nc = doc.createElement("creature");
                    Element C2 = (Element)C.getElementsByTagName("creatures")
                        .item(0);
                    C2.appendChild(nc);
                    nc.setTextContent(creature);
                }
                else
                {
                    // nothing to do
                }
            }
        }
    }

    static void addCreaturesToLegions(Document doc, String al, String[] aca,
        String dl, String[] dca)
    {

        for (int i = 0; i < aca.length; i++)
        {
            addCreature(doc, al, aca[i]);
        }
        for (int i = 0; i < dca.length; i++)
        {
            addCreature(doc, dl, dca[i]);
        }
    }

    static void replaceAIs(Document doc, String ap, String aAI, String dp,
        String dAI)
    {
        Element root = doc.getDocumentElement();
        NodeList l;
        int n;

        /* take from caretaker */
        l = root.getElementsByTagName("Player");
        n = l.getLength();
        for (int i = 0; i < n; i++)
        {
            Element c = (Element)l.item(i);
            if (c.getAttribute("name").equals(ap))
            {
                c.setAttribute("type", "net.sf.colossus.ai." + aAI);
            }
            if (c.getAttribute("name").equals(dp))
            {
                c.setAttribute("type", "net.sf.colossus.ai." + dAI);
            }
        }
    }

    public static void main(String[] arg)
    {
        Document doc = null;
        String[] aca = null;
        String[] dca = null;
        String aAI = "SimpleAI";
        String dAI = "SimpleAI";

        for (int i = 0; i < arg.length; i++)
        {

            if (arg[i].startsWith("--alist="))
            {
                aca = arg[i].substring(8).split(",");
            }
            else if (arg[i].startsWith("--dlist="))
            {
                dca = arg[i].substring(8).split(",");
            }
            else if (arg[i].startsWith("--aAI="))
            {
                aAI = arg[i].substring(6);
            }
            else if (arg[i].startsWith("--dAI="))
            {
                dAI = arg[i].substring(6);
            }
            else
            {
                System.err.println("usage:\n\t <whatever_to_launch>"
                    + "--alist=<comma separated list of attacking creature> "
                    + "--dlist=<comma separated list of defending creature> "
                    + "--aAI=<attacking AI name> "
                    + "--dAI=<defending AI name> ");
                System.exit(-1);
            }
        }

        try
        {
            javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory
                .newInstance().newDocumentBuilder();

            doc = db.parse("MakeBattle.xml");
        }
        catch (java.io.FileNotFoundException fnfe)
        {
            System.err.println("Open Failed (FileNotFoundException): " + fnfe);
        }
        catch (java.io.IOException ie)
        {
            System.err.println("Open failed (IOException): " + ie);
        }
        catch (javax.xml.parsers.ParserConfigurationException pce)
        {
            System.err.println("Open failed (ParserConfigurationException): "
                + pce);
        }
        catch (org.xml.sax.SAXException se)
        {
            System.err.println("Open failed (SAXException): " + se);
        }

        addCreaturesToLegions(doc, "Rd01", aca, "Br01", dca);
        replaceAIs(doc, "Red1", aAI, "Brown", dAI);

        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(System.out);
        TransformerFactory tf = TransformerFactory.newInstance();
        try
        {
            Transformer serializer = tf.newTransformer();
            //        serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
            //        serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        }
        catch (javax.xml.transform.TransformerConfigurationException tce)
        {
            System.err
                .println("Write failed (TransformerConfigurationException): "
                    + tce);
        }
        catch (javax.xml.transform.TransformerException te)
        {
            System.err.println("Write failed (TransformerException): " + te);
        }
    }
}