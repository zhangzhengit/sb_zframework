package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
import com.vo.conf.ZProperties;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;

import cn.hutool.core.collection.CollUtil;
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

	public static void scanAndCreate(final String packageName) {

		final Set<Class<?>> csSet = scanPackage(packageName).stream()
				.filter(cls -> cls.isAnnotationPresent(ZConfigurationProperties.class))
				.collect(Collectors.toSet());
		if (CollUtil.isEmpty(csSet)) {
			return;
		}

		for (final Class<?> cs : csSet) {

			final ZConfigurationProperties zcp = cs.getAnnotation(ZConfigurationProperties.class);

			final String prefix = StrUtil.isEmpty(zcp.prefix()) ? ""
					: (zcp.prefix().endsWith(".") ? zcp.prefix() : zcp.prefix() + ".");

			final Object object = ZSingleton.getSingletonByClass(cs);
			final Field[] fs = cs.getDeclaredFields();
			for (final Field field : fs) {
				checkModifiers(cs, field);
				findValueAndSetValue(prefix, object, field);
			}

			System.out.println("ZCP-object = " + object);

			ZContext.addBean(cs, object);
			ZContext.addBean(cs.getCanonicalName(), object);
		}

		for (final Class<?> cls : csSet) {
			// 如果Class有 @ZAutowired 字段，则先生成对应的的对象，然后注入进来
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZAutowired.class))
				.forEach(f -> ZAutowiredScanner.inject(cls, f));

			// 如果Class有 @ZValue 字段 ，则先给此字段注入值
			Lists.newArrayList(cls.getDeclaredFields()).stream()
				.filter(f -> f.isAnnotationPresent(ZValue.class))
				.forEach(f -> ZValueScanner.inject(cls, f));
		}
	}

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
			// 把[orderCount]转为[order.count]后，仍无对应的配置项，
			// 则看 是否有ZNotNull,有则抛异常
			ZValidator.validatedZNotNull(object, field);
		} else {
			setValueByType(object, field, p, type, keyAR);
		}

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
		ZValidator.validatedZMin(object, field);
		ZValidator.validatedZMax(object, field);
		ZValidator.validatedZNotEmpty(object, field);
		ZValidator.validatedZStartWith(object, field);
	}

	private static String getStringValue(final PropertiesConfiguration p, final AtomicReference<String> keyAR) {
		final StringJoiner joiner = new StringJoiner(",");
		try {
			final String[] stringArray = p.getStringArray(keyAR.get());
			for (final String s : stringArray) {
				final String s2 = new String(s.trim()
						.getBytes(ZProperties.PROPERTIESCONFIGURATION_ENCODING.get()),
						Charset.defaultCharset().displayName());
				joiner.add(s2);
			}
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return joiner.toString();
	}

	@SuppressWarnings("boxing")




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

	public static Set<Class<?>> scanPackage(final String packageName) {
		LOG.info("开始扫描类,scanPackage={}", packageName);
		final Set<Class<?>> clsSet = ClassMap.scanPackage(packageName);
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
		final AtomicInteger replaceCount = new AtomicInteger(0);
		for (int i = 0; i < ca.length; i++) {
			final char c = ca[i];
			if (daxie.contains(c)) {
				final int andIncrement = replaceCount.getAndIncrement();
				builder.replace(i + andIncrement, i + andIncrement + 1, "." + Character.toLowerCase(c));
			}
		}
		return builder.toString();
	}

	static HashSet<Character> daxie = Sets.newHashSet('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
}
