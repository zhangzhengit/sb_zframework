package com.vo.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
@Getter
@AllArgsConstructor
public enum ZTEnum {

	LIST("@list[]") {
		@Override
		public String generateKeyword(final String s) {
			return "@list[" + s + "]";
		}

		@Override
		public String generateMathchKeyword(final String s) {
			return "@list\\[" + s + "\\]";
		}
	},

	VALUE("@value[]") {
		@Override
		public String generateKeyword(final String s) {
			return "@value[" + s + "]";
		}

		@Override
		public String generateMathchKeyword(final String s) {
			return "@value\\[" + s + "\\]";
		}

	},

	SWITCH("@switch[]") {
		@Override
		public String generateMathchKeyword(final String s) {
			return "@switch\\[" + s + "\\]";
		}

		@Override
		public String generateKeyword(final String s) {
			return "@switch[" + s + "]";
		}
	}

	;

	private String value;


//	public static void toMatchString(String string) {
//		string.replace(string, string)
//	}
	public abstract String generateMathchKeyword(String s);

	public abstract String generateKeyword(String s);

}
