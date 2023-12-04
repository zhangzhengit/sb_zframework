package com.vo;

/**
 *
 * 程序启动流程，按方法定义顺序从上到下执行
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
// FIXME 2023年12月4日 下午10:10:30 zhanghen: TODO : 每个步骤 再定义一个接口分出【前、执行、后】三个步骤，用户可以覆盖任何一步来自定义流程
public interface ZApplicationStartupProcessor {

	/**
	 * 校验注解：看用的字段是否支持
	 *
	 * @param startupInfo
	 *
	 */
	void startValidator(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 @ZEventListener 方法
	 *
	 * @param startupInfo
	 *
	 */
	void startEventPublisher(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 @ZConfigurationProperties 的类，生成配置信息
	 *
	 * @param startupInfo
	 * @throws Exception
	 *
	 */
	void scanConfigurationProperties(ZApplicationStartupInfo startupInfo) throws Exception;

	/**
	 * 扫描 @ZConfiguration 的类，读取其中的 @ZBean 方法，来创建一个bean
	 *
	 * @param startupInfo
	 * @throws Exception
	 *
	 */
	void scanConfiguration(ZApplicationStartupInfo startupInfo) throws Exception;

	/**
	 * 扫描对象生成器
	 *
	 * @param startupInfo
	 *
	 */
	void startObjectGenerator(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 @ZComponent 和 @ZService 组件，生成bean
	 *
	 * @param startupInfo
	 *
	 * @author zhangzhen
	 * @date 2023年12月4日
	 */
	void scanComponent(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 @ZControllerAdvice 的类，生成bean
	 *
	 * @param startupInfo
	 *
	 */
	void scanControllerAdvice(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 @ZController 的类生成bean，然后读取其中的 @ZRequestMapping 方法，生成一个接口
	 *
	 * @param startupInfo
	 *
	 */
	void scanController(ZApplicationStartupInfo startupInfo);

	/**
	 * 给组件[ZService.class, ZComponent.class, ZController.class, ZConfiguration.class, ZAOP.class]等等的 @ZAutowired 字段注入值
	 *
	 * @param startupInfo
	 *
	 */
	void injectAutowired(ZApplicationStartupInfo startupInfo);

	/**
	 * 给组件[ZService.class, ZComponent.class, ZController.class]等等的 @ZValue 字段注入值
	 *
	 * @param startupInfo
	 *
	 */
	void injectValue(ZApplicationStartupInfo startupInfo);

	/**
	 * 校验缓存包下的注解使用方式是否正确
	 *
	 * @param startupInfo
	 *
	 */
	void validatedCache(ZApplicationStartupInfo startupInfo);

	/**
	 * set StaticPath
	 *
	 * @param startupInfo
	 *
	 */
	void setStaticPath(ZApplicationStartupInfo startupInfo);

	/**
	 * 在启用打印 @ZConfigurationProperties 的情况下，在
	 * 启动时打印一下 @ZConfigurationProperties 的配置类的值
	 *
	 * @param startupInfo
	 *
	 */
	void printZConfigurationProperties(ZApplicationStartupInfo startupInfo);

	/**
	 * 	@ZAutowired 字段值都注入完成以后，验证一下 required=true的字段是否null，如果null则抛出异常
	 *
	 * @param startupInfo
	 *
	 */
	void aftertAutowiredInject(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描自定义拦截器（ZHandlerInterceptor 的子类）
	 *
	 * @param startupInfo
	 *
	 */
	void scanHandlerInterceptor(ZApplicationStartupInfo startupInfo);

	/**
	 * 扫描 ZCommandLineRunner 的子类，在程序所有动作都执行之后、http服务器启动之前执行一下
	 *
	 * @param startupInfo
	 * @throws Exception
	 *
	 */
	void runCommandLineRunner(ZApplicationStartupInfo startupInfo) throws Exception;

	/**
	 * 启动http服务器
	 *
	 * @param startupInfo
	 *
	 */
	void startHttpServer(ZApplicationStartupInfo startupInfo);

}