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


public class CurrencyPrecisionsTest {

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
		           "AAA=2 decimal places\n"
		         , "BBB=3 decimal places\n"
		         , "CCC=0 decimal places"
		);
		InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
		CurrencyPrecisions currencyPrecisions = new CurrencyPrecisions(stream,true); 
		
		Assert.assertTrue(currencyPrecisions.formattedValue("AAA", 3.14159).equals("3.14"));
		Assert.assertTrue(currencyPrecisions.formattedValue("AAA", 3.149).equals("3.15"));
		Assert.assertTrue(currencyPrecisions.formattedValue("AAA", 0.00001).equals("0.00"));		
		Assert.assertFalse(currencyPrecisions.formattedValue("AAA", 3.149).equals("3.14"));

		Assert.assertTrue(currencyPrecisions.formattedValue("BBB", 3.14159).equals("3.142"));
		Assert.assertTrue(currencyPrecisions.formattedValue("BBB", 3.14149).equals("3.141"));
		Assert.assertTrue(currencyPrecisions.formattedValue("BBB", 3.14).equals("3.140"));

		Assert.assertTrue(currencyPrecisions.formattedValue("CCC", 32.14159).equals("32"));
		Assert.assertTrue(currencyPrecisions.formattedValue("CCC", 32.51).equals("33"));
		Assert.assertTrue(currencyPrecisions.formattedValue("CCC", 0.14).equals("0"));

	}
	
	
	  @Test
	    public void verifiesLineSyntaxErrorInText() {
			String inputStr = String.join(
			           "AAA=2 decimal place\n"
			         , "BBB=3 decimal places\n"
			         , "CCC=0 decimal places"
			);
			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			new CurrencyPrecisions(stream,true); 
			Assert.assertEquals("Decimal Precisions File Syntax error at line :AAA=2 decimal place\n", errContent.toString());

	    }
	  
	  @Test
	    public void verifiesLineSyntaxErrorInPrecision() {
			String inputStr = String.join(
			           "AAA=2 decimal places\n"
			         , "BBB=3.2 decimal places\n"
			         , "CCC=0 decimal places"
			);
			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			new CurrencyPrecisions(stream,true); 
			Assert.assertEquals("Decimal Precisions File Syntax error at line :BBB=3.2 decimal places\n", errContent.toString());

	    }
	  
	  @Test
	    public void verifiesLineSyntaxErrorInCurrencyCode() {
			String inputStr = String.join(
			           "AAA=2 decimal places\n"
			         , "BBB=3 decimal places\n"
			         , "CCCC=0 decimal places"
			);
			InputStream stream = new ByteArrayInputStream(inputStr.getBytes(StandardCharsets.UTF_8));
			new CurrencyPrecisions(stream,true); 
			Assert.assertEquals("Decimal Precisions File Syntax error at line :CCCC=0 decimal places\n", errContent.toString());

	    }
}
