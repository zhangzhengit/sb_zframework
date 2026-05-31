package vo.zframework;

import java.util.List;

/**
 * 程序启动信息
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
public class ZApplicationStartupInfo {

	private final List<String> packageNameList;
	private final boolean httpEnable;
//	private final Integer port;
	private final String[] args;

	public List<String> getPackageNameList() {
		return packageNameList;
	}

	public boolean isHttpEnable() {
		return httpEnable;
	}

	public String[] getArgs() {
		return args;
	}

	public ZApplicationStartupInfo(List<String> packageNameList, boolean httpEnable, String[] args) {
		super();
		this.packageNameList = packageNameList;
		this.httpEnable = httpEnable;
		this.args = args;
	}
}
