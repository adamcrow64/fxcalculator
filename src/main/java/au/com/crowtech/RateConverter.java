package au.com.crowtech;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.usc.citius.hipster.algorithm.Algorithm;
import es.usc.citius.hipster.algorithm.Algorithm.SearchResult;
import es.usc.citius.hipster.algorithm.Hipster;
import es.usc.citius.hipster.graph.GraphBuilder;
import es.usc.citius.hipster.graph.GraphSearchProblem;
import es.usc.citius.hipster.graph.HipsterDirectedGraph;
import es.usc.citius.hipster.model.impl.WeightedNode;
import es.usc.citius.hipster.model.problem.SearchProblem;

public class RateConverter {

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

	private Set<String> currencySet = new HashSet<String>();  // used to confirm existance (TODO: check existence of state in graph instead)
	
	private boolean verbose = false;
	
	
	public RateConverter(InputStream baseTermsInputStream, boolean verbose) {
		this.verbose = verbose;
		
		
		try {
			loadBaseTerms(baseTermsInputStream);
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			System.exit(1); // // return non zero error for use in scripting
		}
	}

	/**
     * Import the Base/Terms data from an input Source into a Graph.
     * <p>
     * This method will parse the each line of a supplied Base/Terms Rates data file to confirm validity of the line format.
     * It will then save each Currency Code to a HashSet so that any RateConversion can confirm the existence of the code before 
     * attempting to process the conversion path.
     * Each pair of currencies and their rate is added as two Vertices (codes) and an edge between them (rate).
     * Then the reverse conversion is added by adding the Vertices in a reversed fashion with an inverted rate (zero rate is checked).
      *
     * @param in InputStream Data containing the Base/Terms data
     * @throws FileNotFoundException
     */

	public void loadBaseTerms(InputStream in) throws FileNotFoundException {
		BufferedReader input = null;

		try {

			input = new BufferedReader(new InputStreamReader(in));

			// Create a simple weighted directed graph with Hipster where
			// vertices are Strings and edge values are just doubles

			GraphBuilder<String, Double> gb = GraphBuilder.<String, Double>create();

			Scanner sc = new Scanner(input);

			try {

				Pattern p = Pattern
						.compile("^(" + REGEX_BASE + ")[\\/]?(" + REGEX_TERMS + ")\\=(" + REGEX_NUMBER_VALUE + ")$");

				// Loop through each supplied Data Line
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					Matcher matcher = p.matcher(line);
					if (matcher.find()) {
						String base = matcher.group(1);
						String terms = matcher.group(2);
						
						// Add to a set so that we can check the validity of supplied currency codes int the command line
						currencySet.add(base);
						currencySet.add(terms);
						
						Double rate = Double.parseDouble(matcher.group(3)); // Note
																			// number
																			// exception
																			// checked
																			// for
																			// in
																			// regex
						if (rate > 0.0) {
							// Add pair to graph
							gb.connect(base).to(terms).withEdge(rate);
							// invert rate and add reverse pair
							Double invRate = 1.0 / rate;
							gb.connect(terms).to(base).withEdge(invRate);

							if (verbose) {
								System.out.println(
										"Added " + base + " -> " + terms + " at " + rate + " and inverse " + invRate);
							}
						} else {
							System.err.println("Loading Base/Terms Rate error at line :" + line
									+ " , Rate must be zero or positive");
						}
					} else {
						System.err.println("Loading Base/Terms  Syntax error at line :" + line);
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


	/**
     * Calculate the Conversion Rate for the Base and Terms pair and a base Value..
     * <p>
     * This method firstly checks that the base and the terms exist in the system.
     * Then the method establishes a graphSearch so that the path between the Base and the Terms currency can be found.
     * This saves a lot of code and will work across a wide variety and complex path routes.
     * The shortest conversion path is found by finding the path from base to terms that has the smallest sum of rate edges. (This is fine in practice 
     * and quick to do for this example).
     * If no path is found then a path error is issued.
     * Otherwise each progressive path stage is multiplied by the previous stage to end up with a final conversion rate.
     * This calculated rate is then applied to the input baseValue to generate the final converted value.
      *
     * @param base  currency code
     * @param terms currency code
     * @param base Value 
     * @return conversion value
     * @throws NoCurrencyConversionPathException
     */
	public Double calculateRate(String base, String terms, Double baseValue) throws NoCurrencyConversionPathException {

		// Confirm existence of input currencies
		if (!(currencySet.contains(base) && currencySet.contains(terms))) {
			throw new NoCurrencyConversionPathException(base, terms);
		}
		
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

}
