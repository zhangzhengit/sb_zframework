package com.vo.template;

import java.util.Date;

import lombok.Data;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月14日
 *
 */
@Data
public class ArticleEntity {

	// FIXME 2023年7月19日 下午9:27:19 zhanghen: 测试类，删除
	private Integer id;
	private String title;
	private String content;
	private Integer viewCount;
	private Integer likesCount;
	private Integer commentsCount;
	private Integer status;
	private Integer isDelete;
	private Date createTime;
	private Integer createBy;
	private Date updateTime;
	private Integer updateBy;
	private String remark;
	private Integer author;

}
