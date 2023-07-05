package com.vo.conf;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotNull;
import com.vo.validator.ZStartWith;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * server相关的配置
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "server")
public class ServerConfiguration {

	/**
	 * 启动的端口号
	 */
	@ZNotNull
	@ZMin(min = 1)
	private Integer port;

	/**
	 * 是否开启SSL
	 */
	@ZNotNull
	private Boolean sslEnable;

	@ZNotNull
	private String sslKeyStore;

	@ZNotNull
	private String sslPassword;

	@ZNotNull
	private String sslType;

	/**
	 * 处理http请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private Integer threadCount;

	/**
	 * 	扫描的包配置，如：com.vo
	 */
	@ZNotNull
	private String scanPackage;

	@ZNotNull
	// FIXME 2023年7月1日 上午4:21:59 zhanghen:  @ZMin在此设为0作为一个feature？可以配置为0让应用拒绝一切服务
//	@ZMin(min = 0)
	@ZMin(min = 1)
	@ZMax(max = 52000000)
	private Integer concurrentQuantity;

	/**
	 * 是否启用内置的 StaticController,
	 * 注意：如果设为false不启用，则需要手动添加Controller处理 StaticController 类里的
	 * 静态资源
	 */
	@ZNotNull
	private Boolean staticControllerEnable;
	/**
	 * 长连接超时时间，一个长连接超过此时间则关闭，单位：秒
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	// FIXME 2023年7月4日 下午6:57:06 zhanghen: TODO 改为：从连接最后一次活动开始计时，超过此值再关闭
	private Integer keepAliveTimeout;

	/**
	 * session超时秒数，超时此值则销毁session
	 */
	@ZNotNull
	@ZMax(max = Integer.MAX_VALUE)
	private Integer sessionTimeout;

	/**
	 * 配置硬盘上的资源目录，如：E\\x
	 * 此值配置了，则优先读取此值下的资源文件
	 * 此值没配置，则读取 staticPrefix 目录下的资源文件
	 */
	private String staticPath;

	/**
	 * 配置读取程序内resources下的资源,
	 * 相对于 resources 目录静态资源的目录，
	 * 如： 配置为 /static，则读取目录为 resources/static
	 */
	@ZNotNull
	@ZStartWith(prefix = "/")
	private String staticPrefix;

	/**
	 * 是否开启gzip压缩
	 */
	@ZNotNull
	private Boolean gzipEnable;

	/**
	 * 开启gzip的content-type,如需配置多个，则用,隔开，如： text/html,text/css
	 */
	@ZNotNull
	private String gzipTypes;

	/**
	 * 资源大于多少KB才启用gzip压缩
	 */
	@ZNotNull
	@ZMin(min = 1)
	// FIXME 2023年7月2日 上午1:31:33 zhanghen: gzipMinLength这个值用上
	private Integer gzipMinLength;

	public boolean gzipContains(final String contentType) {
		final String[] a = this.getGzipContentType();
		for (final String string : a) {
			if (string.equals(contentType)) {
				return true;
			}
		}

		return false;
	}

	public String[] getGzipContentType() {
		final String[] a = this.gzipTypes.split(",");
		return a;
	}

}