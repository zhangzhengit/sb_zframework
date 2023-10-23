# 使用说明
	# 配置文件: zframework.properties 支持零配置启动，配置类 @see ServerConfiguration 其中默认值。
	 如需自定义配置项，在zframework.properties中覆盖即可，如：server.port=8888 即可覆盖掉默认值80
	 查找顺序从先到后如下：
	 	1 jar文件同目录下
	 	2 jar文件下config下
	 	3 IDE中 src/main/resources目录下
	 	4 IDE中 src/main/resources/config目录下
	 按1 2 3 4查找，最后没找到则提示 [找不到配置文件]
	 
	# 新建工程A
		1、A引入 依赖
			<dependency>
				<groupId>com.vo</groupId>
				<artifactId>sb_zframework</artifactId>
				<version>1.0-SNAPSHOT</version>
			</dependency>
		2、新建A工程的启动类AMain：
			
			public class AMain {
			
				public static void main(final String[] args) {
					final String scanPackageName = "com.vo";
					ZApplication.run(scanPackageName, true, args);
				}
		
			}
			
			即可。com.vo为AMain所在包名。
			注意：AMain必须在顶级包下，其他包名必须以AMain所在包名为前缀，如：
				com.vo.api、com.vo.repository等
	 
	# 启动方式：
		java -jar -Dserver.port=7777 app.jar 
		可使用 server.port 参数执行启动的http端口，此参数优先于配置文件的server.port
		
	# 注解式声明Component、Controller
	
	@ZConfigurationProperties
		用于声明一个配置类，类中字段自动匹配 zframework.properties中的字段
		优先按java驼峰式命名匹配，找不到则按[orderCount]转为[order.count]来匹配
			
			prefix 属性：表示匹配的zframework.properties 中的前缀
			
		支持的字段类型：@see ZConfigurationProperties 
			List 类型：支持配置为空，如:
				private List<String> list;
				配置：
				xx.list[0]=A
				xx.list[2]=C
				等同于
				private List<String> list = {A,null,C};
				配置中不存在1，代码中list 1的位置为null
			
			Set 类型：使用LinkedHashSet支持按 0 1 2 3 顺序来配置值
			
		支持在字段上使用校验注解：@see # 校验注解
		
	@ZValue
		用于组件的字段上，表示此字段取值自配置文件,如：
		@ZValue(name = "name", listenForChanges = true)
		String name;
		表示 String类型的name字段，取值自配置文件中的name。listenForChanges = true 表示name字段实时读取配置文件
		变动并且更新字段值.
		配置文件的K优先于代码中的初始值。
		
	@ZComponent 
		用于声明一个通用组件
		
	@ZController 
		用于声明一个http接口类
	
		@ZRequestMapping 
			用于@ZController类里的方法上，声明一个http接口
			支持内容：
				1 	mapping [必填项] 数组，表示此方法匹配的接口路径
				2	isRegex 数组，与mapping数字按位置顺序对应，表示此mapping值是否正则表达式
				3	method 	表示 http method，取值见 MethodEnum
				4 	qps		表示此接口qps限制，默认值 见 ZRequestMapping
	
		@ZRequestMapping 方法支持的参数，按需使用
			@ZRequestParam
				表示此参数作为一个http参数,如 @ZRequestParam String name		
			@ZRequestBody 
				表示此参数是一个请求体
				支持在此注解标记的参数对象上使用 @ZValidated 来启用校验注解：@see 校验注解
				如：
				public CR buildstart(@ZRequestBody @ZValidated final BuildDTO buildDTO) {
					// ............
					// ............
					return CR.ok();
				}
				BuildDTO 类有字段：
				
				@ZNotNull
				private String name;
				
				加入@ZValidated注解，则会校验buildDTO对象的name字段值不能为null，不加则不校验
				
			@ZRequestHeader 
				表示此参数是一个request.header
			ZRequest
				表示本次请求
			ZResponse
				用于返回本次请求的响应结果
				
				
	@ZAutowired
		用于自动注入一个由容器管理的对象
		
	AOP 使用步骤：
	
		@ZAOP(interceptType = ZNow.class)
		public class ZNowAOP implements ZIAOP {
			// ....		
		}
		
		@ZComponent
		class ZService {
			@ZNow
			public Date now() {
				return new Date();
			}
		}
		
			1 组件内方法加入自定义注解 @ZNow
			2 声明AOP类ZNowAOP，指定代理注解 ZNow.class，同时实现 ZIAOP覆盖期方法，编码代码
			3 支持使用ZService.now
		
   		
   	返回html：
   		定义接口如下，即可返回 index.html静态页面.html存放在 ServerConfiguration.htmlPrefix 目录中.
   		@ZHtml
		@ZRequestMapping(mapping = { "/html" })
		public String html() {
			return "index.html";
		}

		@ZHtml 
			表示此接口方法content-type为text/html，无此注解则默认application/json
   		
	使用html模板：
		定义接口如下，接口方法加入ZModel参数，用于设值.
			
   		@ZHtml
		@ZRequestMapping(mapping = { "/html" })
		public String html(final ZModel model) {
			model.set("name", "zhangsan");
	
			return "index.html"; 
		}
		
		模板：freemarker。支持在html中正常写freemarker标签。
				
	内置静态资源接口：StaticController，内置三个正则表达式接口，价值的静态资源放在 ServerConfiguration.staticPrefix 目录中 
		
		@ZRequestMapping(mapping = { "/favicon\\.ico", "/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
		"/.+\\.gif$", "/.+\\.doc$" }, isRegex = { true, true, true, true, true, true, true, true })
		
		@ZRequestMapping(mapping = { "/.+\\.html$" }, isRegex = { true })
		
		@ZRequestMapping(mapping = { "/.+\\.css$" }, isRegex = { true })
		
		
	ZContext :
		用于手动注入bean到容器、获取由容器管理的bean
		
		
	静态文件服务器使用如下：
		1 	配置	server.static.path=E:\\x
		2	E:\\x 目录下 新建ok.html,
		3   输入 http://localhost/html/ok.html 即可展示ok.html
		
		server.static.path 优先于	server.static.prefix
		
	手动注入对象：
		1 注入ZComponent 对象:
			如：新建类A,
			A a = new A();		
			在 工程main方法调用 ZApplication.run 之前执行如下代码：
			
			ZContext.addBean(a.getClass().getCanonicalName(), a);
			
			即可注入a对象.
			
			需要使用A对象的地方正常使用即可
			 
			@ZAutowired
			A a;
			
	@ZAsync 
		用在方法上，表示此方法异步执行，使用说明：
		1、配置文件添加如下内容：
			async.thread.count=12
			async.thread.name.prefix=zasync-Thread-
		2、在目标方法A上加入 @ZAsync，直接调用A即可
		
	
	ZControllerInterceptor 接口，实现此接口拦截 @ZController 里面的接口方法.
								
		
	手动注册接口，ZControllerMap.put
		@see 接口注释
		
	@ZValidated :
		用法如下：
			1、用在接口方法的参数上，表示校验参数对象里的 @see # 校验注解：
				public CR buildstart(@ZRequestBody @ZValidated final BuildDTO buildDTO) {
					// ............
					// ............
					return CR.ok();
				}
			2、用在 @ZComponent 类的方法的参数对象上，如：
				public void zv(final ZVDTO zvdto) {
					// ............
				}	
				ZVDTO 类上加入@ZValidated注解，在接口方法中调用zv方法时则会
				自动校验ZVDTO里带有 @see # 校验注解 的字段
			
		
	# 自定义响应头,添加如下配置即可,
		server.responseHeaders.key1=value1
		server.responseHeaders.key2=value2
		server.responseHeaders.key3=value3
		如：解决CORS问题配置如下
		server.responseHeaders.Access-Control-Allow-Origin=*
	
	# 校验注解：
		@ZMax 表示此配置字段的最大值不能大于多少,支持 extends Number 的类型
		@ZMin 表示此配置字段的最小值不能小于多少,支持 extends Number 的类型
		@ZNotEmtpy 表示此配置字段不能是empty,支持类型：String、List、Set、Map，都是使用isEmpty来判断
		@ZNotNull 表示此配置字段不能是null,支持所有类型
		@ZStartWith 表示此配置字段必须以特定值开头,支持String类型
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
