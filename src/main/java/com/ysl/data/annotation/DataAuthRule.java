package com.ysl.data.annotation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 @author yousili
 @since 2021/8/16 */
@Getter
@AllArgsConstructor
public enum DataAuthRule {

    TAG_AUTH("tag_auth"),
    ROLE_AUTH("role_auth"),
    ORG_AUTH("org_auth"),
    DEP_AUTH("dep_auth"),
    /**
     * 根据传入的sql先执行该sql通过判断执行结果是否为空判断是否有权限
     */
    SQL_AUTH("sql_auth");
    private String type;

}
