package com.vo.anno;

import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 * @ZAsync 用到的线程池
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@ZComponent
public class ZAsyncExecutors {

	private static ZE ze;

	@ZValue(name = "async.thread.count")
	private static Integer threadCount;

	@ZValue(name = "async.thread.name.prefix")
	private static String threadNamePrefix;

	public synchronized static ZE getInstance() {
		if (ze == null) {
			ze = ZES.newZE(threadCount, threadNamePrefix);
		}

		return ze;
	}

}
