package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Sets;
import com.vo.anno.ZConfigurationProperties;
import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.conf.ZProperties;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.http.ZConfigurationPropertiesMap;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotNull;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 扫描 @ZConfigurationProperties 的类，从配置文件读取配置组长一个此类的对象
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZConfigurationPropertiesScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreateObject() {

		final Set<Class<?>> csSet = scanPackage_COM();

		for (final Class<?> cs : csSet) {

			final ZConfigurationProperties zcp = cs.getAnnotation(ZConfigurationProperties.class);
			if (zcp == null) {
				continue;
			}

			final String prefix = StrUtil.isEmpty(zcp.prefix()) ? ""
					: (zcp.prefix().endsWith(".") ? zcp.prefix() : zcp.prefix() + ".");

			final Object object = ZSingleton.getSingletonByClass(cs);
			final Field[] fs = cs.getDeclaredFields();
			for (final Field field : fs) {
				checkModifiers(cs, field);
				findValueAndSetValue(prefix, object, field);
			}

			System.out.println("ZCP-object = " + object);
			ZConfigurationPropertiesMap.put(cs, object);
		}
	}

	@SuppressWarnings("boxing")
	private static void findValueAndSetValue(final String prefix, final Object object, final Field field) {
		final PropertiesConfiguration p = ZProperties.getInstance();
		final String key = prefix + field.getName();
		System.out.println("ZCP.key = " + key);
		final Class<?> type = field.getType();

		final AtomicReference<String> keyAR = new AtomicReference<>();
		if (p.containsKey(key)) {
			keyAR.set(key);
			setValueByType(object, field, p, type, keyAR);
			return;
		}

		// 无 java 字段直接对应的 配置项,则 把[orderCount]转为[order.count]再试
		final String convert = convert(key);
		keyAR.set(convert);
		if (!p.containsKey(convert)) {
			// 把[orderCount]转为[order.count]后，仍无对应的配置项，则看 是否有校验注解

			checkZNotNull(object, field);

			// FIXME 2023年6月30日 下午10:01:54 zhanghen: check @ZMin

		}

		// FIXME 2023年6月30日 下午9:52:22 zhanghen: TODO check @ZMin
	}

	private static void setValueByType(final Object object, final Field field, final PropertiesConfiguration p,
			final Class<?> type, final AtomicReference<String> keyAR) {

		final String v1 = getStringValue(p, keyAR);


		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			setValue(object, field, v1);
		} else if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			setValue(object, field, p.getByte(keyAR.get()));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			setValue(object, field, p.getShort(keyAR.get()));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			setValue(object, field, p.getInt(keyAR.get()));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			setValue(object, field, p.getLong(keyAR.get()));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			setValue(object, field, p.getFloat(keyAR.get()));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			setValue(object, field, p.getDouble(keyAR.get()));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			setValue(object, field, v1.charAt(0));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(object, field, p.getBoolean(keyAR.get()));
		} else if (type.getCanonicalName().equals(BigInteger.class.getCanonicalName())) {
			setValue(object, field, p.getBigInteger(keyAR.get()));
		} else if (type.getCanonicalName().equals(BigDecimal.class.getCanonicalName())) {
			setValue(object, field, p.getBigDecimal(keyAR.get()));
		} else if (type.getCanonicalName().equals(AtomicInteger.class.getCanonicalName())) {
			setValue(object, field, new AtomicInteger(p.getInt(keyAR.get())));
		} else if (type.getCanonicalName().equals(AtomicLong.class.getCanonicalName())) {
			setValue(object, field, new AtomicLong(p.getInt(keyAR.get())));
		}

		// 赋值以后才可以校验
		checkZMin(object, field);

	}

	private static String getStringValue(final PropertiesConfiguration p, final AtomicReference<String> keyAR) {
		String v1 = null;
		try {
			v1 = new String(p.getString(keyAR.get()).getBytes(ZProperties.PROPERTIESCONFIGURATION_ENCODING.get()),
					Charset.defaultCharset().displayName());
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return v1;
	}

	@SuppressWarnings("boxing")
	private static void checkZMin(final Object object, final Field field) {
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

	private static void throwZMinMessage(final Object object, final Field field, final double min, final Object minFiledValue) {
		final String message = ZMin.MESSAGE;
		final String t = "@" + ZConfigurationProperties.class.getSimpleName() + " 对象 "
				+ object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t, min, minFiledValue);
		throw new IllegalArgumentException(format);
	}

	private static void checkZNotNull(final Object object, final Field field) {
		final ZNotNull nn = field.getAnnotation(ZNotNull.class);
		if (nn == null) {
			return;
		}

		final String message = ZNotNull.MESSAGE;
		final String t = "@" + ZConfigurationProperties.class.getSimpleName() + " 对象 "
				+ object.getClass().getSimpleName() + "." + field.getName();
		final String format = String.format(message, t);
		throw new IllegalArgumentException(format);
	}

	private static void setValue(final Object object, final Field field, final Object value) {
		try {
			field.setAccessible(true);
			field.set(object, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void checkModifiers(final Class<?> cs, final Field field) {
		final int modifiers = field.getModifiers();
		if (Modifier.isPublic(modifiers)) {
			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用public修饰");
		}
		if (Modifier.isStatic(modifiers)) {
			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用static修饰");
		}
		if (Modifier.isFinal(modifiers)) {
			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用final修饰");
		}
		if (Modifier.isAbstract(modifiers)) {
			throw new IllegalArgumentException("@" + ZConfigurationProperties.class.getSimpleName() + " 类 "
					+ cs.getSimpleName() + " 的字段 " + field.getName() + " 不能用abstract修饰");
		}
	}

	public static Set<Class<?>> scanPackage_COM() {
		final ZFrameworkProperties p = ZFrameworkDatasourcePropertiesLoader.getFrameworkPropertiesInstance();
		final String scanPackage = p.getScanPackage();
		LOG.info("开始扫描类,scanPackage={}", scanPackage);
		final Set<Class<?>> clsSet = ClassUtil.scanPackage(scanPackage);
		return clsSet;
	}

	/**
	 * 从 orderCount 形式的字段名称， 获取 order.count 形式的名称，
	 * 把其中的[大写字母]替换为[.小写字母]
	 *
	 * @param fieldName
	 * @return
	 *
	 */
	private static String convert(final String fieldName) {
		final StringBuilder builder = new StringBuilder(fieldName);
		final char[] ca = fieldName.toCharArray();
		for(int i= 0;i<ca.length;i++) {
			final char c = ca[i];
			if(daxie.contains(c)) {
				builder.replace(i, i + 1, "." + Character.toLowerCase(c));
			}
		}
		return builder.toString();
	}

	static HashSet<Character> daxie = Sets.newHashSet('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
}
