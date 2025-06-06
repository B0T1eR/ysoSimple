package cn.butler.jndi.controller.database;

import cn.butler.jndi.annotation.JNDIController;
import cn.butler.jndi.annotation.JNDIMapping;
import cn.butler.jndi.controller.DatabaseController;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.Properties;

@JNDIController
@JNDIMapping("/Druid")
public class DruidController extends DatabaseController {
    public Object process(Properties props) {
        Reference ref = new Reference("javax.sql.DataSource", "com.alibaba.druid.pool.DruidDataSourceFactory", null);
        ref.add(new StringRefAddr("driverClassName", props.getProperty("driver")));
        ref.add(new StringRefAddr("url", props.getProperty("url")));
        ref.add(new StringRefAddr("initialSize", "1"));
        ref.add(new StringRefAddr("init", "true"));

        if (props.getProperty("sql") != null) {
            ref.add(new StringRefAddr("initConnectionSqls", props.getProperty("sql")));
        }

        return ref;
    }
}
