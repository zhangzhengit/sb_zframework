package com.vo.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
@Data
@AllArgsConstructor
public class ZSession {

	private final Map<String, Object> map = new HashMap<>(2, 1F);

	private final String id;
	private final Date createTime;
	private Date lastAccessedTime;
	private int intervalSeconds;

	private final AtomicBoolean invalidate = new AtomicBoolean(false);


	public ZSession(final String id, final Date createTime) {
		this.id = id;
		this.createTime = createTime;
	}

	public long getCreationTime() {
		this.checkInvalidate();
    	return this.createTime.getTime();
    }

	public String getId() {
		this.checkInvalidate();
		return this.id;
	}

    public long getLastAccessedTime() {
    	this.checkInvalidate();
		if (this.lastAccessedTime == null) {
			return -1L;
    	}
    	return this.lastAccessedTime.getTime();
    }

    public void setMaxInactiveInterval(final int interval) {
    	this.checkInvalidate();
    	this.intervalSeconds = interval;
    }

    public int getMaxInactiveInterval() {
    	this.checkInvalidate();
    	return this.intervalSeconds;
    }

    public void setAttribute(final String name, final Object value) {
    	this.checkInvalidate();
    	this.map.put(name, value);
    }

    public Object getAttribute(final String name) {
    	this.checkInvalidate();
    	return this.map.get(name);
    }

    public void invalidate() {
    	this.invalidate.set(true);
    	// FIXME 2023年7月2日 上午5:59:22 zhanghen: 从ZSessionMap中删除本对象
    }

	private void checkInvalidate() {
		if (this.invalidate.get()) {
			throw new IllegalArgumentException(ZSession.class.getCanonicalName() + " 已销毁，当前不可用");
		}
	}

}
