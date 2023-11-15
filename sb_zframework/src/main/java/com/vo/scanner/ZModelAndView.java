package com.vo.scanner;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZHtml 接口返回的视图名称和数据
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZModelAndView {

	/**
	 * html名称
	 */
	private String resourceName;

	/**
	 * html中对应的数据
	 */
	private Map<String, Object> map;

}
