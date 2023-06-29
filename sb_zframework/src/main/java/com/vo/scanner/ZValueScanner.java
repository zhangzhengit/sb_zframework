package com.vo.scanner;

import java.lang.reflect.Field;
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

		final String key = zValue.name();
		final Class<?> type = field.getType();
		final PropertiesConfiguration p = ZProperties.getInstance();
		if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			try {
				final String value = p.getString(key);
				field.setAccessible(true);
				field.set(object, value);
				System.out.println("field赋值成功，field = " + field.getName()  + "\t" + "value = " + value
						+ "\t" + " object = " + object
						);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
