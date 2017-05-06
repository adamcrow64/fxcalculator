package au.com.crowtech;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Precision;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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
 * @author
 *      Adam Crow (adamcrow63@Gmail.com)
 */
public class App {
	
	private static final String PROMPT = "%> ";
	
	private static final String REGEX_BASE  = "[A-Z]{3}"; 
	private static final String REGEX_TERMS = "[A-Z]{3}";
	private static final String REGEX_NUMBER_VALUE = "[-+]?[0-9]*\\.[0-9]+|[0-9]+";

	HipsterDirectedGraph<String,Double> ratesGraph = null;  // This graph is used to store all the currency pairs
	
    @Option(name="-q",usage="disable header output for quiet mode")
    private boolean quiet=false;

	
    @Option(name="-f",usage="Load in a file containing the list of currency base/terms and rates")
 //   private File fileBaseTerms = new File(getClass().getClassLoader().getResource("base_terms.txt").getFile());
    private String fileBaseTerms = null;
 
    // receives direct command line parameters than options
    @Argument
    private List<String> commandStrings = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        new App().doMain(args);
    }

    public void doMain(String[] args) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        
        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // you can parse additional arguments if you want.
            // parser.parseArgument("more","args");

            loadBaseTerms(fileBaseTerms);
            
            // after parsing arguments, you should check
            // if enough arguments are given.
            if( commandStrings.isEmpty() ) {
                processCommandLines();
            } else {
                // access non-option arguments
                System.out.println("direct commandLines are:");
                for( String s : commandStrings )
                    System.out.println(s);

            }

        } catch( CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java -jar faxcalculator.jar [options...] commandLines...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java -jar fxcalculator.jar");

            return;
        }

  
     }
    
    private void processCommandLines()
    {
   
        try {
            Scanner scanner = new Scanner(System.in);
            String regex = "("+REGEX_BASE+")\\s+("+REGEX_NUMBER_VALUE+")\\s+in\\s+("+REGEX_TERMS+")";
            Pattern p = Pattern.compile(regex);
            
            System.out.print(PROMPT);
            while ((scanner.hasNextLine())) {
            	String line = scanner.nextLine();
            	Matcher matcher = p.matcher(line);
                if(matcher.find()) {
  	        		String base = matcher.group(1);
  	        		String terms = matcher.group(3);
  	        		Double baseValue = Double.parseDouble(matcher.group(2));
  	        		Double result = calculateRate(base,terms,baseValue);
  	        		Double roundedResult = Precision.round(result,2);
  	        		DecimalFormat df = new DecimalFormat("#.##");
  	        		System.out.println(base+" "+ df.format(baseValue)+" = "+terms+" "+ df.format(roundedResult));
  	        	} else {
  	        		// display usage
  	        	}
                System.out.print(PROMPT);
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
  private void loadBaseTerms(String fileBaseTermsPath) throws FileNotFoundException {
	  File fileBaseTerms = null;
	  
	  if (fileBaseTermsPath == null) {
		  fileBaseTerms = new File(getClass().getClassLoader().getResource("base_terms.txt").getFile());
	  } else {
		  fileBaseTerms = new File(fileBaseTermsPath);
	  }
	  
		// Create a simple weighted directed graph with Hipster where
		// vertices are Strings and edge values are just doubles
		
		GraphBuilder<String, Double> gb =    GraphBuilder.<String,Double>create();
	     


	  Scanner sc=new Scanner(fileBaseTerms);

	  try {

          Pattern p = Pattern.compile("("+REGEX_BASE+")("+REGEX_TERMS+")\\=("+REGEX_NUMBER_VALUE+")");
          
         while (sc.hasNextLine()) {
              String line = sc.nextLine();
              System.out.println(line);
  	        	Matcher matcher = p.matcher(line);
  	        	while(matcher.find()) {
  	        		String base = matcher.group(1);
  	        		String terms = matcher.group(2);
  	        		Double rate = Double.parseDouble(matcher.group(3));
  	        		// Add pair
  	        		System.out.println("Adding "+base+" -> "+terms+" at "+rate);
  	        		gb.connect(base).to(terms).withEdge(rate);
  	        		// invert rate and add reverse pair
  	        		if (rate>0.0) {
  	        			Double invRate = 1.0/rate;
  	        			gb.connect(terms).to(base).withEdge(invRate);
  	 	        		System.out.println("Adding "+terms+" -> "+base+" at "+invRate);

  	        		}
  	          }             
  	       }


      } catch (Exception ex) {
          ex.printStackTrace();
      } 
	  finally {
		  sc.close();
		  ratesGraph =  gb.createDirectedGraph();
	  }
  }
  
   private Double calculateRate(String base, String terms, Double baseValue) {

	// Create the search problem. For graph problems, just use
	// the GraphSearchProblem util class to generate the problem with ease.
	SearchProblem<Double, String, WeightedNode<Double, String, Double>> p = GraphSearchProblem
	                           .startingFrom(base)
	                           .in(ratesGraph)
	                           .takeCostsFromEdges()
	                           .build();
	                           
	// Search the shortest path from "AUD" to "JPY" use smallest conversions
	SearchResult result = Hipster.createDijkstra(p).search(terms);
		
	LinkedList<Double> recoverActionPath = (LinkedList<Double>) Algorithm.recoverActionPath(result.getGoalNode());
	LinkedList<String> recoverStatePath = (LinkedList<String>) Algorithm.recoverStatePath(result.getGoalNode());
	Double conversionRate = recoverActionPath.stream().reduce(1d, (a, b) -> a * b);
	
	Double conversionValue = baseValue * conversionRate;
	
  //  System.out.println("CurrencyPath = "+recoverStatePath+" with conversion = "+conversionRate+" with final value= "+conversionValue+" "+terms);
        
	return conversionValue;
  }
}