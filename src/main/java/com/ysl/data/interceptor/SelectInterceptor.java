package com.ysl.data.interceptor;

import com.ysl.data.annotation.DataAuth;
import com.ysl.data.annotation.DataAuthRule;
import com.ysl.data.util.AuthSqlUtil;
import com.ysl.data.util.PermisssionUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

@Intercepts({
            @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
            @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
            })
@Slf4j
public class SelectInterceptor implements Interceptor {

    private static final Integer MAPPED_STATEMENT_INDEX = 0;
    private static final Integer PARAM_OBJ_INDEX = 1;
    private static final Integer ROW_BOUNDS_INDEX = 2;
    private static final Integer RESULT_HANDLER_INDEX = 3;
    private static final Integer CACHE_KEY_INDEX = 4;
    private static final Integer BOUND_SQL_INDEX = 5;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[MAPPED_STATEMENT_INDEX];
        Object parameter = args[PARAM_OBJ_INDEX];
        RowBounds rowBounds = (RowBounds) args[ROW_BOUNDS_INDEX];
        ResultHandler resultHandler = (ResultHandler) args[RESULT_HANDLER_INDEX];
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql;
        //由于逻辑关系，只会进入一次
        if (args.length == 4) {
            //4 个参数时
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            //6 个参数时
            cacheKey = (CacheKey) args[CACHE_KEY_INDEX];
            boundSql = (BoundSql) args[BOUND_SQL_INDEX];
        }
        //TODO 自己要进行的各种处理
        DataAuth dataAuth = PermisssionUtil.getPermissionByDelegate(ms);
        if (null == dataAuth) {
            return invocation.proceed();
        }

        MappedStatement newMappedStatement = setCurrentSql(ms, parameter, boundSql, permissionSql(boundSql.getSql(), dataAuth));
        args[MAPPED_STATEMENT_INDEX] = newMappedStatement;

        //注：下面的方法可以根据自己的逻辑调用多次，在分页插件中，count 和 page 各调用了一次
        return executor.query(newMappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }



    /**
     权限关联
     @param sql 拦截的原始sql
     @param dataAuth mapper上的注解
     */
    private String permissionSql(String sql, DataAuth dataAuth) {
        String authSql = getDataPermission(dataAuth.rule());
        log.info("权限SQL：{}" + authSql);
        try {
            sql = AuthSqlUtil.addWhereCondition(sql, authSql);
        } catch (JSQLParserException e) {
            e.printStackTrace();
            log.error("按照权限过滤注解拼接过滤条件时解析sql失败了！");
        }
        return sql;
    }

    /**
     获取数据权限后，需要拼接的过滤条件
     @param rule 规则
     @return 待拼接的sql过滤条件
     */
    private String getDataPermission(DataAuthRule[] rule) {
        String sql = " ";
        //按权限标志过滤数据
        for (int i = 0; i < rule.length; i++) {
            if (DataAuthRule.TAG_AUTH == (rule[i])) {
                //todo 获取当前用户权限标志进行拼接sql, 目前用1=1测试
                sql += "1=1";
            }
        }
        return sql;
    }


    /**
     设置sql
     @param mappedStatement
     @param paramObj
     @param boundSql
     @param sql
     @return
     */
    private MappedStatement setCurrentSql(MappedStatement mappedStatement, Object paramObj, BoundSql boundSql, String sql) {
        BoundSqlSource boundSqlSource = new BoundSqlSource(boundSql);
        MappedStatement newMappedStatement = copyFromMappedStatement(mappedStatement, boundSqlSource);
        MetaObject metaObject = MetaObject.forObject(newMappedStatement, new DefaultObjectFactory(), new DefaultObjectWrapperFactory(), new DefaultReflectorFactory());
        metaObject.setValue("sqlSource.boundSql.sql", sql);
        return newMappedStatement;
    }

    /**
     获取MappedStatement
     @param invo
     @return
     */
    private MappedStatement getMappedStatement(Invocation invo) {
        Object[] args = invo.getArgs();
        Object mappedStatement = args[MAPPED_STATEMENT_INDEX];
        return (MappedStatement) mappedStatement;
    }

    /**
     自定义私有SqlSource
     */
    private class BoundSqlSource implements SqlSource {

        private BoundSql boundSql;

        private BoundSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    /**
     copy
     @param ms
     @param newSqlSource
     @return
     */
    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(ms.getKeyProperties()[0]);
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }
}