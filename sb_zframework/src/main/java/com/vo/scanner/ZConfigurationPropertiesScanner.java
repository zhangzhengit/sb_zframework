package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
import com.vo.conf.ZProperties;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.validator.StartupException;
import com.vo.validator.TypeNotSupportedExcpetion;
import com.vo.validator.ZType;
import com.vo.validator.ZValidator;

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

	/**
	 * 	List和Set中根据[i]取值的最大值，从[0]开始：
	 *  0 1 2 3...最大支持到此值
	 */
	public static final int PROPERTY_INDEX = 1520;
	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate(final String... packageName) throws Exception {

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

	private static void findValueAndSetValue(final String prefix, final Object object, final Field field) throws Exception {
		final PropertiesConfiguration p = ZProperties.getInstance();
		final Class<?> type = field.getType();

		final AtomicReference<String> keyAR = new AtomicReference<>();
		final String key = prefix + field.getName();
		if (p.containsKey(key)) {
			keyAR.set(key);

			setValueByType(object, field, p, type, keyAR);

			return;
		}

		// 无 java 字段直接对应的 配置项,则 把[orderCount]转为[order.count]再试
		final String convert = convert(key);
		keyAR.set(convert);
		if (!p.containsKey(convert)) {
			// XXX 上面一行!contains逻辑待定，能走到此的list set map 都是直接匹配k匹配不到需要特殊处理取k的

			if (field.getType().getCanonicalName().equals(Map.class.getCanonicalName())) {
				setMap(object, field, p, key);
			} else if (field.getType().getCanonicalName().equals(List.class.getCanonicalName())) {
				// FIXME 2023年11月9日 上午12:13:59 zhanghen: 支持三种类型要支持什么类型

				final ZType zType = field.getAnnotation(ZType.class);
				if (zType == null) {
					throw new StartupException(object.getClass().getSimpleName() + "." + field.getName() + " 缺少@"
							+ ZType.class.getSimpleName() + "注解");
				}

				final boolean contains = LIST_TYPE.contains(zType.type().getCanonicalName());
				if (!contains && zType.type().getCanonicalName().startsWith("java")) {
					throw new StartupException(object.getClass().getSimpleName() + "." + field.getName() + " @"
							+ ZType.class.getSimpleName() + "的类型不支持,type=" + zType.type().getSimpleName() + ",支持类型为" + LIST_TYPE);
				}

				if (!contains) {
					final Field[] ztfs = zType.type().getDeclaredFields();
					for (final Field f : ztfs) {
						final boolean contains2 = LIST_TYPE.contains(f.getType().getCanonicalName());
						if (!contains2) {
							throw new StartupException("@" + ZType.class.getSimpleName() + ".type["
									+ zType.type().getSimpleName() + "] 中的字段[" + f.getType().getCanonicalName() + " "
									+ f.getName() + "]类型不支持,支持字段类型为" + LIST_TYPE);
						}
					}
				}

				setList(object, field, p, key);
			} else if (field.getType().getCanonicalName().equals(Set.class.getCanonicalName())) {
				setSet(object, field, p, key);
			}
			// 把[orderCount]转为[order.count]后，仍无对应的配置项，
			// 则看 是否有ZNotNull,有则抛异常
//			ZValidator.validatedZNotNull(object, field);

			ZValidator.validatedAll(object, field);

		} else {
			setValueByType(object, field, p, type, keyAR);
		}

	}

	public static final
		ImmutableList<String> LIST_TYPE = ImmutableList.copyOf(
					Lists.newArrayList(
								Byte.class.getCanonicalName(),
								Short.class.getCanonicalName(),
								Integer.class.getCanonicalName(),
								Long.class.getCanonicalName(),
								Float.class.getCanonicalName(),
								Double.class.getCanonicalName(),
								Character.class.getCanonicalName(),
								Boolean.class.getCanonicalName(),
								String.class.getCanonicalName()
							)

				);



	private static void setSet(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) {

		// 从1-N个[i]
		final Set<Object> set = Sets.newLinkedHashSet();
		for (int i = 1; i <= PROPERTY_INDEX + 1; i++) {
			final String suffix = "[" + (i - 1) + "]";
			final Iterator<String> sk = p.getKeys(key + suffix);
			while (sk.hasNext()) {
				final String xa = sk.next();
				// FIXME 2023年10月18日 下午8:56:51 zhanghen: XXX Set和Map是否校验重复？新增一个校验重复的注解？
				final boolean add = set.add(p.getString(xa));
			}
		}
		System.out.println("set = " + set);
		try {
			field.setAccessible(true);
			// set为null，为了走下面的校验 @ZNotNull的流程
			field.set(object, set.isEmpty() ? null : set);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void setList(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) throws Exception {

		// 从1-N个[i]
		final List<Object> list = Lists.newArrayList();


		final boolean isJavaType = LIST_TYPE.contains(field.getAnnotation(ZType.class).type().getCanonicalName());

		final boolean isUserType = !isJavaType && !field.getAnnotation(ZType.class).type().getCanonicalName().startsWith("java");

		for (int i = 1; i <= PROPERTY_INDEX + 1; i++) {


			final String suffix = "[" + (i - 1) + "]";
			final String fullKey = key + suffix;
			final Iterator<String> sk = p.getKeys(fullKey);

			if (sk.hasNext()) {
				final String xa = sk.next();
				final String xaValue = p.getString(xa);
				if (isJavaType) {
					list.add(xaValue);
				} else if (isUserType) {
					final Object newInstance = newInstance(field);
					setValue(newInstance, fullKey, xa, xaValue);

					while (sk.hasNext()) {
						final String xa2 = sk.next();
						final String xaValue2 = p.getString(xa2);
						setValue(newInstance, fullKey, xa2, xaValue2);
					}
					list.add(newInstance);
				}
			} else {
				// 为空也add null，占一个位置，为了这种需求：
				// [0]=A [2]=C 就是不配置第二个位置让其为空，
				// 这样取的时候list.get(1) 取得的第二个就是null
				list.add(null);
			}
		}

		// 最后去除后面的所有的null
		int i = list.size() - 1;
		while (i > 1) {
			if (list.get(i - 1) == null) {
				i--;
			} else {
				break;
			}
		}

		final List<Object> subList = i <= 0 ? null : list.subList(0, i);

		System.out.println("list = " + subList);

		try {
			field.setAccessible(true);
			// set为null，为了走下面的校验 @ZNotNull的流程
			field.set(object, CollUtil.isEmpty(subList) ? null : subList);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void setValue(final Object newInstance, final String fullKey, final String key,
			final String value) throws Exception {
		final String fieldName = key.replace(fullKey + ".", "");

		try {
			final Field field = newInstance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);

			if (field.getType().getCanonicalName().equals(Byte.class.getCanonicalName())) {
				field.set(newInstance, Byte.parseByte(value));
			} else if (field.getType().getCanonicalName().equals(Short.class.getCanonicalName())) {
				field.set(newInstance, Short.parseShort(value));
			} else if (field.getType().getCanonicalName().equals(Integer.class.getCanonicalName())) {
				field.set(newInstance, Integer.parseInt(value));
			} else if (field.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
				field.set(newInstance, Long.parseLong(value));
			} else if (field.getType().getCanonicalName().equals(Float.class.getCanonicalName())) {
				field.set(newInstance, Float.parseFloat(value));
			} else if (field.getType().getCanonicalName().equals(Double.class.getCanonicalName())) {
				field.set(newInstance, Double.parseDouble(value));
			} else if (field.getType().getCanonicalName().equals(Boolean.class.getCanonicalName())) {
				// FIXME 2023年11月9日 下午1:53:34 zhanghen: TODO 其他类型继续提示
				final String bo = String.valueOf(value);
				if (!"true".equalsIgnoreCase(bo) && !"false".equalsIgnoreCase(bo)) {
					// Boolean.parseBoolean 也无需校验，但仍提示
					throw new ConfigurationPropertiesParameterException(
							newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
									+ Boolean.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为true或false");
				}
				field.set(newInstance, Boolean.parseBoolean(value));
			} else if (field.getType().getCanonicalName().equals(Character.class.getCanonicalName())) {
				if (value.length() > 1) {
					throw new ConfigurationPropertiesParameterException(
							newInstance.getClass().getSimpleName() + "." + fieldName + " 为 "
									+ Character.class.getSimpleName() + " 类型，当前参数为 " + value + "，请检查代码参数类型或修改参数值为一个字符");
				}
				field.set(newInstance, Character.valueOf(value.charAt(0)));
			} else if (field.getType().getCanonicalName().equals(String.class.getCanonicalName())) {
				// String 无需校验直接赋值
				field.set(newInstance, value);
			} else {
				throw new TypeNotSupportedExcpetion(fieldName);
			}

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static Object newInstance(final Field field)  {
		final ZType zType = field.getAnnotation(ZType.class);
		final Class<?> type2 = zType.type();
		System.out.println("type = " + type2);
		Object newInstance = null;
		try {
			newInstance = type2.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		System.out.println("newInstance = " + newInstance);

		return newInstance;
	}

	private static void setMap(final Object object, final Field field, final PropertiesConfiguration p,
			final String key) {
		final Iterator<String> keys = p.getKeys(key);
		final Map<String, Object> map = new HashMap<>(16, 1F);
		while (keys.hasNext()) {
			final String k = keys.next();

			final String kName = StrUtil.removeAll(k, key + ".");

			final String value = p.getString(k);
			map.put(kName, value);
		}

		try {
			field.setAccessible(true);
			// set为null，为了走后面的校验ZNotNull流程
			field.set(object, map.isEmpty() ? null : map);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
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
		ZValidator.validatedAll(object, field);
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

	public static Set<Class<?>> scanPackage(final String... packageName) {
		LOG.info("开始扫描类,scanPackage={}", Arrays.toString(packageName));
		final HashSet<Class<?>> rs = Sets.newHashSet();
		for (final String p : packageName) {
			final Set<Class<?>> clsSet = ClassMap.scanPackage(p);
			rs.addAll(clsSet);

		}
		return rs;
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
