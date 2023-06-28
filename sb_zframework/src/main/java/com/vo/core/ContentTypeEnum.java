package com.vo.core;

import org.apache.commons.configuration.PropertiesConfiguration.IOFactory;

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

	PDF("Content-Type: application/pdf;", "application/pdf"),

	XML("Content-Type: application/xml;charset=UTF-8", "application/xml"),
	HTML("Content-Type: text/html;charset=UTF-8", "text/html"),

	MP3("Content-Type: audio/mp3;", "audio/mp3"),

	CSS("Content-Type: text/css;", "text/css"),

	GIF("Content-Type: image/gif;", "image/gif"),

	JPGE("Content-Type: image/jpeg;", "image/jpeg"),

	PNG("Content-Type: image/png;", "image/png"),

	MP4("Content-Type: video/mp4;", "video/mp4"),

	WORD("Content-Type: application/msword;", "application/msword"),
	JPG("Content-Type: image/jpg;", "image/jpg"),

	JS("Content-Type: application/javascript;", "application/javascript"),

	;

	public static ContentTypeEnum gType(final String fileNameSuffix) {
		if (fileNameSuffix.endsWith("js")) {
			return JS;
		}
		if (fileNameSuffix.endsWith("doc") || fileNameSuffix.endsWith("docx")) {
			return ContentTypeEnum.WORD;
		}

		final ContentTypeEnum[] vs = values();
		for (final ContentTypeEnum ee : vs) {
			if (ee.getType().endsWith(fileNameSuffix)) {
				return ee;
			}

		}

		return null;
	}
	private String value;
	private String type;
}
