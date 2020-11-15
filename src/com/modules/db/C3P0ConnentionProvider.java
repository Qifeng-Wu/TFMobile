package com.modules.db;

import java.io.FileInputStream;  
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;  
  
import javax.sql.DataSource;  
  
import com.mchange.v2.c3p0.DataSources;  
  
/** 
 * c3p0连接池管理类 
 */  
public class C3P0ConnentionProvider {  
  
    private static final String JDBC_DRIVER = "driverClass";  
    private static final String JDBC_URL = "jdbcUrl";   
  
    private static DataSource ds;  
    private static DataSource productionSqlServerDs;  
    private static DataSource qualitySqlServerDs;  
    private static DataSource saleSqlServerDs;  
    /** 
     * 初始化连接池代码块 
     */  
    static {  
        initDBSource();  
        initProductionSqlServerDBSource();  
        initQualitySqlServerDBSource();  
        initSaleSqlServerDBSource();  
    }  
  
    /** 
     * 1.初始化c3p0的MySql连接池 
     */  
    private static final void initDBSource() {  
        Properties c3p0Pro = new Properties();  
        try {  
            // 加载配置文件  
            String path = C3P0ConnentionProvider.class.getResource("/").getPath(); 
            String websiteURL = ("/" + path  + "c3p0.properties").replaceFirst("/", "");
            FileInputStream in = new FileInputStream(websiteURL);  
            c3p0Pro.load(in);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
  
        String drverClass = c3p0Pro.getProperty(JDBC_DRIVER);  
        if (drverClass != null) {  
            try {  
                // 加载驱动类  
                Class.forName(drverClass);  
            } catch (ClassNotFoundException e) {  
                e.printStackTrace();  
            }  
  
        }  
  
        Properties jdbcpropes = new Properties();  
        Properties c3propes = new Properties();  
        for (Object key : c3p0Pro.keySet()) {  
            String skey = (String) key;  
            if (skey.startsWith("c3p0.")) {  
                c3propes.put(skey, c3p0Pro.getProperty(skey).trim());  
            } else {  
                jdbcpropes.put(skey, c3p0Pro.getProperty(skey).trim());  
            }  
        }  
  
        try {  
            // 建立连接池  
            DataSource unPooled = DataSources.unpooledDataSource(c3p0Pro.getProperty(JDBC_URL), jdbcpropes);  
            ds = DataSources.pooledDataSource(unPooled, c3propes);  
  
        } catch (SQLException e) {  
            e.printStackTrace();  
        }  
    } 
    
    /** 
     * 2.初始化c3p0的SqlServer连接池 （生产数据库）
     */  
    private static final void initProductionSqlServerDBSource() {  
    	Properties c3p0Pro = new Properties();  
    	try {  
    		// 加载配置文件  
    		String path = C3P0ConnentionProvider.class.getResource("/").getPath(); 
    		String websiteURL = ("/" + path  + "c3p0-production-sqlserver.properties").replaceFirst("/", "");
    		FileInputStream in = new FileInputStream(websiteURL);  
    		c3p0Pro.load(in);  
    	} catch (Exception e) {  
    		e.printStackTrace();  
    	}  
    	
    	String drverClass = c3p0Pro.getProperty(JDBC_DRIVER);  
    	if (drverClass != null) {  
    		try {  
    			// 加载驱动类  
    			Class.forName(drverClass);  
    		} catch (ClassNotFoundException e) {  
    			e.printStackTrace();  
    		}  
    		
    	}  
    	
    	Properties jdbcpropes = new Properties();  
    	Properties c3propes = new Properties();  
    	for (Object key : c3p0Pro.keySet()) {  
    		String skey = (String) key;  
    		if (skey.startsWith("c3p0.")) {  
    			c3propes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		} else {  
    			jdbcpropes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		}  
    	}  
    	
    	try {  
    		// 建立连接池  
    		DataSource unPooled = DataSources.unpooledDataSource(c3p0Pro.getProperty(JDBC_URL), jdbcpropes);  
    		productionSqlServerDs = DataSources.pooledDataSource(unPooled, c3propes);  
    		
    	} catch (SQLException e) {  
    		e.printStackTrace();  
    	}  
    }  
    /** 
     * 3.初始化c3p0的SqlServer连接池 （质量数据库）
     */  
    private static final void initQualitySqlServerDBSource() {  
    	Properties c3p0Pro = new Properties();  
    	try {  
    		// 加载配置文件  
    		String path = C3P0ConnentionProvider.class.getResource("/").getPath(); 
    		String websiteURL = ("/" + path  + "c3p0-quality-sqlserver.properties").replaceFirst("/", "");
    		FileInputStream in = new FileInputStream(websiteURL);  
    		c3p0Pro.load(in);  
    	} catch (Exception e) {  
    		e.printStackTrace();  
    	}  
    	
    	String drverClass = c3p0Pro.getProperty(JDBC_DRIVER);  
    	if (drverClass != null) {  
    		try {  
    			// 加载驱动类  
    			Class.forName(drverClass);  
    		} catch (ClassNotFoundException e) {  
    			e.printStackTrace();  
    		}  
    		
    	}  
    	
    	Properties jdbcpropes = new Properties();  
    	Properties c3propes = new Properties();  
    	for (Object key : c3p0Pro.keySet()) {  
    		String skey = (String) key;  
    		if (skey.startsWith("c3p0.")) {  
    			c3propes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		} else {  
    			jdbcpropes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		}  
    	}  
    	
    	try {  
    		// 建立连接池  
    		DataSource unPooled = DataSources.unpooledDataSource(c3p0Pro.getProperty(JDBC_URL), jdbcpropes);  
    		qualitySqlServerDs = DataSources.pooledDataSource(unPooled, c3propes);  
    		
    	} catch (SQLException e) {  
    		e.printStackTrace();  
    	}  
    }  
    /** 
     * 4.初始化c3p0的SqlServer连接池 （售后服务数据库）
     */  
    private static final void initSaleSqlServerDBSource() {  
    	Properties c3p0Pro = new Properties();  
    	try {  
    		// 加载配置文件  
    		String path = C3P0ConnentionProvider.class.getResource("/").getPath(); 
    		String websiteURL = ("/" + path  + "c3p0-sale-sqlserver.properties").replaceFirst("/", "");
    		FileInputStream in = new FileInputStream(websiteURL);  
    		c3p0Pro.load(in);  
    	} catch (Exception e) {  
    		e.printStackTrace();  
    	}  
    	
    	String drverClass = c3p0Pro.getProperty(JDBC_DRIVER);  
    	if (drverClass != null) {  
    		try {  
    			// 加载驱动类  
    			Class.forName(drverClass);  
    		} catch (ClassNotFoundException e) {  
    			e.printStackTrace();  
    		}  
    		
    	}  
    	
    	Properties jdbcpropes = new Properties();  
    	Properties c3propes = new Properties();  
    	for (Object key : c3p0Pro.keySet()) {  
    		String skey = (String) key;  
    		if (skey.startsWith("c3p0.")) {  
    			c3propes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		} else {  
    			jdbcpropes.put(skey, c3p0Pro.getProperty(skey).trim());  
    		}  
    	}  
    	
    	try {  
    		// 建立连接池  
    		DataSource unPooled = DataSources.unpooledDataSource(c3p0Pro.getProperty(JDBC_URL), jdbcpropes);  
    		saleSqlServerDs = DataSources.pooledDataSource(unPooled, c3propes);  
    		
    	} catch (SQLException e) {  
    		e.printStackTrace();  
    	}  
    }  
  
    /** 
     * 1.MySql获取数据库连接对象 
     *  
     * @return 数据连接对象 
     * @throws SQLException 
     */  
    public static synchronized Connection getConnection() throws SQLException {  
        final Connection conn = ds.getConnection();  
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  
        return conn;  
    }
    /** 
     * 2.SqlServer获取数据库连接对象 （生产数据库）
     *  
     * @return 数据连接对象 
     * @throws SQLException 
     */  
    public static synchronized Connection getProductionSqlServerConnection() throws SQLException {  
    	final Connection conn = productionSqlServerDs.getConnection();  
    	conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  
    	return conn;  
    }
    /** 
     * 3.SqlServer获取数据库连接对象 （质量数据库）
     *  
     * @return 数据连接对象 
     * @throws SQLException 
     */  
    public static synchronized Connection getQualitySqlServerConnection() throws SQLException {  
    	final Connection conn = qualitySqlServerDs.getConnection();  
    	conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  
    	return conn;  
    }
    /** 
     * 4.SqlServer获取数据库连接对象 （售后服务数据库）
     *  
     * @return 数据连接对象 
     * @throws SQLException 
     */  
    public static synchronized Connection getSaleSqlServerConnection() throws SQLException {  
    	final Connection conn = saleSqlServerDs.getConnection();  
    	conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);  
    	return conn;  
    }
}  