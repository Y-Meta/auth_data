package com.ysl.data.util;

import com.hsae.cloud.common.model.entity.AuthUser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.List;

/**
 根据jsqlparser解析并操作sql，参考博客https://blog.csdn.net/u014297722/article/details/53256533
 @author yousili
 @since 2021/8/16 */
public class AuthSqlUtil {

    /**
     获取当前登录的用户信息
     @return AuthUser
     */
    public static AuthUser getAuthUser() {
        PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
        if (null != principals && !principals.isEmpty()) {
            Object primaryPrincipal = principals.getPrimaryPrincipal();
            if (primaryPrincipal instanceof AuthUser) {
                return (AuthUser) primaryPrincipal;
            }
        }
        return new AuthUser();
    }

    /**
     在原有的sql中增加新的where条件
     @param sql 原sql
     @param condition 新的and条件（eg:1=1）
     @return 新的sql
     */
    public static String addWhereCondition(String sql, String condition) throws JSQLParserException {
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        final Expression expression = plainSelect.getWhere();
        final Expression envCondition = CCJSqlParserUtil.parseCondExpression(condition);
        if (expression == null) {
            plainSelect.setWhere(envCondition);
        } else {
            AndExpression andExpression = new AndExpression(expression, envCondition);
            plainSelect.setWhere(andExpression);
        }
        return plainSelect.toString();
    }



    // ********* insert column
    public static Insert addInsertColumn(Insert insertStatement, List<String> strColumn)
            throws JSQLParserException {
        List<Column> insertColumn = insertStatement.getColumns();
        for (int i = 0; i < strColumn.size(); i++) {
            insertColumn.add((Column) CCJSqlParserUtil
                    .parseCondExpression(strColumn.get(i)));
        }
        return insertStatement;

    }

    // ********* insert values
    public static Insert addInsertValues(Insert insertStatement, List<String> strValue)
            throws JSQLParserException {
        List<Expression> insertValuesExpression = ((ExpressionList) insertStatement
                .getItemsList()).getExpressions();
        for (int i = 0; i < strValue.size(); i++) {
            insertValuesExpression.add((Expression) CCJSqlParserUtil
                    .parseExpression(strValue.get(i)));
        }
        return insertStatement;

    }
    // *********update column
    public static Update addUpdateColumn(Update updateStatement, List<String> strColumn)
            throws JSQLParserException {
        List<Column> updateColumn = updateStatement.getColumns();
        for (int i = 0; i < strColumn.size(); i++) {
            updateColumn.add((Column) CCJSqlParserUtil
                    .parseExpression(strColumn.get(i)));
        }
        return updateStatement;

    }
    // *********update values
    public static Update addUpdateValues(Update updateStatement, List<String> strValues)
            throws JSQLParserException {
        List<Expression> updateValues = updateStatement.getExpressions();
        for (int i = 0; i < strValues.size(); i++) {
            updateValues.add((Expression) CCJSqlParserUtil
                    .parseExpression(strValues.get(i)));
        }
        return updateStatement;

    }
    // *******update where
    public static Update addUpdateWhere(Update updateStatement, String strWhere)
            throws JSQLParserException {
        Expression whereExpression = updateStatement.getWhere();
        String where = whereExpression.toString() + " and " + strWhere;
        Expression newWhere = (Expression) (CCJSqlParserUtil
                .parseCondExpression(where));
        updateStatement.setWhere(newWhere);
        return updateStatement;

    }

}
