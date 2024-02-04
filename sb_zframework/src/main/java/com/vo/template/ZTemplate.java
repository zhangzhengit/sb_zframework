package com.vo.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * freemarker模板工具类
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZTemplate {

	private static final Configuration CFG = new Configuration();
//	private static final Configuration CFG = new Configuration(Configuration.VERSION_2_3_31);

	/**
	 * 从一个带有freemarker标签的html文档的字符串形式，处理其中的freemarker标签，而生成一个完整的可以被浏览器直接解析的html文档。
	 *
	 * @param htmlContent
	 * @return
	 *
	 */
	public static String freemarker(final String htmlContent) {
		return freemarker0(htmlContent);
	}

	private static String freemarker0(final String templateString) {

		try {

			CFG.setClassForTemplateLoading(ZTemplate.class, "/static");

			final Template template = new Template("template-" + templateString.hashCode(), templateString, CFG);
			final StringWriter writer = new StringWriter();
			final Map<String, Object> dataModel = ZModel.get();
			template.process(dataModel, writer);
			final String output = writer.toString();
			return output;
		} catch (IOException | TemplateException e) {
			e.printStackTrace();
		}

		return templateString;
	}

}
