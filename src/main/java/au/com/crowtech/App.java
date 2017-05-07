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
 * @author Adam Crow (adamcrow63@gmail.com)
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

	// JCommander Parameters

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

	@Parameter(names = { "-e",
			"--execute" }, variableArity = true, description = "Execute command and quit. Format: <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD ")
	private List<String> commandString = new ArrayList<String>();
	
	RateConverter rateConverter = null;
	CurrencyPrecisions currencyPrecisions = null;

	/**
     * Establishes an interactive FX calculator that can convert a given set of Currencies to each other.
     *
     */
	public static void main(String[] args)  {
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName(PROGRAM_NAME);
		jc.setColumnSize(COLUMN_SIZE);
		jc.parse(args);

		if (app.help) {
			jc.usage();
			return;
		}

		try {
			app.run();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

	}

	/**
     * Run the FX Calculator.
     *
     *<p>
     *This method will examine the given JCommander parameters supplied by the user and display the version or Title
     *if requested.
     * This method will then load in the base Rates and Currency Precision data that is required for
     * calculation. If the user supplies valid files for this data then they will be used, otherwise internal
     * data files are used.
     * <p>
     * 
     * @throws IOException if the input Base/Terms rates file or Decimal Precision file is incorrect.
     */
	public void run() throws IOException {

		if (showVersion) {
			showVersion();
			return;
		}
		if (!quiet) {
			showTitle();
		}

		
		// Import the Precisions for currencies. A default value of 2 decimal places is used if a base is not
		// supplied in the file.
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
			
		// Now run a single external command (if supplied) else enter interactive shell.
		
		if (!commandString.isEmpty()) {
			processCommandLine(String.join(" ", commandString));
		} else {
			processCommandLines();
		}

		// Inform the user that the shell has ended.
		
		if (!quiet) {
			System.out.println(FINISHED);
		}

	}

	/**
     * Display to standard output a human readable Version of this program.
     * <p>
     * The values are set as String constants at the top of this file. This method may be better
     * served by using a maven git.properties generated file.
     * <p>
     */
	private void showVersion() {
		System.out.println(PROGRAM_NAME + " version \"" + VERSION + "\"");
	}

	/**
     * Display to standard output a human readable Title for this program.
     * <p>
     * The values are set as String constants at the top of this file. 
     * The 'exit to quit' Text serves to remind the user how to exit the shell.
     * <p>
     */
	private void showTitle() {
		System.out.println(TITLE);
		System.out.println(EXIT_TO_QUIT);
	}

	/**
     * Process command Lines in an interactive shell.
     * <p>
     * This method will display to the user a shell prompt. The user may then enter in 
     * fxcalculator shell commands in the format
     * <pre>
     * <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD
     * </pre>
     * entering a blank command line or 'exit' or 'quit' will terminate the shell.
     * <p>
     */

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
			e.printStackTrace(); // TO DO ..
		}
	}

	/**
     * Process a single RateConverter command line.
     * <p>
     * This method will parse the complete input line (upon a user initiated newline) to confirm validity.
     * It will then check the given base value to ensure that it is non negative. Any error is reported back to the user.
     * This validated set of inputs is then passed to the RateConverter object whereupon a result is returned.
     * <p>
     * If the user has entered an empty command line then the method returns false .
     * <p>
     * If no errors then the conversion result is displayed to the user.
     *
     * @param line - An fxconverter command line
     * @return whether or not the supplied command line was empty or not.
     */
	private boolean processCommandLine(String line) {

		String regex = "^$|^(" + REGEX_BASE + ")\\s+(" + REGEX_NUMBER_VALUE + ")\\s+in\\s+(" + REGEX_TERMS + ")";
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(line);
		if (matcher.find()) {
			if (StringUtils.isEmpty(line)) {
				return false;
			}
			String base = matcher.group(1).toUpperCase();
			String terms = matcher.group(3).toUpperCase();
			Double baseValue = Double.parseDouble(matcher.group(2));
			
			// Check if baseValue is non negative
			if (Double.compare(baseValue, 0.0) >= 0) {
				try {
					Double result = rateConverter.calculateRate(base, terms, baseValue);
					
					// Display the required result
					System.out.println(base + " " + currencyPrecisions.formattedValue(base, baseValue) + " = " + terms + " "
							+ currencyPrecisions.formattedValue(terms, result));
				} catch (NoCurrencyConversionPathException e) {
					System.err.println(ERROR_MSG_UNABLE_TO_FIND_RATE + e.getBaseTerms());
				}
			} else {
				System.err.println(ERROR_MSG_NEGATIVE_NUMBER + " " + baseValue);
			}
		} else {
			// display that an error has occured and the correct syntax
			System.err.println(ERROR_MSG_BAD_COMMAND_SYNTAX);
		}
		return true;
	}



}