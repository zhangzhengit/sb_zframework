package vo.zframework;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import vo.log.core.ZLog2;
import vo.zframework.anno.ZAOP;
import vo.zframework.anno.ZCommandLineRunner;
import vo.zframework.anno.ZComponent;
import vo.zframework.anno.ZConfiguration;
import vo.zframework.anno.ZController;
import vo.zframework.anno.ZPathVariable;
import vo.zframework.anno.ZRequestMapping;
import vo.zframework.anno.ZRestController;
import vo.zframework.anno.ZService;
import vo.zframework.api.StaticController;
import vo.zframework.api.StaticResourcesPreCompressionService;
import vo.zframework.bean.ZObjectGeneratorStarter;
import vo.zframework.bean.ZSingleton;
import vo.zframework.common.STU;
import vo.zframework.configuration.properties.CommonConfigurationProperties;
import vo.zframework.configuration.properties.ServerConfigurationProperties;
import vo.zframework.configuration.properties.ZConfigurationProperties;
import vo.zframework.core.ZContext;
import vo.zframework.event.APIRouteR;
import vo.zframework.event.IAPIRoute;
import vo.zframework.event.ZApplicationEventPublisher;
import vo.zframework.exception.StartupException;
import vo.zframework.exception.ZControllerAdviceScanner;
import vo.zframework.html.ResourcesLoader;
import vo.zframework.http.ZControllerMap;
import vo.zframework.http.ZRMethod;
import vo.zframework.http.ZServer;
import vo.zframework.http.request.HttpRequestProcessor;
import vo.zframework.scanner.ZAsyncScanner;
import vo.zframework.scanner.ZAutowiredScanner;
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
import vo.zframework.zclass.ZClass;
import vo.zframework.zclass.ZField;
import vo.zframework.zclass.ZMethod;
import vo.zframework.zclass.ZMethodArg;
import vo.zframework.zclass.ZPackage;

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

			final ZClass proxyZClass = this.gControllerProxyZClass();

			final Object newInstance = proxyZClass.newInstance();

			ZContext.addBean(IAPIRoute.class, newInstance);

		}
	}

	private ZClass gControllerProxyZClass() {
		// FIXME 2026年7月16日 10:02:17 zhangzhen : 动态生成接口方法路由代理类
		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage("vo.zframework.generated"));
		proxyZClass.setName("ZAPIRoute");
		proxyZClass.setImplementsSet(Set.of(IAPIRoute.class.getCanonicalName()));

		proxyZClass.addField(new ZField(APIRouteR.class.getName(), "MATCHED",
				"new " + APIRouteR.class.getCanonicalName() + "(true);"));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setThrowsE(List.of(Exception.class.getCanonicalName()));
		routeMethod.setReturnType(APIRouteR.class.getCanonicalName());

		routeMethod.setBodyReturn("return new " + APIRouteR.class.getCanonicalName()
				+ "(false);");

		final Object[] a = {};
		routeMethod.setMethodArgList(List.of(
				new ZMethodArg(String.class.getCanonicalName(), "path"),
				new ZMethodArg(Object.class.getCanonicalName(), "controller"),
				new ZMethodArg(ZRMethod.class.getCanonicalName(), "zrMethod"),
				new ZMethodArg(a.getClass(), "parameters")));

		proxyZClass.setMethodSet(Set.of(routeMethod));

		final StringBuilder routeBody = new StringBuilder("switch (path) {");

		final Map<Method, Object> mcmap = ZControllerMap.getMCMap();
		final Set<Entry<Method, Object>> es = mcmap.entrySet();

		int pI = 0;
		for (final Entry<Method, Object> e : es) {
			final Method method = e.getKey();

			final Parameter[] mp = method.getParameters();
			final Optional<Parameter> isZPVO = Arrays.stream(mp)
			.filter(p -> p.isAnnotationPresent(ZPathVariable.class)).findAny();
			if (isZPVO.isPresent()) {
				// FIXME 2026年7月16日 16:57:08 zhangzhen : @ZPathVariable的有点不好匹配，先不支持
				continue;
			}

			final Object controller = e.getValue();
			if (controller.getClass().getCanonicalName().equals(StaticController.class.getCanonicalName())) {
				// FIXME 2026年7月16日 16:57:47 zhangzhen : StaticController 里面都是正则的，也不好匹配，也暂时不支持
				continue;
			}

			pI++;

			final ZController zc = controller.getClass().getAnnotation(ZController.class);
			final String prefix = zc != null ? zc.prefix() :  controller.getClass().getAnnotation(ZRestController.class).prefix();

			final ZRequestMapping rm = method.getAnnotation(ZRequestMapping.class);

			final boolean[] regex = rm.isRegex();

			final String[] ma = rm.mapping();

			ZApplicationStartupAdapter.newLine(routeBody);

			routeBody.append("case \"")
			.append(prefix)
			// FIXME 2026年7月16日 17:46:01 zhangzhen : 不该写死ma[0]。而是foreach
			// 并且isRegex为true的也跳过
			.append(ma[0]).append("\"").append(':');

			ZApplicationStartupAdapter.newLine(routeBody);

			// 方法调用

			routeBody.append(controller.getClass().getCanonicalName()).append(" ")
			.append("controller").append(pI).append(" = ")
			.append("(").append(controller.getClass().getCanonicalName()).append(")controller;");

			ZApplicationStartupAdapter.newLine(routeBody);

			final Class<?> returnType = method.getReturnType();
			final int parameterCount = method.getParameterCount();

			// void 方法
			final boolean returnVOID = returnType.getCanonicalName().equals(void.class.getCanonicalName());
			// 非void方法，需要return
			if (parameterCount <= 0) {

				if (!returnVOID) {
					routeBody.append("return ");
					routeBody.append("new ")
					.append(APIRouteR.class.getCanonicalName())
					.append("(")
					;
				}

				routeBody
				.append("controller").append(pI).append(".").append(method.getName()).append("()");

			} else {

				final Parameter[] parameters = method.getParameters();
				final StringBuilder pb = new StringBuilder();
				for (int i = 0; i < parameters.length; i++) {
					final Parameter p = parameters[i];

					final Class<?> type = p.getType();
					pb.append("(")
					.append(type.getCanonicalName())
					.append(")")
					.append("parameters[").append(i).append("]");
					if (i < (parameters.length - 1)) {
						pb.append(',');
					}
				}

				if (!returnVOID) {
					routeBody.append("return ");
					routeBody.append("new ")
					.append(APIRouteR.class.getCanonicalName())
					.append("(")
					;
				}

				routeBody
				.append("controller").append(pI).append(".").append(method.getName()).append("(")
				.append(pb)
				.append(")")
				;
			}
			if (!returnVOID) {
				routeBody.append(");");
			} else {
				routeBody.append(";");
			}

			ZApplicationStartupAdapter.newLine(routeBody);

			if (returnVOID) {
				routeBody.append("return MATCHED;");
			}
		}

		routeBody.append("default:\r\n"
							+ "	break;\r\n"
							+ "}	");

		routeMethod.setBody(routeBody.toString());

//		System.out.println("proxyZClass = ");
//		System.out.println(proxyZClass.toString());
		return proxyZClass;
	}

	private static void newLine(final StringBuilder routeBody) {
		routeBody.append(ZMethod.NEW_LINE);
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
//		ZCacheScanner.scanAndValidate();
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
			// FIXME 2026年6月24日 18:01:40 zhangzhen : 发现bug:这么指定name了，有多个的话只会找到一个,改为支持*.后缀的方式
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
		final Class[] ca = { ZComponent.class, ZService.class };
		ZSynchronouslyScanner.scan(ca, pn);
	}

	@Override
	public void scanZAsync(final ZApplicationStartupInfo startupInfo) {
		final String[] pn = startupInfo.getPackageNameList().toArray(new String[0]);
		ZAsyncScanner.scan(ZComponent.class, pn);
		ZAsyncScanner.scan(ZService.class, pn);
	}

	@Override
	public void preCompression(final ZApplicationStartupInfo startupInfo) {
		StaticResourcesPreCompressionService.preCompression();
	}

}