package com.vo.scanner;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.omg.PortableServer.POAPackage.ObjectAlreadyActiveHelper;

import com.vo.core.ValidatedException;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;
import com.vo.validator.ZStartWith;

/**
 *	验证器
 *
 * @author zhangzhen
 * @date 2023年10月15日
 *
 */
public class ZValidator {

	public static void validatedZNotNull(final Object object, final Field field) {
		final ZNotNull nn = field.getAnnotation(ZNotNull.class);
		if (nn == null) {
			return;
		}

		final Object v = getFieldValue(object, field);
		if (v != null) {
			return;
		}

		throwZNotNullException(object, field);
	}

	public static void validatedZStartWith(final Object object, final Field field) {
		final ZStartWith startWidh = field.getAnnotation(ZStartWith.class);
		if (startWidh == null) {
			return;
		}

		final Class<?> type = field.getType();
		if (!type.getCanonicalName().equals(String.class.getCanonicalName())) {
			throw new ValidatedException(
					"@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]");
		}

		try {
			field.setAccessible(true);
			final Object value = field.get(object);
			if (value == null) {
				throwZNotNullException(object, field);
			}

			final String v2 = String.valueOf(value);
			if (v2.isEmpty()) {
				throwZNotEmptyException(object, field);
			}

			final String prefix = startWidh.prefix();
			final boolean startsWith = v2.startsWith(prefix);
			if (!startsWith) {

				final String message = ZStartWith.MESSAGE;
				final String t = object.getClass().getSimpleName() + "." + field.getName();

				final String format = String.format(message, t, prefix);

				throw new ValidatedException(format);
			}

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void validatedZMin(final Object object, final Field field) {
		final ZMin zMin = field.getAnnotation(ZMin.class);
		if (zMin == null) {
			return;
		}

		final double min = zMin.min();

		try {
			field.setAccessible(true);
			final Object minFiledValue = field.get(object);
			final String canonicalName = minFiledValue.getClass().getCanonicalName();
			if (canonicalName.equals(Byte.class.getCanonicalName())) {
				if (Byte.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, (byte) min, minFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, (short) min, minFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, (int) min, minFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, (long) min, minFiledValue);
				}
			} else if (canonicalName.equals(Float.class.getCanonicalName())) {
				if (Float.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(Double.class.getCanonicalName())
					&& (Double.valueOf(String.valueOf(minFiledValue)) < min)) {
				throwZMinMessage(object, field, min, minFiledValue);
			} else if (canonicalName.equals(BigInteger.class.getCanonicalName())) {
				final BigInteger bi = (BigInteger) minFiledValue;
				if (bi.doubleValue() < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(BigDecimal.class.getCanonicalName())) {
				final BigDecimal bd = (BigDecimal) minFiledValue;
				if (bd.doubleValue() < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicInteger.class.getCanonicalName())) {
				final AtomicInteger ai = (AtomicInteger) minFiledValue;
				if (ai.doubleValue() < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicLong.class.getCanonicalName())) {
				final AtomicLong al = (AtomicLong) minFiledValue;
				if (al.doubleValue() < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			}
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void validatedZNotEmpty(final Object object, final Field field) {
		final ZNotEmtpy nn = field.getAnnotation(ZNotEmtpy.class);
		if (nn == null) {
			return;
		}

		field.setAccessible(true);
		try {
			final Object value = field.get(object);
			if (value == null) {
				throwZNotNullException(object, field);
			}

			if (value instanceof List || value instanceof Set) {
				if (((Collection) value).isEmpty()) {
					throwZNotEmptyException(object, field);
				}
			} else if (value instanceof Map) {
				if (((Map) value).isEmpty()) {
					throwZNotEmptyException(object, field);
				}
			} else if (value instanceof String) {
				// 此处不内联，防止自动保存 两个条件放在了一个if里，导致后续添加else分支时混乱
				final String string = (String) value;
				if (string.isEmpty()) {
					throwZNotEmptyException(object, field);
				}
			}

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	private static void throwZNotEmptyException(final Object object, final Field field) {
		final String message = ZNotEmtpy.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t);
		throw new ValidatedException(format);
	}

	public static void validatedZMax(final Object object, final Field field) {
		final ZMax zMax = field.getAnnotation(ZMax.class);
		if (zMax == null) {
			return;
		}

		final double max = zMax.max();

		try {
			field.setAccessible(true);
			final Object minFiledValue = field.get(object);
			final String canonicalName = minFiledValue.getClass().getCanonicalName();
			if (canonicalName.equals(Byte.class.getCanonicalName())) {
				if (Byte.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, (byte) max, minFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, (short) max, minFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, (int) max, minFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, (long) max, minFiledValue);
				}
			} else if (canonicalName.equals(Float.class.getCanonicalName())) {
				if (Float.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(Double.class.getCanonicalName())
					&& (Double.valueOf(String.valueOf(minFiledValue)) > max)) {
				throwZMaxMessage(object, field, max, minFiledValue);
			} else if (canonicalName.equals(BigInteger.class.getCanonicalName())) {
				final BigInteger bi = (BigInteger) minFiledValue;
				if (bi.doubleValue() > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(BigDecimal.class.getCanonicalName())) {
				final BigDecimal bd = (BigDecimal) minFiledValue;
				if (bd.doubleValue() > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicInteger.class.getCanonicalName())) {
				final AtomicInteger ai = (AtomicInteger) minFiledValue;
				if (ai.doubleValue() > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(AtomicLong.class.getCanonicalName())) {
				final AtomicLong al = (AtomicLong) minFiledValue;
				if (al.doubleValue() > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			}
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void validatedAll(final Object object, final Field field) {
		validatedZNotNull(object, field);
		validatedZNotEmpty(object, field);
		validatedZMin(object, field);
		validatedZMax(object, field);
		validatedZStartWith(object, field);

	}

	static Object getFieldValue(final Object object, final Field field) {
		try {
			field.setAccessible(true);
			final Object v = field.get(object);
			return v;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void throwZMinMessage(final Object object, final Field field, final Object min,
			final Object minFiledValue) {
		final String message = ZMin.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t, min, minFiledValue);
		throw new ValidatedException(format);
	}


	private static void throwZNotNullException(final Object object, final Field field) {
		final String message = ZNotNull.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t);
		throw new ValidatedException(format);
	}

	private static void throwZMaxMessage(final Object object, final Field field, final Object max, final Object maxFiledValue) {
		final String message = ZMax.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t, max, maxFiledValue);
		throw new ValidatedException(format);
	}

}
