package com.vo.validator;

import com.vo.anno.ZConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @ZLength 的message 配置
 *
 * @author zhangzhen
 * @date 2023年10月31日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "validator.constraints.length")
public class ZLengthMessage {

	@ZNotEmtpy
	private String message = "[%s]长度必须在[%s]和[%s]之间,当前值[%s]";

}
