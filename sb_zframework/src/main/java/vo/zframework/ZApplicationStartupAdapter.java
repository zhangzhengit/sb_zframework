package vo.zframework;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import vo.log.core.ZLog2;
import vo.zframework.anno.ZAOP;
import vo.zframework.anno.ZCommandLineRunner;
import vo.zframework.anno.ZComponent;
import vo.zframework.anno.ZConfiguration;
import vo.zframework.anno.ZController;
import vo.zframework.anno.ZRestController;
import vo.zframework.anno.ZService;
import vo.zframework.bean.ZObjectGeneratorStarter;
import vo.zframework.bean.ZSingleton;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.CommonConfigurationProperties;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.configuration.properties.ZConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.event.ZApplicationEventPublisher;
import vo.zframework.exception.StartupException;
import vo.zframework.exception.ZControllerAdviceScanner;
import vo.zframework.html.ResourcesLoader;
import vo.zframework.http.HttpRequestProcessor;
import vo.zframework.http.ZServer;
import vo.zframework.scanner.ZAsyncScanner;
import vo.zframework.scanner.ZAutowiredScanner;
import vo.zframework.scanner.ZCacheScanner;
import vo.zframework.scanner.ZCommandLineRunnerScanner;
import vo.zframework.scanner.ZComponentScanner;
import vo.zframework.scanner.ZConfigurationPropertiesScanner;
import vo.zframework.scanner.ZConfigurationScanner;
import vo.zframework.scanner.ZControllerScanner;
import vo.zframework.scanner.ZHandlerInterceptorScanner;
import vo.zframework.scanner.ZSynchronouslyScanner;
import vo.zframework.scanner.ZValueScanner;
import vo.zframework.starter.ZStarter;
import vo.zframework.validator.ZCacheableValidator;
import vo.zframework.validator.ZValidator;

