package au.com.crowtech;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Precision;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import es.usc.citius.hipster.algorithm.Algorithm;
import es.usc.citius.hipster.algorithm.Algorithm.SearchResult;
import es.usc.citius.hipster.algorithm.Hipster;
import es.usc.citius.hipster.graph.GraphBuilder;
import es.usc.citius.hipster.graph.GraphSearchProblem;
import es.usc.citius.hipster.graph.HipsterDirectedGraph;
import es.usc.citius.hipster.model.impl.WeightedNode;
import es.usc.citius.hipster.model.problem.SearchProblem;

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

	private static final Integer DEFAULT_PRECISION = 2;

	private static final String ERROR_MSG_BAD_COMMAND_SYNTAX = "Syntax should be <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD\n"+EXIT_TO_QUIT;
	private static final String ERROR_MSG_NEGATIVE_NUMBER = "Amount must be positive";
	private static final String ERROR_MSG_UNABLE_TO_FIND_RATE = "Unable to find rate for ";

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

	private HipsterDirectedGraph<String, Double> ratesGraph = null; // This
																	// graph is
																	// used to
																	// link all
																	// the
																	// currencies
																	//

	private Map<String, Integer> currencyPrecisionMap = new HashMap<String, Integer>(); // Store
																						// the
																						// set
																						// of
	// currencies precision

	@Parameter(names = { "-h", "?", "--help" }, description = "Show help", help = true)
	private boolean help = false;

	@Parameter(names = { "-q", "--quiet" }, description = "hide info output for quiet mode")
	private boolean quiet = false;

	@Parameter(names = { "-v", "--verbose" }, description = "Write more info")
	private boolean verbose = false;

	@Parameter(names = { "-V", "--version" }, description = "Output version information and exit")
	private boolean showVersion = false;

	@Parameter(names = { "-fr",
			"--rates-file" }, description = "Load in a file containing the list of currency base/terms and rates")
	private String fileBaseTermsPath = null;

	@Parameter(names = { "-fp",
			"--precisions-file" }, description = "Load in a file containing the list of currency precisions")
	private String fileCurrencyPrecisionsPath = null;

	// receives direct command line parameters than options
	@Parameter(names = { "-e",
			"--execute" }, variableArity = true, description = "Execute command and quit. Format: <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD ")
	private List<String> commandString = new ArrayList<String>();

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

		try {
			loadBaseTerms(fileBaseTermsPath);
			loadCurrencyPrecisions(fileCurrencyPrecisionsPath);
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1); // // return non zero error for use in scripting
		}

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
			String regex = "^$|(" + REGEX_BASE + ")\\s+(" + REGEX_NUMBER_VALUE + ")\\s+in\\s+(" + REGEX_TERMS + ")";
			Pattern p = Pattern.compile(regex);

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
					Double result = calculateRate(base, terms, baseValue);
					System.out.println(base + " " + formattedValue(base, baseValue) + " = " + terms + " "
							+ formattedValue(terms, result));
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

	private void loadBaseTerms(String fileBaseTermsPath) throws FileNotFoundException {
		BufferedReader input = null;
		InputStream in = null;

		try {

			if (fileBaseTermsPath == null) {
				in = getClass().getResourceAsStream("/base_terms.txt");
			} else {
				in = new FileInputStream(new File(fileBaseTermsPath));
			}

			input = new BufferedReader(new InputStreamReader(in));

			// Create a simple weighted directed graph with Hipster where
			// vertices are Strings and edge values are just doubles

			GraphBuilder<String, Double> gb = GraphBuilder.<String, Double>create();

			Scanner sc = new Scanner(input);

			try {

				Pattern p = Pattern
						.compile("(" + REGEX_BASE + ")[\\/]?(" + REGEX_TERMS + ")\\=(" + REGEX_NUMBER_VALUE + ")");

				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					Matcher matcher = p.matcher(line);
					if (matcher.find()) {
						String base = matcher.group(1);
						String terms = matcher.group(2);
						Double rate = Double.parseDouble(matcher.group(3)); // Note
																			// number
																			// exception
																			// checked
																			// for
																			// in
																			// regex
						if (rate > 0.0) {
							// Add pair

							gb.connect(base).to(terms).withEdge(rate);
							// invert rate and add reverse pair
							Double invRate = 1.0 / rate;
							gb.connect(terms).to(base).withEdge(invRate);

							if (verbose) {
								System.out.println(
										"Added " + base + " -> " + terms + " at " + rate + " and inverse " + invRate);
							}
						} else {
							System.out.println("Rate error at line :" + line + " in file " + fileBaseTermsPath
									+ " , Rate must be positive");
						}
					} else {
						System.out.println("Syntax error at line :" + line + " in file " + fileBaseTermsPath);
					}
				}

			} catch (Exception ex) {
				System.exit(1);
			} finally {
				sc.close();
				ratesGraph = gb.createDirectedGraph();
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

	private void loadCurrencyPrecisions(String fileCurrencyPrecisionPath) throws FileNotFoundException {
		BufferedReader input = null;
		InputStream in = null;

		try {
			if (fileCurrencyPrecisionPath == null) {
				in = getClass().getResourceAsStream("/decimal_places.txt");
			} else {
				in = new FileInputStream(new File(fileCurrencyPrecisionPath));
			}

			input = new BufferedReader(new InputStreamReader(in));

			Scanner sc = new Scanner(input);

			try {

				Pattern p = Pattern.compile("(" + REGEX_BASE + ")\\=(\\d+)");

				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					Matcher matcher = p.matcher(line);
					if (matcher.find()) {
						String currencyCode = matcher.group(1);
						String precisionStr = matcher.group(2);
						Integer precision = Integer.valueOf(precisionStr);
						currencyPrecisionMap.put(currencyCode, precision);

						if (verbose) {
							System.out.println("Setting " + currencyCode + " Precision to " + precisionStr);
						}
					} else {
						System.out.println("Syntax error at line :" + line + " of file " + fileCurrencyPrecisionPath);
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

	private Double calculateRate(String base, String terms, Double baseValue) throws NoCurrencyConversionPathException {

		// Create the search problem. For graph problems, just use
		// the GraphSearchProblem util class to generate the problem with ease.
		SearchProblem<Double, String, WeightedNode<Double, String, Double>> p = GraphSearchProblem.startingFrom(base)
				.in(ratesGraph).takeCostsFromEdges().build();

		// Search the shortest path from "AUD" to "JPY" use smallest conversions
		SearchResult result = Hipster.createDijkstra(p).search(terms);

		LinkedList<Double> recoverActionPath = (LinkedList<Double>) Algorithm.recoverActionPath(result.getGoalNode());
		if ((recoverActionPath.isEmpty()) && (!base.equals(terms))) {
			throw new NoCurrencyConversionPathException(base, terms);
		}
		if (verbose) {
			LinkedList<String> recoverStatePath = (LinkedList<String>) Algorithm.recoverStatePath(result.getGoalNode());
			System.out.println("Conversion Path is " + recoverStatePath);
		}
		Double conversionRate = recoverActionPath.stream().reduce(1d, (a, b) -> a * b);

		Double conversionValue = baseValue * conversionRate;

		return conversionValue;
	}

	public String formattedValue(String currencyCode, Double value) {
		Integer precision = currencyPrecisionMap.get(currencyCode);
		if (precision == null)
			precision = DEFAULT_PRECISION;

		Double roundedResult = Precision.round(value, precision);
		String form = "%." + precision + "f";
		return String.format(form, roundedResult);

	}
}