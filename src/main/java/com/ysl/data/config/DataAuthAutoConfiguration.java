package com.ysl.data.config;

import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import com.ysl.data.interceptor.SelectInterceptor;
import com.ysl.data.interceptor.UpdateInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 @author yousili
 @since 2021/8/16 */

@Configuration
@AutoConfigureAfter(PageHelperAutoConfiguration.class)
public class DataAuthAutoConfiguration {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addInterceptor() {
        SelectInterceptor s = new SelectInterceptor();
        UpdateInterceptor u = new UpdateInterceptor();
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(s);
            sqlSessionFactory.getConfiguration().addInterceptor(u);
        }
    }

}
