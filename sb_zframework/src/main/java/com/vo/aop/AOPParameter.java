package com.vo.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.List;

import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;

import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
// FIXME 2023年6月18日 下午2:34:35 zhanghen:  用此类作为beofre和after方法参数
public class AOPParameter {
	private String methodName;
	private Method method;

	private Boolean isVOID;

	private List<Object> parameterList;

	private Object target;

	public Object invoke() {

		try {

			if (Boolean.TRUE.equals(this.getIsVOID())) {
				this.method.invoke(this.target, this.parameterList.toArray());
				return null;
			}

			return this.method.invoke(this.target, this.parameterList.toArray());

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}
}