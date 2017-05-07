package au.com.crowtech;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class RateConverterTest {

	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

	
	@Before
	public void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@After
	public void cleanUpStreams() {
	    System.setOut(null);
	    System.setErr(null);
	}

	@Test
	public void loading()
	{
		String inputStr = String.join(
		           "AAABBB=2.0\n"
		         , "BBBCCC=4.0\n"
		         , "CCCDDD=0.5"
		);
		InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
		RateConverter rateConverter = new RateConverter(stream,true); 
		
		try {
			Assert.assertTrue(rateConverter.calculateRate("AAA", "BBB", 1.0).equals(2.0));
			Assert.assertTrue(rateConverter.calculateRate("BBB", "AAA", 1.0).equals(0.5));
			Assert.assertTrue(rateConverter.calculateRate("BBB", "CCC", 1.0).equals(4.0));
			Assert.assertTrue(rateConverter.calculateRate("CCC", "BBB", 1.0).equals(0.25));
			Assert.assertTrue(rateConverter.calculateRate("CCC", "DDD", 1.0).equals(0.5));
			Assert.assertTrue(rateConverter.calculateRate("DDD", "CCC", 1.0).equals(2.0));
			
		} catch (NoCurrencyConversionPathException e) {
			Assert.assertTrue(e.getMessage(), false);
		}
	}
	
	@Test
	public void testMultiPathRate()
	{
		String inputStr = String.join(
		           "AAABBB=2.0\n"
		         , "BBBCCC=4.0\n"
		         , "CCCDDD=0.5"
		);
		InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
		RateConverter rateConverter = new RateConverter(stream,true); 
		
		try {
			Assert.assertTrue(rateConverter.calculateRate("AAA", "CCC", 1.0).equals(8.0));
			Assert.assertTrue(rateConverter.calculateRate("CCC", "AAA", 1.0).equals(0.125));
			Assert.assertTrue(rateConverter.calculateRate("AAA", "DDD", 1.0).equals(4.0));
			
		} catch (NoCurrencyConversionPathException e) {
			Assert.assertTrue(e.getMessage(), false);
		}
	}
	
	@Test
	public void testMissingPathRate()
	{
		String inputStr = String.join(
		           "AAABBB=2.0\n"
		         , "CCCDDD=0.5"
		);
		InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
		RateConverter rateConverter = new RateConverter(stream,true); 
		
		try {
			rateConverter.calculateRate("AAA", "CCC", 1.0);
			Assert.assertTrue(false);
		} catch (NoCurrencyConversionPathException e) {
			Assert.assertTrue("No Currency Conversion Path exists from AAA to CCC",true);
		}
	}
	
	
	  @Test
	    public void verifiesLineSyntaxErrorInCode() {
			String inputStr = String.join(
			           "AAABBBB=2.0\n"
			         , "BBBCCC=4.0\n"
			         , "CCCDDD=0.5"
			);
			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			RateConverter rateConverter = new RateConverter(stream,true); 
			Assert.assertEquals("Loading Base/Terms  Syntax error at line :AAABBBB=2.0\n", errContent.toString());
	    }

	  @Test
	    public void verifiesLineSyntaxErrorInRate() {
			String inputStr = String.join(
			           "AAABBB=2.0\n"
			         , "BBBCCC=4.0A\n"
			         , "CCCDDD=0.5"
			);
			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			RateConverter rateConverter = new RateConverter(stream,true); 
			Assert.assertEquals("Loading Base/Terms  Syntax error at line :BBBCCC=4.0A\n", errContent.toString());
	    }
	  
	  @Test
	    public void verifiesLineSyntaxForLoading() {
			String inputStr = 
			         "BBBCCC=40\n";

			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			RateConverter rateConverter = new RateConverter(stream,true); 
			Assert.assertEquals("Added BBB -> CCC at 40.0 and inverse 0.025\n", outContent.toString());
	    }
	
}