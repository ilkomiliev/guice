package org.mybatis.guice.multidstest.combinedservice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.PrivateModule;
import com.google.inject.name.Names;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.builtin.PooledDataSourceProvider;
import org.mybatis.guice.datasource.helper.JdbcHelper;
import org.mybatis.guice.jta.simple.*;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by ilko on 11/10/15.
 */
public class MultiJDBCcombinedServiceTest {

    private CombinedService combinedService;

    @Before
    public void setup() throws Exception {
        Injector injector = getInjector();
        combinedService = injector.getInstance(CombinedService.class);
        System.out.format("CombinedService %s%n", combinedService);

        // create the schemas in the memory databases
        Schema1Service schema1Service = injector.getInstance(Schema1Service.class);
        Schema2Service schema2Service = injector.getInstance(Schema2Service.class);
        schema1Service.createSchema1();
        schema2Service.createSchema2();
    }

    @Test
    public void testRollBack() {
        try {
            combinedService.insert2RecordsIntoSchema1And1RecordIntoSchema2AndRollbackAll();
            fail("Expected an exception to force rollback");
        } catch (Exception e) {
            // ignore - expected
        }

        assertEquals(0, combinedService.getAllNamesFromSchema1().size());
        assertEquals(0, combinedService.getAllNamesFromSchema2().size());
    }

    private Injector getInjector() {
        return Guice.createInjector(new PrivateModule() {
            @Override
            protected void configure() {
                install(new MyBatisModule() {
                    @Override
                    protected void initialize() {
                        bindDataSourceProviderType(PooledDataSourceProvider.class);
                        bindTransactionFactoryType(JdbcTransactionFactory.class);

                        install(JdbcHelper.HSQLDB_IN_MEMORY_NAMED);

                        Properties connectionProps = new Properties();
                        connectionProps.setProperty("mybatis.environment.id",
                                "jdbc");
                        connectionProps.setProperty("JDBC.username", "sa");
                        connectionProps.setProperty("JDBC.password", "");
                        connectionProps.setProperty("JDBC.schema", "schema1");
                        connectionProps.setProperty("JDBC.autoCommit", "false");

                        Names.bindProperties(binder(), connectionProps);

                        addMapperClass(Schema1Mapper.class);
                        bind(Schema1Service.class);

                    }
                });

                expose(Schema1Service.class);
            }
        }, new PrivateModule() {
            @Override
            protected void configure() {
                install(new MyBatisModule() {
                    @Override
                    protected void initialize() {
                        bindDataSourceProviderType(PooledDataSourceProvider.class);
                        bindTransactionFactoryType(JdbcTransactionFactory.class);

                        install(JdbcHelper.HSQLDB_IN_MEMORY_NAMED);

                        Properties connectionProps = new Properties();
                        connectionProps.setProperty("mybatis.environment.id",
                                "jdbc");
                        connectionProps.setProperty("JDBC.username", "sa");
                        connectionProps.setProperty("JDBC.password", "");
                        connectionProps.setProperty("JDBC.schema", "schema2");
                        connectionProps.setProperty("JDBC.autoCommit", "false");

                        Names.bindProperties(binder(), connectionProps);

                        addMapperClass(Schema2Mapper.class);
                        bind(Schema2Service.class);
                    }
                });

                expose(Schema2Service.class);
            }
        }, new AbstractModule() {
            @Override
            protected void configure() {
                bind(CombinedService.class);
            }
        });
    }
}
