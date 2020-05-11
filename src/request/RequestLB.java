package request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RequestLB {

	public static void main(String[] args) throws IOException {
		System.out.println("Data (yyyy-MM-dd) nuo kada:");
		String dateFrom = checkDate();
		System.out.println("Data (yyyy-MM-dd) iki kada:");
		String dateTo = checkDate();
		System.out.println("Valiuta:");
		String ccy = checkCurrency();
		System.out.println("Formatas:");
		String format = checkFormat();
		String st = requestLb(dateFrom, dateTo, ccy, format);
		float lastRate = 0;
		float firstRate = 0;
		URLConnection stockURL = null;
		stockURL = new URL(st).openConnection();
		stockURL.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		stockURL.setConnectTimeout(1000);
		stockURL.connect();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(stockURL.getInputStream(), Charset.forName("UTF-8")));
		if (format.equals("xml")) {
			ArrayList<String> listOfRates = new ArrayList<String>();
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder b = factory.newDocumentBuilder();
				Document doc = b.parse(stockURL.getInputStream());
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("item");
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node node = nList.item(temp);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) node;
						listOfRates.add(eElement.getElementsByTagName("santykis").item(0).getTextContent());
						System.out.println(eElement.getElementsByTagName("pavadinimas").item(0).getTextContent());
						System.out.println(eElement.getElementsByTagName("valiutos_kodas").item(0).getTextContent());
						System.out.println(eElement.getElementsByTagName("santykis").item(0).getTextContent());
						System.out.println(eElement.getElementsByTagName("data").item(0).getTextContent());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastRate = Float.parseFloat(listOfRates.get(1).replace("\"", "").replace(",", "."));
			firstRate = Float.parseFloat(listOfRates.get(listOfRates.size() - 1).replace("\"", "").replace(",", "."));
			System.out.println("Pokytis: " + (lastRate - firstRate));
		}
		if (format.equals("csv")) {
			String next, line = in.readLine();
			for (; (line = in.readLine()) != null;) {
				System.out.println(line);
				for (boolean first = true, last = (line == null); !last; first = false, line = next) {
					last = ((next = in.readLine()) == null);
					if (first) {
						String[] parts = line.split(";");
						lastRate = Float.parseFloat(parts[2].replace("\"", "").replace(",", "."));
						System.out.println(parts[0] + parts[1] + parts[2] + parts[3]);
					} else if (last) {
						String[] parts = line.split(";");
						firstRate = Float.parseFloat(parts[2].replace("\"", "").replace(",", "."));
						System.out.println(parts[0] + parts[1] + parts[2] + parts[3]);
					} else {
//						System.out.println("Vidurines periodo dienos: " + line);
					}
				}
			}
			System.out.println("Pokytis: " + (lastRate - firstRate));
		}
	}

	/**
	 * Tikrina ar formatas yra tinkamas.
	 * 
	 * @return valiuta.
	 */
	public static String checkFormat() {
		boolean check = false;
		String format = null;
		do {
			@SuppressWarnings("resource")
			Scanner myObj = new Scanner(System.in);
			format = myObj.nextLine().toLowerCase();
			check = (format.equals("csv") || format.equals("xml"));
			if (!check) {
				System.out.println("Bandykite dar kartà:");
			}
		} while (!check);
		return format;
	}

	/**
	 * Tikrina ar valiuta yra Currency klaseje.
	 * 
	 * @return valiuta.
	 */
	private static String checkCurrency() {
		boolean check = false;
		String currency = null;
		do {
			@SuppressWarnings("resource")
			Scanner myObj = new Scanner(System.in);
			currency = myObj.nextLine().toUpperCase();
			check = Currency.getAvailableCurrencies().contains(Currency.getInstance(currency));
			if (!check) {
				System.out.println("Bandykite dar kartà:");
			}
		} while (!check);
		return currency;
	}

	/**
	 * URL formavimas.
	 * 
	 * @return URL.
	 */
	public static String requestLb(String dateFrom, String dateTo, String ccy, String format) {
		String initialUrlStr = "https://www.lb.lt/lt/currency/exportlist/?" + format + "=1&currency=" + ccy
				+ "&ff=1&class=Eu&type=day&date_from_day=" + dateFrom + "&date_to_day=" + dateTo;
		return initialUrlStr;
	}

	/**
	 * Priima naudotojo ivesti. Jei tai ne darbo diena, prasoma ivesti is naujo.
	 * 
	 * @return diena.
	 */
	public static String checkDate() {
		boolean check = false;
		@SuppressWarnings("resource")
		Scanner myObj = new Scanner(System.in);
		String dateInput = null;
		do {
			dateInput = myObj.nextLine();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date date = null;
			try {
				date = sdf.parse(dateInput);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			check = isBusinessDay(cal);
			if (!check) {
				System.out.println("Bandykite dar kartà:");
			}
		} while (!check);
		return dateInput;
	}

	/**
	 * Tikrina ar darbo diena.
	 */
	public static boolean isBusinessDay(Calendar cal) {
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			return false;
		}
		// Naujieji metai
		if (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) == 1) {
			return false;
		}
		// Kaledos
		if (cal.get(Calendar.MONTH) == Calendar.DECEMBER
				&& (cal.get(Calendar.DAY_OF_MONTH) == 25 || cal.get(Calendar.DAY_OF_MONTH) == 26)) {
			return false;
		}
		// Geguzes 1
		if (cal.get(Calendar.MONTH) == Calendar.MAY && cal.get(Calendar.DAY_OF_MONTH) == 1) {
			return false;
		}
		return true;
	}

}