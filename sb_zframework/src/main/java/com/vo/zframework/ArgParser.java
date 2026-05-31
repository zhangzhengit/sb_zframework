package com.vo.zframework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vo.zframework.aop.ArgR;
import vo.zframework.cache.AU;

/**
 * 解析 java 命令行中传来的参数
 * 自定义格式为 --myKey=myValue 的形式.
 * 如：--server.port=88 解析出结果为key=server.port value=80
 * 此配置优先级高于 application.properties配置文件，即：
 * 	.p文件中有比如：server.port=80 同时指定了 java --server.port=88 -jar app.jar
 *  则程序会以88的端口来启动，其他配置项也是如此。
 *
 * 本功能不会改变其他如@ZValue和@ZConfigurationProperties的功能，只是相比于写在.p文件中多了一种传参方式。
 * 也不改变 @ZValue.listenForChanges的功能，即：即使--指定了参数，它会优先于.p文件指定的，但是程序运行后
 * 在.p文件中修改了这个参数，此时仍会使用更新的.p中的参数值。
 *
 * 严格定义为必须--开头，后面紧跟着k，后面是=，最后面是v，
 * key不允许有空格，如：-- k=v --k =v都被直接忽略掉不被认为是在用本类方法传参。
 * value如果包含空格，需要用""括起来,如：--key=" value" --key="value " --key=" va lue "
 * 	上面三种情况被程序解析出的结果分别是前面一个空格、后面一个、前中后各一个。
 *
 * @author zhangzhen
 * @date 2025年8月25日
 *
 */
public class ArgParser {

	private static final char SPACE = ' ';
	public static String PREFIX = "--";

//	public static void main(String[] args) {
//		String a = " --server.port=88";
//		String b = "--life.ip=localhost";
//		String c = "--";
//		String d = "-- acutator.enable=false";
//		String e = "--acutator.enable =false";
//		String f = "--acut ator.enable =false";
//		String g = "--key= value";
//		p(new String[] { a, b, c, d, e, f ,g});
//	}

	/**
	 * 解析--开头的参数，注意：非--开头的参数本方法不会解析，而是直接忽略
	 *
	 * @param args
	 */

	// FIXME 2025年8月25日 下午8:24:11 zhangzhen: 考虑：如果key重复了怎么办？
	// 可能是用户不小心输重复了，要提示吗？还是直接不允许启动？
	// 考虑：list  set 类型要不要支持此方式传值？

	public static List<ArgR> p(final String[] args) {
		if (AU.isEmpty(args)) {
			return Collections.EMPTY_LIST;
		}

		final List<ArgR> r = new ArrayList<>();


		for (final String a : args) {
			if ((a == null) || (a.length() <= PREFIX.length())) {
				continue;
			}

			final int pI = a.indexOf(PREFIX);
			if(pI < 0) {
				continue;
			}

			final int eI = a.indexOf('=',pI);
			// 包括=0也不处理，如：--=abc，这个形式可能是普通的传参，传的就是"--=abc"这个字符串
			// 而非使用本类定义的形式
			if (eI <= 0) {
				continue;
			}

			// 从--后第一个字符开始
			if (eI < PREFIX.length()) {
				continue;
			}

			final String key = a.substring(pI + PREFIX.length(), eI);

			if (keyContainsSpace(key)) {
				continue;
			}

			// FIXME 2025年8月25日 下午7:52:10 zhangzhen: value 要继续处理前后中分别含有空格的情况，
			// 并且规定为命令行中用""抱起来
			final String value = a.substring(eI + 1);
//			System.out.println("key = " + key);
//			System.out.println("value = " + value);

			r.add(new ArgR(key, value));

		}
		return r;
	}

	/**
	 * key前、后、中都不允许有空格，否则忽略此项
	 *
	 * @param key
	 * @return
	 */
	private static boolean keyContainsSpace(final String key) {
		for (int i = 0; i < key.length(); i++) {
			if (key.charAt(i) == SPACE) {
				return true;
			}
		}

		return false;
	}

}
