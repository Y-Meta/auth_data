package com.ysl.data.annotation;

import java.lang.annotation.*;

/**
 该注解表示该方法不需要进行权限认证，添加后将不会主动拼接权限过滤条件（用于通行mapper接口的一些关联查询操作）
 @author yousili
 @since 2021/8/17 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DataNotAuth {
}
