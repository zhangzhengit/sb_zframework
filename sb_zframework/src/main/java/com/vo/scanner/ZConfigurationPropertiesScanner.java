package com.vo.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
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
		if (!p.containsKey(key)) {
			final String convert = convert(key);
			if (!p.containsKey(convert)) {
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
			keyAR.set(convert);
		} else {
			keyAR.set(key);
		}

		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			setValue(object, field, p.getString(keyAR.get()));
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
			setValue(object, field, p.getString(keyAR.get()).charAt(0));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(object, field, p.getBoolean(keyAR.get()));
		}
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
