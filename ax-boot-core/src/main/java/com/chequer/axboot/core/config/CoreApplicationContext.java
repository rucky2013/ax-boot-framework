package com.chequer.axboot.core.config;

import com.chequer.axboot.core.code.GlobalConstants;
import com.chequer.axboot.core.db.dbcp.AXBootDataSourceFactory;
import com.chequer.axboot.core.db.monitor.SqlMonitoringService;
import com.chequer.axboot.core.model.extract.service.jdbc.JdbcMetadataService;
import com.chequer.axboot.core.mybatis.AuditInterceptor;
import com.chequer.axboot.core.mybatis.MyBatisMapper;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true, mode = AdviceMode.PROXY)
@EnableJpaRepositories(basePackages = GlobalConstants.CORE_PACKAGE)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CoreApplicationContext {

    @Bean
    @Primary
    public DataSource dataSource(@Named(value = "axBootContextConfig") AXBootContextConfig axBootContextConfig) throws Exception {
        return AXBootDataSourceFactory.create(axBootContextConfig.getDataSourceConfig());
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setFieldMatchingEnabled(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, AXBootContextConfig axBootContextConfig) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan(GlobalConstants.CORE_PACKAGE);
        AXBootContextConfig.DataSourceConfig.HibernateConfig hibernateConfig = axBootContextConfig.getDataSourceConfig().getHibernateConfig();
        entityManagerFactory.setJpaVendorAdapter(hibernateConfig.getHibernateJpaVendorAdapter());
        entityManagerFactory.setJpaProperties(hibernateConfig.getAdditionalProperties());

        return entityManagerFactory;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean
    public SpringManagedTransactionFactory managedTransactionFactory() {
        SpringManagedTransactionFactory managedTransactionFactory = new SpringManagedTransactionFactory();
        return managedTransactionFactory;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(SpringManagedTransactionFactory springManagedTransactionFactory, DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setTypeAliasesPackage(GlobalConstants.CORE_DOMAIN_PACKAGE);
        sqlSessionFactoryBean.setPlugins(new Interceptor[]{new AuditInterceptor()});
        sqlSessionFactoryBean.setTransactionFactory(springManagedTransactionFactory);
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() throws Exception {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage(GlobalConstants.CORE_DOMAIN_PACKAGE);
        mapperScannerConfigurer.setMarkerInterface(MyBatisMapper.class);
        mapperScannerConfigurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return mapperScannerConfigurer;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) throws ClassNotFoundException {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
        return jpaTransactionManager;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    public JdbcMetadataService jdbcMetadataService() {
        return new JdbcMetadataService();
    }

    @Bean(name = "axBootContextConfig")
    public AXBootContextConfig axBootContextConfig() {
        return new AXBootContextConfig();
    }

    @Bean
    public SqlMonitoringService sqlMonitoringService(DataSource dataSource) throws Exception {
        return new SqlMonitoringService(dataSource);
    }

    @Bean
    public LocalValidatorFactoryBean validatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }
}
