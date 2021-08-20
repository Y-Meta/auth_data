package com.ysl.data.util;

import com.ysl.data.annotation.DataAuth;
import com.ysl.data.annotation.DataNotAuth;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 @author yousili
 @since 2021/8/18 */
public class PermisssionUtil {
    /**
     * pagehelper自动生成的count查询会在MappedStatement中生成一个id为原sql的id+"_COUNT"
     */
    private static final String COUNT_PRE = "_COUNT";

    /**
     获取Mapper接口方法上的数据权限注解信息，判断当前拦截方法是否需要进行权限过滤
     @param mappedStatement
     @return 需要权限过滤返回DataAuth对象，不需要则返回null
     */
    public static DataAuth getPermissionByDelegate(MappedStatement mappedStatement) throws Exception {

        String id = mappedStatement.getId();
        //统计SQL取得注解也是实际查询id上得注解，所以需要去掉_COUNT
        if (id.contains(COUNT_PRE)) {
            id = id.replace(COUNT_PRE, "");
        }
        String className = id.substring(0, id.lastIndexOf("."));
        String methodName = id.substring(id.lastIndexOf(".") + 1);
        final Class<?> cls = Class.forName(className);
        Annotation[] annotations = cls.getAnnotations();
        boolean classHaveAnnotation = false;
        for (Annotation annotation : annotations) {
            //类上有@DataAuth注解
            if (annotation instanceof DataAuth) {
                classHaveAnnotation = true;
                DataAuth dataAuth = (DataAuth) annotation;
                List<String> methodNames = Arrays.asList(dataAuth.methodNames());
                //注解值无添加方法名称
                final Method[] method = cls.getMethods();
                if (methodNames.size() == 0) {
                    for (Method me : method) {
                        if (me.getName().equals(methodName) && !me.isAnnotationPresent(DataNotAuth.class)) {
                            return (DataAuth) annotation;
                        }
                    }
                    //注解值有添加方法名称, 仅权限过滤包含在方法名称数组里的方法
                } else {
                    for (Method me : method) {
                        //当拦截的方法名被包含于注解的methodNames值中，且该方法上不含有@DataNotAuth注解时进行权限过滤操作
                        if (me.getName().equals(methodName) && methodNames.contains(me.getName()) && !me.isAnnotationPresent(DataNotAuth.class)) {
                            return (DataAuth) annotation;
                        }
                    }
                }
            }
        }
        //当类上无@dataauth注解时
        if (!classHaveAnnotation){
            //当前拦截的方法上有注解
            final Method[] method = cls.getMethods();
            for (Method me : method) {
                if (me.getName().equals(methodName) && me.isAnnotationPresent(DataAuth.class)) {
                    return me.getAnnotation(DataAuth.class);
                }
            }
        }


        return null;
    }
}
