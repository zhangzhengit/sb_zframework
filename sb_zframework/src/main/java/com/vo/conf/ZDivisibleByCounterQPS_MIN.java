package com.vo.conf;

import java.lang.reflect.Field;

import com.vo.core.ZServer;
import com.vo.validator.ValidatedException;
import com.vo.validator.ZCustom;
import com.vo.validator.ZCustomValidator;

/**
 * 必须可以被 ZServer.Counter.QPS_MIN 整除
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
public class ZDivisibleByCounterQPS_MIN implements ZCustomValidator {

	@Override
	public void validated(final Object object, final Field field) throws Exception {

		try {
			field.setAccessible(true);
			final Object value = field.get(object);
			final Integer v = (Integer) value;

			if (v % ZServer.Counter.QPS_MIN != 0) {
				final String message = field.getAnnotation(ZCustom.class).message();

				final String t = object.getClass().getSimpleName() + "." + field.getName() + " 必须配置为可以被 "
						+ ZServer.Counter.QPS_MIN + " (" + ZServer.Counter.class.getCanonicalName() + ".QPS_MIN)"
						+ " 整除";
				final String format = String.format(message, t);
				throw new ValidatedException(format);

			}

		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw e;
		}

	}

}
