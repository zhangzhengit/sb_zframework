package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
@Getter
@AllArgsConstructor
public enum ContentTypeEnum {

	TEXT("Content-Type: text/plain;charset=UTF-8", "text/plain"),

	JSON("Content-Type: application/json;charset=UTF-8", "application/json"),

	HTML("Content-Type: text/html;charset=UTF-8", "text/html"),

	AUDIO_MP3("Content-Type: audio/mp3;", "audio/mp3"),

	;

	private String value;
	private String type;
}
