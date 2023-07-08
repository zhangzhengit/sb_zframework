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
//@ZConfiguration
// FIXME 2023年7月8日 上午10:28:15 zhanghen: TODO 此方式
// （@ZConfiguration 里面用 @ZAutowired）还不支持，因为ZMain里面是按顺序扫描各个注解来设值的
// 扫描 @ZConfiguration 在扫描 @ZAutowired 之前，所以此方式暂不支持
public class ZAsyncConfiguration {

	@ZAutowired
	private ZAsyncProperties zAsyncProperties;

	@ZBean
	public ZE zeZAsync() {
		return ZES.newZE(this.zAsyncProperties.getThreadNamePrefix(), this.zAsyncProperties.getThreadNamePrefix());
	}

}
