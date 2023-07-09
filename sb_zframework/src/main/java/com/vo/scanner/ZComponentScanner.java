package com.vo.scanner;

import java.util.Map;
import java.util.Set;

import com.vo.anno.ZComponent;
import com.vo.aop.ZAOPScaner;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZClass;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZSingleton;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate() {

		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1();

		final ServerConfiguration serverConfiguration = ZContext.getBean(ServerConfiguration.class);

		LOG.info("开始扫描带有[{}]注解的类", ZComponent.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(serverConfiguration.getScanPackage(), ZComponent.class);
		LOG.info("扫描到带有[{}]注解的类个数={}", ZComponent.class.getCanonicalName(),zcSet.size());
		LOG.info("开始给带有[{}]注解的类创建对象",ZComponent.class.getCanonicalName());
		for (final Class<?> zc : zcSet) {
			LOG.info("开始给待有[{}]注解的类[{}]创建对象",ZComponent.class.getCanonicalName(),zc.getCanonicalName());
			final Object object = ZSingleton.getSingletonByClass(zc);
			LOG.info("给带有[{}]注解的类[{}]创建对象[{}]完成", ZComponent.class.getCanonicalName(),
					zc.getCanonicalName(), object);

			final Object newComponent = ZObjectGeneratorStarter.generate(zc);
			final ZClass proxyClass = map.get(newComponent.getClass().getSimpleName());
			if (proxyClass != null) {
				final Object newInstance = proxyClass.newInstance();
				// 放代理类
				ZContext.addBean(newComponent.getClass().getCanonicalName(), newInstance);
			} else {
				// 放原类
				ZContext.addBean(newComponent.getClass().getCanonicalName(), newComponent);
			}
		}

		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

}
