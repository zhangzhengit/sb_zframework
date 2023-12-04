package com.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 程序启动信息
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
@Data
@AllArgsConstructor
public class ZApplicationStartupInfo {

	private final List<String> packageNameList;
	private final boolean httpEnable;
//	private final Integer port;
	private final String[] args;
}
