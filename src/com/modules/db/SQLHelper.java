package com.modules.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SQLHelper {
	private Connection conn = null;
	private PreparedStatement ps = null;
	private ResultSet rs = null;
	private Statement stmt = null;

	/**
	 * 数据查询
	 * 
	 * @param sql语句
	 * @return 返回结果集List<Object>
	 */
	public List<Object> query(String sql) {
		if (sql.equals("") || sql == null) {
			return null;
		}
		List<Object> list = new ArrayList<Object>();
		try {
			conn = C3P0ConnentionProvider.getConnection();
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			// 可以得到有多少列
			int columnNum = rsmd.getColumnCount();
			// 将数据封装到list中
			while (rs.next()) {
				HashMap<String, Object> objects = new HashMap<String, Object>();
				for (int i = 0; i < columnNum; i++) {
					objects.put(rsmd.getColumnName(i + 1), rs.getObject(i + 1));
				}
				list.add(objects);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (ps != null) {
					ps.close();
					ps = null;
				}
				if (conn != null) {
					conn.close();
					conn = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/**
	 * 插入、修改数据操作
	 * 
	 * @param sql语句
	 * @return boolean 成功返回true，失败返回false
	 */
	public boolean update(String sql) {
		boolean b = false;
		if (sql.equals("") || sql == null) {
			return b;
		}
		try {
			conn = C3P0ConnentionProvider.getConnection();
			ps = conn.prepareStatement(sql);
			int i = ps.executeUpdate();
			if (i == 1) {
				b = true;
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			try {
				if (ps != null) {
					ps.close();
					ps = null;
				}
				if (conn != null) {
					conn.close();
					conn = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return b;
	}
	
	/**
	 * 批量插入， 修改 的事务处理
	 * @param sqlist
	 * @return
	 */
	public boolean update(List<String> sqlist) {
		boolean b = false;
		if (sqlist.isEmpty() || sqlist == null) {
			return b;
		}
		try {
			conn = C3P0ConnentionProvider.getConnection();
			stmt = conn.createStatement();
			// JAVA默认为TRUE,我们自己处理需要设置为FALSE,并且修改为手动提交,才可以调用rollback()函数
			conn.setAutoCommit(false);
			for (int i = 0; i < sqlist.size(); i++) {
				stmt.addBatch(sqlist.get(i));
			}
			stmt.executeBatch();
			// 事务提交
			conn.commit();
			// 设置为自动提交,改为TRUE
			conn.setAutoCommit(true);
			b=true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			b=false;
			try {
				// 产生的任何SQL异常都需要进行回滚,并设置为系统默认的提交方式,即为TRUE
				if (conn != null) {
					conn.rollback();
					conn.setAutoCommit(true);
				}
			} catch (SQLException se) {
				se.printStackTrace();
			}
		} finally {
			try {
				if (ps != null) {
					ps.close();
					ps = null;
				}
				if (conn != null) {
					conn.close();
					conn = null;
				}
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return b;
	}

}
