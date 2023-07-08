package com.vo.anno;

import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 * @ZAsync 配置类
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZConfiguration
public class ZAsyncConfiguration {

	@ZAutowired
	private ZAsyncProperties zAsyncProperties;

	@ZBean
	public ZE zeZAsync() {
		return ZES.newZE(this.zAsyncProperties.getThreadNamePrefix(), this.zAsyncProperties.getThreadNamePrefix());
	}

}
