package au.com.crowtech;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Precision;

public class CurrencyPrecisions {

	private static final String REGEX_BASE = "[a-zA-Z]{3}";
	private static final Integer DEFAULT_PRECISION = 2;

	private Map<String, Integer> currencyPrecisionMap = new HashMap<String, Integer>(); // Store
	// the
	// set
	// of
	// Currency Precisions

	private boolean verbose = false;

	public CurrencyPrecisions(InputStream currencyPrecisionsInputStream, boolean verbose) {

		try {
			loadCurrencyPrecisions(currencyPrecisionsInputStream);
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1); // // return non zero error for use in scripting
		}
	}

	/**
	 * Import the Currency Precisions data from an input Source into a HashMap.
	 * <p>
	 * This method will parse the each line of a supplied Currency Precisions
	 * data file to confirm validity of the line format. It will then save each
	 * Currency Code to a HashMap so that the precision value of any currency
	 * code can be quickly fetched.
	 *
	 * @param in
	 *            InputStream Data containing the Curreny Precisions data
	 * @throws FileNotFoundException
	 */

	public void loadCurrencyPrecisions(InputStream in) throws FileNotFoundException {
		BufferedReader input = null;

		try {

			input = new BufferedReader(new InputStreamReader(in));

			Scanner sc = new Scanner(input);

			try {

				Pattern p = Pattern.compile("^(" + REGEX_BASE + ")\\=(\\d+)\\sdecimal\\splaces");

				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					Matcher matcher = p.matcher(line);
					if (matcher.find()) {
						String currencyCode = matcher.group(1);
						String precisionStr = matcher.group(2);
						Integer precision = Integer.valueOf(precisionStr);

						// Add to the HashMap. Duplicates will override any
						// existing
						currencyPrecisionMap.put(currencyCode, precision);

						if (verbose) {
							System.out.println("Setting " + currencyCode + " Precision to " + precisionStr);
						}
					} else {
						System.err.println("Decimal Precisions File Syntax error at line :" + line);
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				sc.close();
			}
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Returned a formatted Decimal number based upon the supplied currency
	 * code, original value, and using the stored associated Precision.
	 * <p>
	 * If no precision is available for the supplied currency code then a
	 * default precision is used.
	 *
	 * @param currencyCode
	 * @param value
	 *            the original value to be formatted to suit the precision
	 *            requirements for the supplied code.
	 * @return formatted decimal String
	 */

	public String formattedValue(String currencyCode, Double value) {
		Integer precision = currencyPrecisionMap.get(currencyCode);
		if (precision == null)
			precision = DEFAULT_PRECISION;

		Double roundedResult = Precision.round(value, precision);

		// Ensure that all trailing digits are zeroed to ensure precision is in
		// place.
		DecimalFormat df = new DecimalFormat("0.000000000000000000000");
		df.setMinimumFractionDigits(precision); // force the precision
		String ret = df.format(roundedResult);
		return ret;

	}

	/**
	 * @return the currencyPrecisionMap
	 */
	public Map<String, Integer> getCurrencyPrecisionMap() {
		return currencyPrecisionMap;
	}

	/**
	 * @param currencyPrecisionMap
	 *            the currencyPrecisionMap to set
	 */
	public void setCurrencyPrecisionMap(Map<String, Integer> currencyPrecisionMap) {
		this.currencyPrecisionMap = currencyPrecisionMap;
	}

}
