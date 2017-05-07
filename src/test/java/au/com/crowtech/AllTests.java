package au.com.crowtech;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                LoadingTest.class,
                CommandLineTest.class,
                RateConverterTest.class})

public class AllTests {

}