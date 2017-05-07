package au.com.crowtech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * FX Calculator.
 *
 * @author Adam Crow (adamcrow63@Gmail.com)
 */
public class App {

	private static final String PROGRAM_NAME = "fxcalculator";
	private static final String VERSION = "1.0"; // Ideally derive from
													// git.properties
	private static final String TITLE = PROGRAM_NAME + " " + VERSION + " by Adam Crow <adamcrow63@gmail.com>";
	private static final Integer COLUMN_SIZE = 120; // used for terminal width

	private static final String PROMPT = "%> ";
	private static final String EXIT_TO_QUIT = "Type 'exit' to quit shell.";
	private static final String FINISHED = "Finished"; // useful for IDE
														// consoles.

	private static final String REGEX_BASE = "[a-zA-Z]{3}";
	private static final String REGEX_TERMS = "[a-zA-Z]{3}";
	private static final String REGEX_NUMBER_VALUE = "[+-]?[0-9]*[\\.]?[0-9]*"; // allow
																				// 100.
																				// and
																				// +100
																				// style
																				// numbers
																				// to
																				// ease
																				// syntax
																				// error
																				// confusion

	private static final String ERROR_MSG_BAD_COMMAND_SYNTAX = "Syntax should be <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD\n"+EXIT_TO_QUIT;
	private static final String ERROR_MSG_NEGATIVE_NUMBER = "Amount must be zero or positive";
	private static final String ERROR_MSG_UNABLE_TO_FIND_RATE = "Unable to find rate for ";

	// currencies precision

	@Parameter(names = { "-h", "?", "--help" }, description = "Show help", help = true)
	private boolean help = false;

	@Parameter(names = { "-q", "--quiet" }, description = "hide info output for quiet mode")
	private boolean quiet = false;

	@Parameter(names = { "-v", "--verbose" }, description = "Write more info")
	protected boolean verbose = false;

	@Parameter(names = { "-V", "--version" }, description = "Output version information and exit")
	private boolean showVersion = false;

	@Parameter(names = { "-fr",
			"--rates-file" }, description = "Load in a file containing the list of currency base/terms and rates")
	protected String fileBaseTermsPath = null;

	@Parameter(names = { "-fp",
			"--precisions-file" }, description = "Load in a file containing the list of currency precisions")
	protected String fileCurrencyPrecisionsPath = null;

	// receives direct command line parameters than options
	@Parameter(names = { "-e",
			"--execute" }, variableArity = true, description = "Execute command and quit. Format: <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD ")
	private List<String> commandString = new ArrayList<String>();
	
	RateConverter rateConverter = null;
	CurrencyPrecisions currencyPrecisions = null;

	public static void main(String[] args) throws IOException {
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName(PROGRAM_NAME);
		jc.setColumnSize(COLUMN_SIZE);
		jc.parse(args);

		if (app.help) {

			jc.usage();
			return;
		}

		app.run();

	}

	public void run() throws IOException {

		if (showVersion) {
			showVersion();
			return;
		}
		if (!quiet) {
			showTitle();
		}

		InputStream currencyPrecisionsInputStream = null;
		
		if (fileCurrencyPrecisionsPath == null) {
			currencyPrecisionsInputStream = getClass().getResourceAsStream("/decimal_places.txt");
		} else {
			currencyPrecisionsInputStream = new FileInputStream(new File(fileCurrencyPrecisionsPath));
		}

		InputStream baseTermsInputStream = null;

		if (fileBaseTermsPath == null) {
			baseTermsInputStream = getClass().getResourceAsStream("/base_terms.txt");
		} else {
			baseTermsInputStream = new FileInputStream(new File(fileBaseTermsPath));
		}

		rateConverter = new RateConverter(baseTermsInputStream,verbose); // pass command parameters
		currencyPrecisions = new CurrencyPrecisions(currencyPrecisionsInputStream,verbose); // pass command parameters
			
		if (!commandString.isEmpty()) {
			processCommandLine(String.join(" ", commandString));
		} else {
			processCommandLines();
		}

		if (!quiet) {
			System.out.println(FINISHED);
		}

	}

	private void showVersion() {
		System.out.println(PROGRAM_NAME + " version \"" + VERSION + "\"");
	}

	private void showTitle() {
		System.out.println(TITLE);
		System.out.println(EXIT_TO_QUIT);
	}

	private void processCommandLines() {

		try {
			Scanner scanner = new Scanner(System.in);

			System.out.print(PROMPT);
			while ((scanner.hasNextLine())) {
				String line = scanner.nextLine();
				if (("quit".equals(line))||("exit".equals(line))||(!processCommandLine(line))) {  // note the order of execution
					break;
				}
				System.out.print(PROMPT);
			}
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean processCommandLine(String line) {

		String regex = "^$|(" + REGEX_BASE + ")\\s+(" + REGEX_NUMBER_VALUE + ")\\s+in\\s+(" + REGEX_TERMS + ")";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(line);
		if (matcher.find()) {
			if (StringUtils.isEmpty(line)) {
				return false;
			}
			String base = matcher.group(1).toUpperCase();
			String terms = matcher.group(3).toUpperCase();
			// Normally we should check if base and terms exist in the Currency
			// Set, but the specs do not require it.
			Double baseValue = Double.parseDouble(matcher.group(2));
			if (Double.compare(baseValue, 0.0) >= 0) {
				try {
					Double result = rateConverter.calculateRate(base, terms, baseValue);
					System.out.println(base + " " + currencyPrecisions.formattedValue(base, baseValue) + " = " + terms + " "
							+ currencyPrecisions.formattedValue(terms, result));
				} catch (NoCurrencyConversionPathException e) {
					System.out.println(ERROR_MSG_UNABLE_TO_FIND_RATE + e.getBaseTerms());
				}
			} else {
				System.out.println(ERROR_MSG_NEGATIVE_NUMBER + " " + baseValue);
			}
		} else {
			// display description
			System.out.println(ERROR_MSG_BAD_COMMAND_SYNTAX);
		}
		return true;
	}



}