/**
 * ZApplication 的启动流程适配类，如需自定义或查插入代码等，覆盖本类方法
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
public class ZApplicationStartupAdapter implements ZApplicationStartupProcessor {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@Override
	public void startValidator(final ZApplicationStartupInfo startupInfo) {
		ZValidator.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void startEventPublisher(final ZApplicationStartupInfo startupInfo) {
		ZApplicationEventPublisher.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanConfigurationProperties(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationPropertiesScanner.scanAndCreate(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanConfiguration(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationScanner.scanAndCreate(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void startObjectGenerator(final ZApplicationStartupInfo startupInfo) {
		ZObjectGeneratorStarter.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanComponent(final ZApplicationStartupInfo startupInfo) {
		final Class[] cA = { ZComponent.class, ZService.class };
		for (final Class cls : cA) {
			ZComponentScanner.scanAndCreate(cls, startupInfo.getPackageNameList().toArray(new String[0]));
		}

		// FIXME 2025年1月24日 下午6:22:14 zhangzhen : 下面的parallel注释掉，重新用foreach了
		// 因为在panther x2 的armbian上会导致后面的NPE
		//		Arrays.stream(cA)
		//		.parallel()
		//		.forEach(cls -> {
		//			ZComponentScanner.scanAndCreate(cls, startupInfo.getPackageNameList().toArray(new String[0]));
		//		});
	}

	@Override
	public void scanControllerAdvice(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			ZControllerAdviceScanner.scan(startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void scanController(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			ZControllerScanner.scanAndCreateObject(startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void injectAutowired(final ZApplicationStartupInfo startupInfo) {
		final Class[] cA = { ZService.class, ZComponent.class,
				ZRestController.class,
				ZController.class,
				ZConfiguration.class, ZAOP.class };
		for (final Class cls : cA) {
			ZAutowiredScanner.inject(cls, startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void injectValue(final ZApplicationStartupInfo startupInfo) {
		ZValueScanner.inject(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void validatedCache(final ZApplicationStartupInfo startupInfo) {
		ZCacheableValidator.validated(startupInfo.getPackageNameList().toArray(new String[0]));
		ZCacheScanner.scanAndValidate();
	}

	@Override
	public void setStaticPath(final ZApplicationStartupInfo startupInfo) {
		final ServerConfigurationProperties serverConfiguration = ZSingleton
				.getSingletonByClass(ServerConfigurationProperties.class);
		if (STU.isNotEmpty(serverConfiguration.getStaticPath())) {
			System.setProperty(ResourcesLoader.STATIC_RESOURCES_PROPERTY_NAME, serverConfiguration.getStaticPath());
		}
	}

	@Override
	public void printZConfigurationProperties(final ZApplicationStartupInfo startupInfo) {
		if (ZContext.getBean(ServerConfigurationProperties.class).getPrintConfigurationProperties()) {

			//			LOG.info("开始打印@{}配置类信息", ZConfigurationProperties.class.getSimpleName());

			final Collection<Object> bs = ZContext.all().values();
			for (final Object bean : bs) {
				if (!bean.getClass().isAnnotationPresent(ZConfigurationProperties.class)) {
					continue;
				}

				final Field[] fs = bean.getClass().getDeclaredFields();
				for (final Field f : fs) {
					try {
						f.setAccessible(true);
						final Object value = f.get(bean);
						// FIXME 2023年11月8日 下午9:08:00 zhanghen: XXX 是否会打印出某些敏感信息？
						// 新增注解标记下不打印？似乎没必要
						//						LOG.info("配置项{}.{}={}", bean.getClass().getSimpleName(), f.getName(), value);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}

			//			LOG.info("打印@{}配置类信息完成", ZConfigurationProperties.class.getSimpleName());
		}

	}

	@Override
	public void aftertAutowiredInject(final ZApplicationStartupInfo startupInfo) {
		ZAutowiredScanner.after();
	}

	@Override
	public void scanHandlerInterceptor(final ZApplicationStartupInfo startupInfo) {
		ZHandlerInterceptorScanner.scan();
	}

	@Override
	public void runCommandLineRunner(final ZApplicationStartupInfo startupInfo) throws Exception {
		for (final Object zclr : ZCommandLineRunnerScanner.scan(startupInfo.getPackageNameList().toArray(new String[0]))) {
			((ZCommandLineRunner) zclr).run(startupInfo.getArgs());
		}
	}

	@Override
	public void loadStarter() {

//		LOG.info("开始初始化starter");
		final ClassLoader classLoader = ZApplication.class.getClassLoader();
		try {
			final CommonConfigurationProperties common = ZContext.getBean(CommonConfigurationProperties.class);
//			LOG.info("/resources/META-INF/下指定的启动文件名称={}", common.getStarterName());
			final Enumeration<URL> resources = classLoader.getResources("META-INF/" + common.getStarterName());

			int c = 0;
			while (resources.hasMoreElements()) {
				c++;
				final URL url = resources.nextElement();
				final Properties properties = new Properties();
				properties.load(url.openStream());

				final int size = properties.size();

				LOG.info("/resources/META-INF/下文件size={}", size);
				final String start = properties.getProperty("start");

				initializeStarter(start);
			}

//			LOG.info("初始化[{}]个starter结束", c);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startHttpServer(final int httpPort, final ZApplicationStartupInfo startupInfo) {
		if (!startupInfo.isHttpEnable()) {
			return;
		}

		final HttpRequestProcessor httpReader = ZContext.getBean(HttpRequestProcessor.class);
		final Map<String, Object> map = ZContext.all();
		final Set<Entry<String, Object>> es = map.entrySet();
		int childClassSize = 0;
		for (final Entry<String, Object> e : es) {
			final boolean equals = e.getValue().getClass().getSuperclass().equals(httpReader.getClass());
			if (equals) {
				childClassSize++;
				if (childClassSize > 1) {
					throw new StartupException(HttpRequestProcessor.class.getSimpleName() + "只允许有一个子类");
				}

				ZContext.remove(HttpRequestProcessor.class);
				ZContext.addBean(HttpRequestProcessor.class, e.getValue());
			}
		}

		final ZServer server = new ZServer();
		server.startServer(httpPort);
	}

	private static void initializeStarter(final String className) {

		try {
			final Class<?> cls = Class.forName(className);

			final ZStarter starter = (ZStarter) cls.getDeclaredConstructor().newInstance();
			starter.start();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void scanZSynchronously(final ZApplicationStartupInfo startupInfo) {
		final String[] pn = startupInfo.getPackageNameList().toArray(new String[0]);
		ZSynchronouslyScanner.scan(ZComponent.class, pn);
		ZSynchronouslyScanner.scan(ZService.class, pn);
	}

	@Override
	public void scanZAsync(final ZApplicationStartupInfo startupInfo) {
		final String[] pn = startupInfo.getPackageNameList().toArray(new String[0]);
		ZAsyncScanner.scan(ZComponent.class, pn);
		ZAsyncScanner.scan(ZService.class, pn);
	}

}