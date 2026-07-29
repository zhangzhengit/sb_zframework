package vo.zframework;

import java.io.IOException;
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
import vo.zframework.core.ZApplicationStartupInfo;
import vo.zframework.core.ZContext;
import vo.zframework.enums.MethodEnum;
import vo.zframework.event.ZApplicationEventPublisher;
import vo.zframework.exception.StartupException;
import vo.zframework.exception.ZControllerAdviceScanner;
import vo.zframework.html.ResourcesLoader;
import vo.zframework.http.ZControllerMap;
import vo.zframework.http.ZRMethod;
import vo.zframework.http.ZServer;
import vo.zframework.http.request.HttpRequestProcessor;
import vo.zframework.route.APIRouteR;
import vo.zframework.route.IAPIRoute;
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
		ZValidator.start(startupInfo);
	}

	@Override
	public void startEventPublisher(final ZApplicationStartupInfo startupInfo) {
		ZApplicationEventPublisher.start(startupInfo.getPackageNameArray());
	}

	@Override
	public void scanConfigurationProperties(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationPropertiesScanner.scanAndCreate(startupInfo);
	}

	@Override
	public void scanConfiguration(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationScanner.scanAndCreate(startupInfo.getPackageNameArray());
	}

	@Override
	public void startObjectGenerator(final ZApplicationStartupInfo startupInfo) {
		ZObjectGeneratorStarter.start(startupInfo);
	}

	@Override
	public void scanComponent(final ZApplicationStartupInfo startupInfo) {
		ZComponentScanner.scanAndCreate(startupInfo);
	}

	@Override
	public void scanControllerAdvice(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			ZControllerAdviceScanner.scan(startupInfo);
		}
	}

	@Override
	public void scanController(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {

//			Thread.ofVirtual().start(() -> {

				ZControllerScanner.scanAndCreateObject(startupInfo);

				// 2
				ZContext.addBeanAsync(IAPIRoute.class, () -> {
					final ZClass proxyZClass = this.gControllerProxyZClass();
					final Object newInstance = proxyZClass.newInstance();
					return newInstance;
				});
//			});


			// 1
//			final ZClass proxyZClass = this.gControllerProxyZClass();
//			final Object newInstance = proxyZClass.newInstance();
//			ZContext.addBean(IAPIRoute.class, newInstance);

		}
	}

	private ZClass gControllerProxyZClass() {
		// FIXME 2026年7月16日 10:02:17 zhangzhen : 动态生成接口方法路由代理类
		final ZClass proxyZClass = new ZClass();
		proxyZClass.setPackage1(new ZPackage("vo.zframework.generated"));
		proxyZClass.setName("ZAPIRoute");
		proxyZClass.setImplementsSet(Set.of(IAPIRoute.class.getCanonicalName()));

		proxyZClass.addField(new ZField(String.class.getName(), "GET",
				MethodEnum.class.getCanonicalName() + ".GET.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "POST",
				MethodEnum.class.getCanonicalName() + ".POST.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "PUT",
				MethodEnum.class.getCanonicalName() + ".PUT.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "DELETE",
				MethodEnum.class.getCanonicalName() + ".DELETE.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "HEAD",
				MethodEnum.class.getCanonicalName() + ".HEAD.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "CONNECT",
				MethodEnum.class.getCanonicalName() + ".CONNECT.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "TRACE",
				MethodEnum.class.getCanonicalName() + ".TRACE.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "OPTIONS",
				MethodEnum.class.getCanonicalName() + ".OPTIONS.getMethod()"));
		proxyZClass.addField(new ZField(String.class.getName(), "PATCH",
				MethodEnum.class.getCanonicalName() + ".PATCH.getMethod()"));
		proxyZClass.addField(new ZField(APIRouteR.class.getName(), "MATCHED",
				"new " + APIRouteR.class.getCanonicalName() + "(true);"));
		proxyZClass.addField(new ZField(APIRouteR.class.getName(), "NOTMATCHED",
				"new " + APIRouteR.class.getCanonicalName() + "(false);"));

		final ZMethod routeMethod = new ZMethod();
		routeMethod.setName("route");
		routeMethod.setThrowsE(List.of(Exception.class.getCanonicalName()));
		routeMethod.setReturnType(APIRouteR.class.getCanonicalName());

		routeMethod.setBodyReturn("return NOTMATCHED;");

		final Object[] a = {};
		routeMethod.setMethodArgList(List.of(
				new ZMethodArg(String.class.getCanonicalName(), "path"),
				new ZMethodArg(Object.class.getCanonicalName(), "controller"),
				new ZMethodArg(ZRMethod.class.getCanonicalName(), "zrMethod"),
				new ZMethodArg(a.getClass(), "parameters")));

		proxyZClass.setMethodSet(Set.of(routeMethod));

		final StringBuilder routeBody =
				new StringBuilder("final String httpMethod = zrMethod.getHttpMethod();");

		routeBody.append
			("if (GET.equals(httpMethod)) {"
				+ "GETAPI"
				+ "} else if (POST.equals(httpMethod)) {"
				+ "POSTAPI"
				+ "} else if (PUT.equals(httpMethod)) {"
				+ "PUTAPI"
				+ "} else if (DELETE.equals(httpMethod)) {"
				+ "DELETEAPI"
				+ "} else if (HEAD.equals(httpMethod)) {"
				+ "HEADAPI"
				+ "} else if (CONNECT.equals(httpMethod)) {"
				+ "CONNECTAPI"
				+ "} else if (TRACE.equals(httpMethod)) {"
				+ "TRACEAPI"
				+ "} else if (OPTIONS.equals(httpMethod)) {"
				+ "OPTIONSAPI"
				+ "} else if (PATCH.equals(httpMethod)) {"
				+ "PATCHAPI"
				+ "}");

		final StringBuilder get = ZApplicationStartupAdapter.gMethod(MethodEnum.GET);
		final StringBuilder post = ZApplicationStartupAdapter.gMethod(MethodEnum.POST);
		final StringBuilder put = ZApplicationStartupAdapter.gMethod(MethodEnum.PUT);
		final StringBuilder delete = ZApplicationStartupAdapter.gMethod(MethodEnum.DELETE);
		final StringBuilder head = ZApplicationStartupAdapter.gMethod(MethodEnum.HEAD);
		final StringBuilder CONNECT = ZApplicationStartupAdapter.gMethod(MethodEnum.CONNECT);
		final StringBuilder TRACE = ZApplicationStartupAdapter.gMethod(MethodEnum.TRACE);
		final StringBuilder OPTIONS = ZApplicationStartupAdapter.gMethod(MethodEnum.OPTIONS);
		final StringBuilder PATCH = ZApplicationStartupAdapter.gMethod(MethodEnum.PATCH);

		final String body = routeBody.toString()
				.replace("GETAPI", get)
				.replace("POSTAPI", post)
				.replace("PUTAPI", put)
				.replace("DELETEAPI", delete)
				.replace("HEADAPI", head)
				.replace("CONNECTAPI", CONNECT)
				.replace("TRACEAPI", TRACE)
				.replace("OPTIONSAPI", OPTIONS)
				.replace("PATCHAPI", PATCH)
				;

		routeMethod.setBody(body);

//		System.out.println("apiproxyZClass = ");
//		System.out.println(proxyZClass.toString());
		return proxyZClass;
	}

	private static StringBuilder gMethod(final MethodEnum methodEnum) {

		final StringBuilder switchcase = new StringBuilder("switch (path) {");

		int pI = 0;

		for (final Entry<Method, Object> e : ZControllerMap.getMCMap().entrySet()) {
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


			final String prefix = ZApplicationStartupAdapter.gCPrefix(controller);

			final ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);
			final boolean[] isRegex = requestMapping.isRegex();

			final String[] mappings = requestMapping.mapping();
			// 对于同一个api的method，会生成多个case，也没问题，该不该为case A:case B:的形式问题都不大
			for (int mi = 0; mi < mappings.length; mi++) {
				if (isRegex[mi]) {
					continue;
				}

				if (requestMapping.method() != methodEnum) {
					continue;
				}

				pI++;

				switchcase.append("case \"").append(prefix).append(mappings[mi]).append("\":");

				ZApplicationStartupAdapter.newLine(switchcase);

				// 方法调用
				switchcase.append(controller.getClass().getCanonicalName()).append(" ")
				.append("controller").append(pI).append(" = ")
				.append("(").append(controller.getClass().getCanonicalName()).append(")controller;");

				ZApplicationStartupAdapter.newLine(switchcase);

				final Class<?> returnType = method.getReturnType();
				final int parameterCount = method.getParameterCount();


				// void 方法
				final boolean returnVOID = returnType.getCanonicalName().equals(void.class.getCanonicalName());
				// 非void方法，需要return
				if (parameterCount <= 0) {

					if (!returnVOID) {
						switchcase.append("return ");
						switchcase.append("new ")
						.append(APIRouteR.class.getCanonicalName())
						.append("(")
						;
					}

					switchcase
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
						switchcase.append("return ");
						switchcase.append("new ")
						.append(APIRouteR.class.getCanonicalName())
						.append("(")
						;
					}

					switchcase
					.append("controller").append(pI).append(".").append(method.getName()).append("(")
					.append(pb)
					.append(")")
					;
				}
				if (!returnVOID) {
					switchcase.append(");");
				} else {
					switchcase.append(";");
				}

				ZApplicationStartupAdapter.newLine(switchcase);

				if (returnVOID) {
					switchcase.append("return MATCHED;");
				}
			}
		}

		switchcase.append("default:"
							+ "	break;"
							+ "}	");
		return switchcase;
	}

	private static String gCPrefix(final Object controller) {
		final ZController zc = controller.getClass().getAnnotation(ZController.class);
		final String prefix = zc != null ? zc.prefix() :  controller.getClass().getAnnotation(ZRestController.class).prefix();
		return prefix;
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
			ZAutowiredScanner.inject(cls, startupInfo);
		}
	}

	@Override
	public void injectValue(final ZApplicationStartupInfo startupInfo) {
		ZValueScanner.inject(startupInfo);
	}

	@Override
	public void validatedCache(final ZApplicationStartupInfo startupInfo) {
		ZCacheableValidator.validated(startupInfo.getPackageNameArray());
		ZCacheScanner.scan(startupInfo);
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
		ZAutowiredScanner.after(startupInfo);
	}

	@Override
	public void scanHandlerInterceptor(final ZApplicationStartupInfo startupInfo) {
		ZHandlerInterceptorScanner.scan(startupInfo);
	}

	@Override
	public void runCommandLineRunner(final ZApplicationStartupInfo startupInfo) throws Exception {
		for (final Object zclr : ZCommandLineRunnerScanner.scan(startupInfo.getPackageNameArray())) {
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
		ZSynchronouslyScanner.scan(startupInfo);
	}

	@Override
	public void scanZAsync(final ZApplicationStartupInfo startupInfo) {
		ZAsyncScanner.scan(startupInfo);
	}

	@Override
	public void preCompression(final ZApplicationStartupInfo startupInfo) {
		StaticResourcesPreCompressionService.preCompression();
	}

}