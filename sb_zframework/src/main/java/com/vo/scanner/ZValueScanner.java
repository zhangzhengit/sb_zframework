package com.vo.scanner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Lists;
import com.vo.anno.ZComponent;
import com.vo.anno.ZComponentMap;
import com.vo.anno.ZController;
import com.vo.anno.ZValue;
import com.vo.conf.ZProperties;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;

/**
 *
 * 扫描组件中 带有 @ZValue 的字段，根据name注入配置文件中对应的value
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZValueScanner {

	public static void scan(final String packageName) {
		final Set<Class<?>> zcSet = ClassUtil.scanPackageByAnnotation(packageName, ZComponent.class);
		final Set<Class<?>> zc2Set = ClassUtil.scanPackageByAnnotation(packageName, ZController.class);

		final List<Class<?>> clist = Lists.newArrayListWithCapacity(zcSet.size() + zc2Set.size());
		clist.addAll(zcSet);
		clist.addAll(zc2Set);

		if (clist.isEmpty()) {
			return;
		}

		for (final Class<?> cls : clist) {
			final Object byName = ZComponentMap.getByName(cls.getCanonicalName());
			if (byName == null) {
				continue;
			}

			final Field[] fields = ReflectUtil.getFields(byName.getClass());
			for (final Field f2 : fields) {
				final ZValue zv2 = f2.getAnnotation(ZValue.class);
				if (zv2 == null) {
					continue;
				}

				setValue(f2, zv2, byName);
			}
		}
	}

	private static void setValue(final Field field, final ZValue zValue, final Object object) {

		final PropertiesConfiguration p = ZProperties.getInstance();
		final String key = zValue.name();
		if (!p.containsKey(key)) {
			return;
		}

		final Class<?> type = field.getType();
		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			try {
				final String v1 = p.getString(key);
				final String v2 = new String(v1.getBytes(ZProperties.PROPERTIESCONFIGURATION_ENCODING.get()),
						Charset.defaultCharset().displayName());
				setValue(field, object, v2);
			} catch (final UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

		} else if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			setValue(field, object, p.getByte(key));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			setValue(field, object, p.getShort(key));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			setValue(field, object, p.getInt(key));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			setValue(field, object, p.getLong(key));
		} else if (type.getCanonicalName().equals(BigInteger.class.getCanonicalName())) {
			setValue(field, object, p.getBigInteger(key));
		} else if (type.getCanonicalName().equals(BigDecimal.class.getCanonicalName())) {
			setValue(field, object, p.getBigDecimal(key));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			setValue(field, object, p.getBoolean(key));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			setValue(field, object, p.getDouble(key));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			setValue(field, object, p.getFloat(key));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			setValue(field, object, p.getString(key).charAt(0));
		} else {
			// FIXME 2023年7月1日 上午9:19:27 zhanghen: 继续考虑支持什么类型
			throw new IllegalArgumentException("@" + ZValue.class.getSimpleName() + " 字段 " + field.getName() + " 的类型 "
					+ field.getType().getSimpleName() + " 暂不支持");
		}
	}

	private static void setValue(final Field field, final Object object, final Object value) {
		try {
			field.setAccessible(true);
			field.set(object, value);
			System.out.println(
					"field赋值成功，field = " + field.getName() + "\t" + "value = " + value + "\t" + " object = " + object);
		} catch (IllegalArgumentException | IllegalAccessException  e) {
			e.printStackTrace();
		}
	}
}
