package com.vo.template;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Z模板，定义一组用于html中的标签，用java解析后，输出html内容
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
// FIXME 2023年6月27日 下午10:06:48 zhanghen:  TODO 同一个html中，一个list用多次还是有问题
public class ZTemplate {

//	public static void main(final String[] args) {
//		final String htmlContent =
//					"<h3>\r\n"
//				+ "		if: \r\n"
//				+ "		<@switch[i1]>\r\n"
//				+ "			<case 1> 	一\r\n"
//				+ "			<case 2> 	二\r\n"
//				+ "			<case 3> 	三\r\n"
//				+ "		</endswitch[i1]>\r\n"
//				+ "	</h3>";
//
//		final ZModel model = new ZModel();
////		model.set("list1", Lists.newArrayList(new ZTemplate.User("测试1", 1111)));
////		model.set("list2", Lists.newArrayList(new ZTemplate.User("测试2", 2222)));
////		model.set("list3", Lists.newArrayList(new ZTemplate.User("测试3", 3333)));
////		model.set("name", "zhangsan");
//		model.set("i1", "2");
//
//		final String r = ZTemplate.generate(htmlContent);
//
//		System.out.println("r = ");
//		System.out.println(r);
//	}

	public static String generate(final String htmlContent) {

		final String r1 = parseValue(htmlContent);
		final String r2 = parseList(r1);
		final String r3 = parseIf(r2);

		return r3;
	}

	public static String parseIf(String r) {
		final ZTEnum SWITCH = ZTEnum.SWITCH;
		int from = 0;
		for (final Entry<String, Object> entry : ZModel.get().entrySet()) {
			final String k = SWITCH.generateKeyword(entry.getKey());

			final int r1 = r.indexOf('<' + k,from);
			if (r1 > -1) {
				final int end1 = r.indexOf('>', r1);
				if (end1 > r1) {
					final SwitchPattern p = new SwitchPattern();
					final String start = r.substring(r1, end1 + 1);
					p.setStart(start);

					final String endK = "</endswitch[" + entry.getKey() + "]>";
					final int i2 = r.indexOf(endK, end1);
					if (i2 > end1) {
						final String end = r.substring(i2, i2 + endK.length());
						p.setEnd(end);

						final String content = r.substring(end1 + ">".length(), i2);
						p.setContent(content);

						p.setValue(entry.getValue());

						final String rx = p.generate();

						r = r.replace(p.getStart(), "").replace(p.getEnd(), "")
								.replace(p.getContent(), rx);
					} else {
						throw new IllegalArgumentException(SWITCH.getValue() + " 标签没有正确关闭");
					}
				} else {
					throw new IllegalArgumentException(SWITCH.getValue() + " 标签没有正确关闭");
				}

				from += k.length();
			}

		}
		return r;
	}

	private static String parseList(String r) {
		final ZTEnum list = ZTEnum.LIST;
		int from = 0;
		for (final Entry<String, Object> entry : ZModel.get().entrySet()) {
			final String k = list.generateKeyword(entry.getKey());
//			System.out.println("list-k = " + k);

			final int r1 = r.indexOf('<' + k,from);
			if (r1 > -1) {
				final int end1 = r.indexOf('>', r1);
				if (end1 > r1) {
					final ListPattern p = new ListPattern();
					final String start = r.substring(r1, end1 + 1);
					p.setStart(start);
//					System.out.println("start = " + start);

					final String endK = "</endlist[" + entry.getKey() + "]>";
					final int i2 = r.indexOf(endK, end1);
					if (i2 > end1) {
						final String end = r.substring(i2, i2 + endK.length());
						p.setEnd(end);
//						System.out.println("end = " + end);

						final String content = r.substring(end1 + ">".length(), i2);
//						System.out.println("content = " + content);
						p.setContent(content);

//						System.out.println("------------------p-------------------");
						p.setValue(entry.getValue());

//						System.out.println(p);
						final String rx = p.generate();

						r = r.replace(p.getStart(), "").replace(p.getEnd(), "")
								.replace(p.getContent(), rx);
//						System.out.println("rrrrr = " + r);
					} else {
						throw new IllegalArgumentException(ZTEnum.LIST.getValue() + " 标签没有正确关闭");
					}
				} else {
					throw new IllegalArgumentException(ZTEnum.LIST.getValue() + " 标签没有正确关闭");
				}

				from += k.length();
			}

		}
		return r;
	}

