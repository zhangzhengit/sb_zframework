package vo.vortex;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import vo.vortex.core.ZContext;
import vo.vortex.event.ZApplicationEvent;
import vo.vortex.event.ZApplicationEventPublisher;

/**
 * 程序启动的信息
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
public final class ZApplicationContext {

	/**
	 * 程序启动扫描的包铭
	 */
	private final List<String> scanPackageNameList;

	/**
	 * 是否启用http服务器
	 */
	private final boolean httpEnable;

	/**
	 * java命令行中传来的参数
	 */
	private final String[] args;

	/**
	 * 配置文件信息
	 */
	private final Properties properties;

	/**
	 * 获取容器中所有的bean
	 *
	 * @return
	 *
	 */
	public Collection<Object> getBeans() {
		final Map<String, Object> map = ZContext.all();
		final Collection<Object> values = map.values();
		return values;
	}

	/**
	 * 获取容器中所有的bean的名称
	 *
	 * @return
	 *
	 */
	public Set<String> getBeanNames() {
		final Map<String, Object> map = ZContext.all();
		final Set<String> ks = map.keySet();
		return ks;
	}

	/**
	 * 获取容器中的beanMap
	 *
	 * @return
	 *
	 */
	public Map<String, Object> getBeanMap() {
		return ZContext.all();
	}


	/**
	 * 手动注册一个bean
	 *
	 * @param beanClass
	 * @param bean
	 *
	 */
	public void registerBeanDefinition(final Class<?> beanClass, final Object bean) {
		ZContext.addBean(beanClass, bean);
	}

	/**
	 * 手动注册一个bean
	 *
	 * @param beanName
	 * @param bean
	 *
	 */
	public void registerBeanDefinition(final String beanName, final Object bean) {
		ZContext.addBean(beanName, bean);
	}

	/**
	 * 发布一个事件
	 *
	 * @param event
	 *
	 */
	public void publishEvent(final ZApplicationEvent event) {
		ZContext.getBean(ZApplicationEventPublisher.class).publishEvent(event);
	}

	public List<String> getScanPackageNameList() {
		return this.scanPackageNameList;
	}

	public boolean isHttpEnable() {
		return this.httpEnable;
	}

	public String[] getArgs() {
		return this.args;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public ZApplicationContext(final List<String> scanPackageNameList, final boolean httpEnable, final String[] args,
			final Properties properties) {
		this.scanPackageNameList = scanPackageNameList;
		this.httpEnable = httpEnable;
		this.args = args;
		this.properties = properties;
	}

}
