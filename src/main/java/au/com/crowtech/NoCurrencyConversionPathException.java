package au.com.crowtech;

public class NoCurrencyConversionPathException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	String base;
	String terms;
	
	public NoCurrencyConversionPathException(String base, String terms)
	{
		this.base = base;
		this.terms = terms;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "No Currency Conversion Path exists from " + base + " to " + terms;
	}

	public String getBaseTerms() {
		return base+"/"+terms;
	}
	
	
}
