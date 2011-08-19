package org.deri.eurostat.toc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.deri.eurostat.zip.UnCompressXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Downloads the ToC.XML from EuroStat and extracts the Dataset URLs from it. Each of
 * the parsed URL is than sent to UnCompressXML class to uncompress the file and
 * RDFize the DSD and SDMX observations.
 * 
 * @author Aftab Iqbal
 *
 */
public class ParseToC {

	//private static String xmlFileURL = "E:/EU Projects/EuroStat/ToC/table_of_contents.xml";
	private Document xmlDocument;
	private ArrayList<String> lstDatasetURLs = new ArrayList<String>();
	private static int printDatasets = 10;
	
	public void get_ToC_XMLStream()
	{
		try {
			
			URL url = new URL("http://epp.eurostat.ec.europa.eu/NavTree_prod/everybody/BulkDownloadListing?sort=1&file=table_of_contents.xml");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream is = conn.getInputStream();

			if (conn.getResponseCode() != 200) {
				System.err.println(conn.getResponseCode());
			}

			initObjects(is);
			parseDataSets();
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void initObjects(InputStream in){        
        try {
            xmlDocument = DocumentBuilderFactory.
			newInstance().newDocumentBuilder().
			parse(in);            
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }       
    }
	
	public void parseDataSets()
	{
		int count = 0;
		Element element = xmlDocument.getDocumentElement();
		
		NodeList nl = element.getElementsByTagName("nt:leaf");
		if(nl != null && nl.getLength() > 0)
		{
			for(int i = 0 ; i < nl.getLength();i++)
			{
				Element ele = (Element)nl.item(i);
				if(ele.getAttribute("type").equals("dataset") || ele.getAttribute("type").equals("table"))
				{
					//getObservations(ele);
					getDatasetURLs(ele);
				}
			}
		}
		
		System.out.println("Total Datasets found in the ToC are : " + lstDatasetURLs.size());
		
		for(String str:lstDatasetURLs)
		{
			System.out.println(str);
			if(++count == printDatasets)
				break;
		}

		// This piece of code will parse the compressed file URLs sequentially.
//		UnCompressXML obj = new UnCompressXML();
//		for(String str:lstDatasetURLs)
//		{
//			if(++count == 10)
//				break;
//			
//			obj.parseZipFile(str);
//		}
		
	}
	
	// get the URLs of datasets which have format SDMX
	public void getDatasetURLs(Element element)
	{
		
		NodeList nl = element.getElementsByTagName("nt:downloadLink");
		if(nl != null && nl.getLength() > 0)
		{
			for(int i = 0 ; i < nl.getLength();i++)
			{
				Element ele = (Element)nl.item(i);
				if(ele.getAttribute("format").equals("sdmx"))
				{
					if(!lstDatasetURLs.contains(ele.getTextContent()))
						lstDatasetURLs.add(ele.getTextContent());
					
				}
			}
		}
		
	}
	
//	// get the total number of observations which exists in the datasets
//	public void getObservations(Element element)
//	{
//		String code = getTextValue(element, "nt:code");
//		
//		if(!lstDatasets.contains(code))
//		{
//			lstDatasets.add(code);
//			if(!(getTextValue(element, "nt:values").equals("")))
//				sumObeservation += Integer.parseInt(getTextValue(element, "nt:values"));
//		}
//		else
//			lstDuplicateDatasets.add(code);	
//		
//	}
//
//	private String getTextValue(Element ele, String tagName) {
//		String textVal = "";
//		NodeList nl = ele.getElementsByTagName(tagName);
//		
//		if(nl != null && nl.getLength() > 0) {
//			Element el = (Element)nl.item(0);
//			if(el.getFirstChild() != null)
//				textVal = el.getFirstChild().getNodeValue();
//			else
//				textVal = "";
//		}
//		
//		
//		return textVal;
//	}
	
	private static void usage()
	{
		System.out.println("usage: ParseToC [parameters]");
		System.out.println();
		System.out.println("	-n num		No. of Dataset URLs to print. Default sets to 10.");
	}
	
	public static void main(String[] args) throws Exception
	{
		ParseToC obj = new ParseToC();
		
		CommandLineParser parser = new BasicParser( );
		Options options = new Options( );
		options.addOption("h", "help", false, "Print this usage information");
		options.addOption("n", "num", true, "No. of Dataset URLs to print. Default sets to 10.");

		CommandLine commandLine = parser.parse( options, args );
		
		if( commandLine.hasOption('h') ) {
		    usage();
		    return;
		 }
		
		if(commandLine.hasOption('n'))
			printDatasets = Integer.parseInt(commandLine.getOptionValue('n'));
		
		obj.get_ToC_XMLStream();
	}
	
}