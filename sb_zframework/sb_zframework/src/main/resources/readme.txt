# 写一个类似springboot的框架

# 使用说明：
	1 新建java项目，引入此项目的依赖，配置 zframework.properties 
	2 在 main 方法中 加入 ZApplication.run(args);
	3 定义类，按作用加入不同的注解, ZComponent  ZController 等等 
		 如：
		 	@ZComponent
			class A {
				public Date time() {
					return new Date();
				}
			}
		 		
		 	@ZController
			class API {
			
				@ZAutowired
				A a;
			
				@ZGet(path = "/time")
				public Date time() {
					return this.a.time();
				}
			}	
		 	  
	4 启动main方法，访问http://localhost:定义的端口/time 即可访问第三步定义的接口

# 2023.6.20 已实现的功能
 	1 ioc 和 aop
 	2 ZComponent  ZController ZAutowired ZGet ZPost ZRequestParam
   
# 2023.6.26 已实现的功能
	1 ZRequestHeader 注解，用在接口的参数上，表示一个header
	2 默认返回 text，加入@ZHtml注解则返回html页面
	
# 2023.6.27 已实现的功能
	1 html支持模板，现支持的标签：
		1.1 @value[] ，如： @value[name],在 @ZController 的方法中，加入参数 ZModel,使用 model.set("name","zhang"),
			html 中加入 @value[name],访问接口，即可在html中展示 zhang
		1.2 @list[]	,如：
			<@list[list1] as a>
				<h3>姓名：@value[a.name] | 年龄 @value[a.age]</h3>
			</endlist[list1]>
		 
		    在接口中使用 model.set("list1",list)即可.
		    
		    注意：如果同一页面用到多个@list[] 标签，则as后面的名称不可以重复	
		    	如：<@list[list1] as a>
		    	   <@list[list2] as a>
		    	上面两个@list[] 标签中都以a为变量名是不可以的。
		    	
		    	TODO:同一list名称暂时不可以在同一html页面中使用多次,
		    	如：<@list[list1] as a1>
		    	   <@list[list1] as a2>
		    	上面 list1 变量在一个html中用了两次，暂时不支持这么用
# 2023.6.28 已实现的功能		 
	1. ZRequestMapping 注解，代替 ZGet、ZPost 等等 httpmethod的注解
	2. ZRequestMapping 的mapping 值 支持配置是否正则表达式
	3. StaticController 新增此接口类，内置了处理静态资源的接口 StaticController.staticResources

# 2023.6.29 已实现的功能		 
	1. html模板的 switch标签,用法如下：
		接口中设置
			 model.set("statusAAA", 1);
			 
		html中 加入
			<@switch[statusAAA]>
				<case 1> 	状态111
				<case 2> 	状态222
				<case 3> 	状态333
			</endswitch[statusAAA]>
			    	