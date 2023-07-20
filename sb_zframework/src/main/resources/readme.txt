# 使用说明
	# 配置文件: zframework.properties
	 查找顺序从先到后如下：
	 	1 jar文件同目录下
	 	2 jar文件下config下
	 	3 IDE中 src/main/resources目录下
	 	4 IDE中 src/main/resources/config目录下
	 按1 2 3 4查找，最后没找到则提示 [找不到配置文件]
	 
	 配置内容见：ServerConfiguration
		
	# 注解式声明Component、Controller
	
	@ZConfigurationProperties
		用于声明一个配置类，类中字段自动匹配 zframework.properties中的字段
		优先按java驼峰式命名匹配，找不到则按[orderCount]转为[order.count]来匹配
			
			prefix 属性：表示匹配的zframework.properties 中的前缀
		
			@ZMax 表示此配置字段的最大值不能大于多少
			@ZMin 表示此配置字段的最小值不能小于多少
			@ZNotEmtpy 表示此配置字段不能是empty
			@ZNotNull 表示此配置字段不能是null
			@ZStartWith 表示此配置字段必须以特定值开头
		
	@ZValue
		用于组件的字段上，表示此字段取值自配置文件,如：
		@ZValue(name = "name", listenForChanges = true)
		String name;
		表示 String类型的name字段，取值自配置文件中的name。listenForChanges = true 表示name字段实时读取配置文件
		变动并且更新字段值.
		
		
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
		
		支持的模板标签：
			1 @value[key] 
				用于取值，如上接口取name，则声明为 @value[name]，即可在html显示 zhangsan
			  	或 @value[user.name] 形式，从user对象中取name字段值
			2 	<@list[userList] as u>
					<h3>姓名：@value[u.name] | 年龄 @value[u.age]</h3>
				</endlist[userList]>
				用于遍历一个Iterable对象或数组，如上代码表示：
					遍历一个userList，别名为u，循环生成h3标签，里面内容为 [姓名:zhangsan | 年龄 : 200] 的形式
			3 	<@switch[i1]>
					<case 1> 	一
					<case 2> 	二
					<case 3> 	三
				</endswitch[i1]>
				
				用于判断一个值，如上代码表示：
					if == 1则展示[一],if == 2则展示[二],if == 3则展示[三].
				
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
		2、在模板方法上加入 @ZAsync，支持调用即可
		
	
	ZControllerInterceptor 接口，实现此接口拦截 @ZController 里面的接口方法.
								
		
	手动注册接口，ZControllerMap.put
		@see 接口注释
