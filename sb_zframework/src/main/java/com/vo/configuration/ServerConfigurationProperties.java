package com.vo.configuration;

import java.util.Map;

import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZValue;
import com.vo.core.QPSEnum;
import com.vo.enums.ZSessionStorageTypeEnum;
import com.vo.validator.ZCustom;
import com.vo.validator.ZDivisibleByCounterClientQPS_MIN;
import com.vo.validator.ZDivisibleByCounterQPS_MIN;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
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
public class ServerConfigurationProperties {

	/**
	 * 启动的端口号
	 */
	@ZNotNull
	@ZMin(min = 1)
	private Integer port = 80;

	/**
	 * response 响应头中是否包含 Cookie (ZSESSIONID)
	 */
	@ZNotNull
	@ZValue(name = "server.response.z.session.id", listenForChanges = true)
	private Boolean responseZSessionId = false;

	/**
	 * server的name，用于响应头中的Server字段
	 */
	@ZNotNull
	private String name = "ZServer";

	/**
	 * 读取http请求的ByteBuffer的容量大小
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 500000)
	private Integer byteBufferSize = 1024 * 2;

	/**
	 * 是否开启SSL
	 */
	@ZNotNull
	private Boolean sslEnable = false;

//	@ZNotNull
	private String sslKeyStore;

//	@ZNotNull
	private String sslPassword;

//	@ZNotNull
	private String sslType;

	/**
	 * 处理http请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private Integer threadCount = Runtime.getRuntime().availableProcessors() * 2;

	/**
	 * 是否启用静态资源的缓存
	 */
	@ZNotNull
	@ZValue(name = "server.static.resource.cache.enable", listenForChanges = true)
	private Boolean staticResourceCacheEnable = true;

	/**
	 * 扫描的包配置，如：com.vo
	 */
	@ZNotNull
	private String scanPackage = "com.vo";

	/**
	 * 是否启用QPS限制 (server.qps)
	 */
	@ZNotNull
	@ZValue(name = "server.qps.limit.enabled", listenForChanges = true)
	private Boolean qpsLimitEnabled = true;

	@ZNotNull
	// FIXME 2023年7月1日 上午4:21:59 zhanghen:  @ZMin在此设为0作为一个feature？可以配置为0让应用拒绝一切服务
//	@ZMin(min = 0)
	@ZMin(min = ZDivisibleByCounterQPS_MIN.MIN_VALUE)
	@ZMax(max = ZDivisibleByCounterQPS_MIN.MAX_VALUE)
	@ZValue(name = "server.qps", listenForChanges = true)
	@ZCustom(cls = ZDivisibleByCounterQPS_MIN.class)
	// FIXME 2023年11月15日 下午3:02:12 zhanghen: TODO 是否限制同一个clientip短时间内高频率访问（脚本刷）？
	// 如果不限制的话，是否其他ip的请求优先处理？
	private Integer qps = QPSEnum.SERVER.getDefaultValue();

	/**
	 * 访问超过 本类 qps限制时给客户端的提示语
	 */
	@ZNotEmtpy
	private String qpsExceedMessage = "访问频繁，请稍后再试";

	@ZMin(min = ZDivisibleByCounterClientQPS_MIN.MIN_VALUE)
	@ZMax(max = ZDivisibleByCounterClientQPS_MIN.MAX_VALUE)
	@ZValue(name = "server.client.qps", listenForChanges = true)
	@ZCustom(cls = ZDivisibleByCounterClientQPS_MIN.class)
	private Integer clientQps = QPSEnum.CLIENT.getDefaultValue();

	/**
	 * 是否启用内置的 StaticController,
	 * 注意：如果设为false不启用，则需要手动添加Controller处理 StaticController 类里的
	 * 静态资源
	 */
	@ZNotNull
	private Boolean staticControllerEnable = true;

	/**
	 * 长连接超时时间，一个长连接超过此时间则关闭，单位：秒
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 86400)
	// FIXME 2023年7月4日 下午6:57:06 zhanghen: TODO 改为：从连接最后一次活动开始计时，超过此值再关闭
	private Integer keepAliveTimeout = 60 * 10;

	/**
	 * session 存储类型
	 */
	@ZNotEmtpy
	@ZCustom(cls = ZSessionStorageTypeValidator.class)
	private String sessionStorageType = ZSessionStorageTypeEnum.MEMORY.name();

	/**
	 * session超时秒数，超时此值则销毁session
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	@ZValue(name = "server.session.timeout", listenForChanges = true)
	private Long sessionTimeout = 60 * 30L;

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
	private String staticPrefix = "/static";

	/**
	 * 是否开启gzip压缩
	 */
	@ZNotNull
	private Boolean gzipEnable = true;

	/**
	 * 开启gzip的content-type,如需配置多个，则用,隔开，如： text/html,text/css
	 */
	@ZNotNull
	private String gzipTypes = "text/html,text/css,application/json";

	/**
	 * 资源大于多少KB才启用gzip压缩
	 */
	@ZNotNull
	@ZMin(min = 1)
	// FIXME 2023年7月2日 上午1:31:33 zhanghen: gzipMinLength这个值用上
	private Integer gzipMinLength = 1;

	/**
	 * 支持的自定义响应头header，如：解决CORS问题，配置如下：
	 * server.responseHeaders.Access-Control-Allow-Origin=*
	 */
	private Map<String, String> responseHeaders;

	/**
	 * 程序启动时是否打印 @ZConfigurationProperties 配置类信息
	 */
	@ZNotNull
	private Boolean printConfigurationProperties = false;

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