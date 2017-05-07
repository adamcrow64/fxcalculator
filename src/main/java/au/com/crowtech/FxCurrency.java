package au.com.crowtech;

import org.apache.commons.math3.util.Precision;

public class FxCurrency {
	
	static int DEFAULT_PRECISION = 2;
	
	private String code;
	private int precision;
	
	public FxCurrency(String code) {
		this(code,DEFAULT_PRECISION);
	}
	
	public FxCurrency(String code, int precision) {
		this.code = code;
		this.precision = precision;
	}
	
	public String formattedValue(Double value) {
		Double roundedResult = Precision.round(value, precision);
		    String form = "%."+precision+"f\n";
		    return String.format(form, roundedResult);
		
	}

	
	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}

	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(int precision) {
		this.precision = precision;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FxCurrency other = (FxCurrency) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		return true;
	}
	
	
}