	private static String parseValue(String string) {
		final ZTEnum value = ZTEnum.VALUE;
		final Map<String, Object> map = ZModel.get();
		final Set<Entry<String, Object>> es = map.entrySet();
		for (final Entry<String, Object> entry : es) {
			final String k = value.generateMathchKeyword(entry.getKey());
//			System.out.println("value-k = " + k);
			string = string.replaceAll(k, String.valueOf(entry.getValue()));
		}

//		System.out.println("string = " + string);
		return string;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SwitchPattern {
		private String start;
		private String content;
		private String end;

		private Object value;
		public String generate() {
			final String keyword = this.start.replace("<@switch[", "").replace("]>", "");
			int from = 0;
			final StringBuilder r = new StringBuilder();
			while (true) {
				final int i = this.content.indexOf("<case", from);
				if (i < 0) {
					break;
				}

				final int endI = this.content.indexOf(">", i);
				final PV pv = new PV();
				pv.setValue(this.getValue());
				if (endI > i) {
					final String substring = this.content.substring(i, endI + ">".length());

					final String pV = substring.replace("<case", "").replace(">", "");
					final String pvv = pV.trim();
					pv.setExp(pvv);

					final int indexOf = this.content.indexOf("<case", endI + ">".length());
					if (indexOf > -1) {
						final String value = this.content.substring(endI + ">".length(), indexOf);
						pv.setContent(value);
					} else {
						final String value = this.content.substring(endI + ">".length(), this.content.length());
						pv.setContent(value);
					}
				}

				r.append(pv.g());

				from += i + "<case".length();
			}


			return r.toString();
		}

		@Data
		@AllArgsConstructor
		@NoArgsConstructor
		public static class PV {

			private Object value;
			private Object exp;
			private String content;

			public String g() {
				final StringBuilder builder = new StringBuilder();
				if (this.value.getClass().getCanonicalName().equals(String.class.getCanonicalName())) {
					final String v = String.valueOf(this.value);
					if (v.equals(String.valueOf(this.exp))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Byte.class.getCanonicalName())) {
					if (Byte.valueOf(String.valueOf(this.value)).equals(Byte.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Short.class.getCanonicalName())) {
					if (Short.valueOf(String.valueOf(this.value)).equals(Short.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Integer.class.getCanonicalName())) {
					if (Integer.valueOf(String.valueOf(this.value)).equals(Integer.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Long.class.getCanonicalName())) {
					if (Long.valueOf(String.valueOf(this.value)).equals(Long.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Float.class.getCanonicalName())) {
					if (Float.valueOf(String.valueOf(this.value)).equals(Float.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Double.class.getCanonicalName())) {
					if (Double.valueOf(String.valueOf(this.value)).equals(Double.valueOf(String.valueOf(this.exp)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Character.class.getCanonicalName())) {
					if (Character.valueOf(String.valueOf(this.value).charAt(0))
							.equals(Character.valueOf(String.valueOf(this.exp).charAt(0)))) {
						builder.append(this.content);
					}
				} else if (this.value.getClass().getCanonicalName().equals(Boolean.class.getCanonicalName()) && Boolean
						.valueOf(String.valueOf(this.value)).equals(Boolean.valueOf(String.valueOf(this.exp)))) {
					builder.append(this.content);
				}

				return builder.toString();
			}

		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ListPattern {
		private String start;
		private String content;
		private String end;

		private Object value;

		public String generate() {
//			System.out.println("value = " + this.value);

			final HashMap<String, String> filedMap = this.getFiled();
//			System.out.println("filedMap = " + filedMap);

			final StringBuilder r = new StringBuilder();

			if (this.value instanceof Iterable) {
				final Iterable<?> iterable = (Iterable<?>) this.value;
				for (final Object v : iterable) {
					this.do1(filedMap, r, v);
				}
			} else if (this.value.getClass().isArray()) {
				final Object[] array = (Object[]) this.value;
				for (final Object v : array) {
					this.do1(filedMap, r, v);
				}
			} else {
				throw new IllegalArgumentException(ZTEnum.LIST.getValue() + "标签的值类型不支持，value.getClass = "
						+ this.value.getClass().getCanonicalName() + " 请使用 java.lang.Iterable类型或数组类型");
			}

//			System.out.println("rxxx = " + r);

			return r.toString();

		}

		private void do1(final HashMap<String, String> filedMap, final StringBuilder r, final Object v) {
			final AtomicReference<String> cr = new AtomicReference<>();
			cr.set(this.content);
			for (final Entry<String, String> entry : filedMap.entrySet()) {
				final String fieldName = entry.getKey();

				try {
					final Field field = v.getClass().getDeclaredField(fieldName);
					field.setAccessible(true);
					final Object fieldValue = field.get(v);
//					System.out.println("fieldValue = " + fieldValue);
//					System.out.println("field = " + entry.getValue());

					final String ccc = cr.get().replace(String.valueOf(entry.getValue()),
							String.valueOf(fieldValue));
					cr.set(ccc);
//					System.out.println("ccc = " + ccc);

				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			r.append(cr.get());
		}

		private HashMap<String, String> getFiled() {
			final ArrayList<String> fieldList = Lists.newArrayList();
			final HashMap<String, String> map = Maps.newHashMap();
			final int asi = this.start.indexOf("as");
			if (asi > -1) {
				final int end = this.start.indexOf(">", asi);
				if (end > asi) {
					final String v = this.start.substring(asi + "as".length(), end);
//					System.out.println("v = " + v.trim());

					final String sss = "@value[" + v.trim();

					// 2
					int from = 0;
					while (true) {
						final int ci = this.content.indexOf(sss, from);
						if (ci > -1) {
							final int ci2 = this.content.indexOf("]", ci);
//							System.out.println("content 匹配 ");
							if (ci2 > ci) {
								final String field = this.content.substring(ci + sss.length() + ".".length(), ci2);
//								System.out.println("field  = " + field);
								fieldList.add(field);
								map.put(field, sss + "." + field + "]");

								from += sss.length();
							}
						}else {
							break;
						}
					}

				}
			}
//			System.out.println("map = " + map);
//			return fieldList;
			return map;
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class User {
		private String name;
		private Integer age;
	}
}
