package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是一个通用的组件，由容器自动管理
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZComponent {

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

	// FIXME 2023年11月6日 上午1:30:16 zhanghen: TODO 紧急修复：
	/*
	 * 大问题：
	 * 本注解声明的方法上 有字段 如
	@ZAutowired
	private UserRepositoryZ userRepositoryZ;

	并且有方法如下，注解此方法带有自定义注解，并且使用了@ZAutowired 的字段。
	@ZCacheable(group = "fff", key = "id",expire = 10)
	public UserEntityZ findByIdA(final Long id) {


		return this.userRepositoryZ.findById(id);
	}


	由于存在自定义注解，会被AOP生成代理类，代理中super.findByIdA 会用到本类中的userRepositoryZ。
	试了给代理类和父类（本类）都给此字段赋值依然不行(字段仍是null)。极有可能原因如下：


		package com.vo.api;

		@com.vo.aop.ZAOPProxyClass

		public class UserService_ProxyZclass extends com.vo.api.UserService{

		com.vo.aop.ZIAOP ziaop_findByIdA = (com.vo.aop.ZIAOP)com.vo.core.ZSingleton.getSingletonByClassName("com.vo.cache.ZCacheableAOP");

		com.vo.testZRepository.UserRepositoryZ userRepositoryZ = null;


		 public  com.vo.testZRepository.UserEntityZ findByIdA ( java.lang.Long id ) {

			 	final com.vo.aop.AOPParameter parameter = new com.vo.aop.AOPParameter();
				parameter.setIsVOID(false);
				parameter.setTarget(com.vo.core.ZSingleton.getSingletonByClass(this.getClass().getSuperclass()));
				parameter.setMethodName("findByIdA");
				java.lang.reflect.Method m = com.vo.aop.ZAOPScaner.cmap.get("com.vo.api.UserService@findByIdA");
				parameter.setMethod(m);
				parameter.setParameterList(com.google.common.collect.Lists.newArrayList(id));
				ziaop_findByIdA.before(parameter);
				final Object v = this.ziaop_findByIdA.around(parameter);
				ziaop_findByIdA.after(parameter);
				return ( com.vo.testZRepository.UserEntityZ)v;



		 }

 		}

 		在此：
 			parameter.setTarget(com.vo.core.ZSingleton.getSingletonByClass(this.getClass().getSuperclass()));

		ZSingleton.getSingletonByClass	如果没有单例对象则会生成一个，如果代理方法中是第一次执行此方法，则会市场一个全新的父类对象，
		对于执行的给代理类和父类字段赋值是肯定不起作用的。

		修复思路：
		 1、依然用ZSingleton.getSingletonByClass，只是在之前先调用一下，把结果（父类）的字段赋值。后面在代理方法中再次调用
		  得到的是已经赋值了的父类对象。就没问题了。
		 2、不用ZSingleton.getSingletonByClass了，直接用ZContext.getBean 思路似乎和什么差不多
























	 *
	 *
	 */

}
