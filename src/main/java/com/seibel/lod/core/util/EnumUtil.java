package com.seibel.lod.core.util;

import java.io.InvalidObjectException;

/**
 * Methods related to handling and using enums.
 *
 * @author James Seibel
 * @version 2022-6-6
 */
public class EnumUtil
{
	/**
	 * Attempts to find the enum value with the given name
	 * (ignoring case).
	 *
	 * @param enumType The class of the enum of parse
	 * @param enumName the string value to parse
	 * @param <T> The Enum type to parse
	 * @throws InvalidObjectException if no enum exists with the given enumName
	 */
	public static <T extends Enum<T>> T parseEnumIgnoreCase(Class<T> enumType, String enumName) throws InvalidObjectException
	{
		// attempt to find an enum with enumName
		for (T enumValue : enumType.getEnumConstants())
		{
			if (enumValue.name().equalsIgnoreCase(enumName))
			{
				return enumValue;
			}
		}
		
		// no enum found
		throw new InvalidObjectException("No Enum of type [" + enumType.getSimpleName() + "] exists with the name [" + enumName + "]. Possible enum values are: [" + createEnumCsv(enumType) + "]" );
	}
	
	/**
	 * Returns a comma separated list of all possible enum values
	 * for the given enumType. <Br><Br>
	 *
	 * Example output: <Br>
	 * "NEAR, FAR, NEAR_AND_FAR"
	 */
	public static <T extends Enum<T>> String createEnumCsv(Class<T> enumType)
	{
		StringBuilder str = new StringBuilder();
		T[] enumValues = enumType.getEnumConstants();
		
		for (int i = 0; i < enumValues.length; i++)
		{
			if (i == 0)
			{
				// the first value doesn't need a comma
				str.append(enumValues[i].name());
			}
			else
			{
				str.append(", ").append(enumValues[i].name());
			}
		}
		
		return str.toString();
	}
	
	/** Returns true if both enums contain the same values. */
	public static <Ta extends Enum<Ta>, Tb extends Enum<Tb>> EnumComparisonResult compareEnumsByValues(Class<Ta> alphaEnum, Class<Tb> betaEnum)
	{
		Ta[] alphaValues = alphaEnum.getEnumConstants();
		Tb[] betaValues = betaEnum.getEnumConstants();
		
		// compare the number of enum values
		if (alphaValues.length != betaValues.length)
		{
			return new EnumComparisonResult(false, createFailMessageHeader(alphaEnum, betaEnum) + "the enums have [" + alphaValues.length + "] and [" + betaValues.length + "] values respectively.");
		}
		
		// check that each value exists in both enums
		for(Ta alphaVal : alphaValues)
		{
			boolean valueFoundInBothEnums = false;
			for(Tb betaVal : betaValues)
			{
				if (alphaVal.name().equals(betaVal.name()))
				{
					valueFoundInBothEnums = true;
					break;
				}
			}
			
			if (!valueFoundInBothEnums)
			{
				// an enum value wasn't found
				return new EnumComparisonResult(false, createFailMessageHeader(alphaEnum, betaEnum) + "the enum value [" + alphaVal.name() + "] wasn't found in [" + betaEnum.getSimpleName() + "].");
			}
		}
		
		// every enum value is the same
		return new EnumComparisonResult(true, "");
	}
	/** helper method */
	public static <Ta extends Enum<Ta>, Tb extends Enum<Tb>> String createFailMessageHeader(Class<Ta> alphaEnum, Class<Tb> betaEnum)
	{
		return "The enums [" + alphaEnum.getSimpleName() + "] and [" + betaEnum.getSimpleName() + "] aren't equal: ";
	}
	/** helper object */
	public static class EnumComparisonResult
	{
		public final boolean success;
		public final String failMessage;
		
		public EnumComparisonResult(boolean newSuccess, String newFailMessage)
		{
			this.success = newSuccess;
			this.failMessage = newFailMessage;
		}
	}
	
}