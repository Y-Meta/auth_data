package com.ysl.data.interceptor;

import com.ysl.data.annotation.DataAuth;
import com.ysl.data.util.AuthSqlUtil;
import com.ysl.data.util.PermisssionUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 更新、插入语句权限拦截插件
 @author yousili
 @since 2021/8/18 */

@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
@Slf4j
public class UpdateInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement)invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = boundSql.getSql();

        DataAuth dataAuth = PermisssionUtil.getPermissionByDelegate(mappedStatement);
        if (null == dataAuth) {
            return invocation.proceed();
        }
        MappedStatement newMappedStatement = null;
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (SqlCommandType.INSERT.equals(sqlCommandType)){
            Statement statement = CCJSqlParserUtil.parse(sql);
            Insert insert = (Insert) statement;
            //TODO 根据业务需要对insert语句的列字段进行扩充
            List<String> addColum = new ArrayList<>();
            addColum.add("test");
            Insert insert1 = AuthSqlUtil.addInsertColumn(insert, addColum);
            //TODO 根据业务需要对insert语句的值字段进行扩充
            List<String> addValues = new ArrayList<>();
            addValues.add("test");
            Insert newInsert = AuthSqlUtil.addInsertValues(insert1, addValues);
           newMappedStatement = setCurrentSql(mappedStatement, parameter, boundSql, newInsert.toString());
        }else if (SqlCommandType.UPDATE.equals(sqlCommandType)){
            Statement statement = CCJSqlParserUtil.parse(sql);
            Update update = (Update) statement;
            //TODO 根据业务需要对update语句的SET列字段进行扩充
            List<String> addColum = new ArrayList<>();
            addColum.add("test");
            Update update1 = AuthSqlUtil.addUpdateColumn(update, addColum);
            //TODO 根据业务需要对update语句的SET值字段进行扩充
            List<String> addValues = new ArrayList<>();
            addValues.add("test");
            Update update2 = AuthSqlUtil.addUpdateValues(update1, addValues);
            //TODO 根据业务需要添加update语句的where条件
            String addWhere = "1=1";
            Update newUpdate = AuthSqlUtil.addUpdateWhere(update2, addWhere);
            newMappedStatement = setCurrentSql(mappedStatement, parameter, boundSql, newUpdate.toString());

        }
        Executor executor = (Executor) invocation.getTarget();
        return executor.update(newMappedStatement, parameter);
    }
    private void updateFeild(Field[] declaredFields, Object parameter, SqlCommandType sqlCommandType) throws IllegalAccessException {
        for (Field field: declaredFields){
            if (SqlCommandType.INSERT.equals(sqlCommandType)){
                if (field.getName().equals("createTime")){
                    field.setAccessible(true);
                    field.set(parameter,new Date());
                }
            }else if (SqlCommandType.UPDATE.equals(sqlCommandType)){
                if (field.getName().equals("updateTime")){
                    field.setAccessible(true);
                    field.set(parameter,new Date());
                }
            }
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

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
