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

		final String message = ZNotNull.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t);
		throw new IllegalArgumentException(format);
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

	public static void validatedZStartWith(final Object object, final Field field) {
		final ZStartWith startWidh = field.getAnnotation(ZStartWith.class);
		if (startWidh == null) {
			return;
		}

		final Class<?> type = field.getType();
		if(!type.getCanonicalName().equals(String.class.getCanonicalName())) {
			throw new IllegalArgumentException("@" + ZStartWith.class.getSimpleName() + " 只能用于 String类型,当前用于字段[" + field.getName() + "]");
		}

		field.setAccessible(true);
		try {
			final Object value = field.get(object);
			if (value == null) {
				throw new IllegalArgumentException(
						"@" + ZStartWith.class.getSimpleName() + " 字段[" + field.getName() + "]不能为null");
			}
			final String v2 = String.valueOf(value);
			if (v2.isEmpty()) {
				throw new IllegalArgumentException(
						"@" + ZStartWith.class.getSimpleName() + " 字段[" + field.getName() + "]不能为empty");
			}

			final String prefix = startWidh.prefix();
			final boolean startsWith = v2.startsWith(prefix);
			if (!startsWith) {

				final String message = ZStartWith.MESSAGE;
				final String t = object.getClass().getSimpleName() + "." + field.getName();

				final String format = String.format(message, t, prefix);

				throw new IllegalArgumentException(format);
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
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, min, minFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(minFiledValue)) < min) {
					throwZMinMessage(object, field, min, minFiledValue);
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

	static void throwZMaxMessage(final Object object, final Field field, final double max, final Object maxFiledValue) {
		final String message = ZMax.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t, max, maxFiledValue);
		throw new IllegalArgumentException(format);
	}

	static void throwZMinMessage(final Object object, final Field field, final double min, final Object minFiledValue) {
		final String message = ZMin.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t, min, minFiledValue);
		throw new IllegalArgumentException(format);
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
				// 用 @ZNotNull的提示信息
				final String message = ZNotNull.MESSAGE;
				final String t = object.getClass().getSimpleName() + "." + field.getName();
				final String format = String.format(message, t);
				throw new IllegalArgumentException(format);
			}

			if (value instanceof List || value instanceof Set) {
				final Collection collection = (Collection) value;
				if (collection.isEmpty()) {
					throwZNotEmptyError(object, field);
				}
			} else if (value instanceof Map) {
				final Map map = (Map) value;
				if (map.isEmpty()) {
					throwZNotEmptyError(object, field);
				}
			} else if (value instanceof String) {
				final String string = (String) value;
				if (string.isEmpty()) {
					throwZNotEmptyError(object, field);
				}
			}

		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void throwZNotEmptyError(final Object object, final Field field) {
		final String message = ZNotEmtpy.MESSAGE;
		final String t = object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t);
		throw new IllegalArgumentException(format);
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
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(Short.class.getCanonicalName())) {
				if (Short.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(Integer.class.getCanonicalName())) {
				if (Integer.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
				}
			} else if (canonicalName.equals(Long.class.getCanonicalName())) {
				if (Long.valueOf(String.valueOf(minFiledValue)) > max) {
					throwZMaxMessage(object, field, max, minFiledValue);
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

}
