package au.com.crowtech;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.beust.jcommander.JCommander;

public class CommandLineTest {

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
	public void commandLineTest()
	{
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName("Test");
		jc.setColumnSize(80);
		String[] args = {"-q","-e","AUD","1.0","in","USD"};
		jc.parse(args);

		try {
			app.run();
			Assert.assertEquals("AUD 1.00 = USD 0.84\n", outContent.toString());
		} catch (IOException e) {
			Assert.assertTrue("Error in running App",false);
		}
		
	}
	
	@Test
	public void commandLineSyntaxBaseErrorTest()
	{
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName("Test");
		jc.setColumnSize(80);
		String[] args = {"-q","-e","AUDX","1.0","in","USD"};
		jc.parse(args);

		try {
			app.run();
			Assert.assertEquals("Syntax should be <ccy1> <amount1> in <ccy2> e.g AUD 100.00 in USD\nType 'exit' to quit shell.\n", errContent.toString());
		} catch (IOException e) {
			Assert.assertTrue("Error in running App",false);
		}
		
	}
	
	@Test
	public void commandLineSyntaxTermsErrorTest()
	{
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName("Test");
		jc.setColumnSize(80);
		String[] args = {"-q","-e","AUD","1.0","in","USA"};
		jc.parse(args);

		try {
			app.run();
			Assert.assertEquals("Unable to find rate for AUD/USA\n", errContent.toString());
		} catch (IOException e) {
			Assert.assertTrue("Error in running App",false);
		}
		
	}
	
	@Test
	public void commandLineSelfTest()
	{
		App app = new App();
		JCommander jc = JCommander.newBuilder().addObject(app).build();

		jc.setProgramName("Test");
		jc.setColumnSize(80);
		String[] args = {"-q","-e","AUD","1.0","in","AUD"};
		jc.parse(args);

		try {
			app.run();
			Assert.assertEquals("AUD 1.00 = AUD 1.00\n", outContent.toString());
		} catch (IOException e) {
			Assert.assertTrue("Error in running App",false);
		}
		
	}
}
