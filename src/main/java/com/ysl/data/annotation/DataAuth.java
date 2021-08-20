package com.ysl.data.annotation;

import java.lang.annotation.*;

/**
 该注解如果加在类上的话可配合DataNotAuth注解使用。
 当mapper接口添加该注解后mapper内的所有方法都会进行权限过滤（如果指定方法无需过滤权限需要在该方法上添加DataNotAuth注解）
 @author yousili
 @since 2021/8/16 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataAuth {

    /**
     @return 进行数据权限处理的规则，可支持多规则
     */
    DataAuthRule[] rule() default {};

    /**
     只要目的是解决继承BaseMapper的方法无法添加注解的问题如（selectList等）
     @return 需要拦截的MAPPER接口方法名；如果写了则只会权限操作与该方法名相同的方法（如果该方法上添加了@DataNotAuth则不会权限操作该方法），不写则不会根据方法名筛选
     */
    String[] methodNames() default {};

}