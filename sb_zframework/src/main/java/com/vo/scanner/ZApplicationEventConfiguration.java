package com.vo.scanner;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@ZConfiguration
public class ZApplicationEventConfiguration {

	@ZAutowired
	private ZApplicationEventConfigurationProperties applicationEventConfigurationProperties;

	@ZBean
	public ZE zeForApplicationEventPublisher() {
		return ZES.newZE(this.applicationEventConfigurationProperties.getThreadCount(), this.applicationEventConfigurationProperties.getThreadNamePrefix());
	}
}
