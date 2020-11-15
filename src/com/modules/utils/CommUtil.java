package com.modules.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;
import com.modules.db.C3P0ConnentionProvider;

import net.sf.json.JSONObject;
import sun.misc.BASE64Decoder;

public class CommUtil {

	/**
	 * 向指定URL发送POST请求
	 * 
	 * @param url
	 * @param paramMap
	 * @return 响应结果
	 */
	public static String sendPost(String url, Map<String, String> paramMap) {  
        PrintWriter out = null;  
        BufferedReader in = null;  
        String result = "";  
        try {  
            URL realUrl = new URL(url);  
            URLConnection conn = realUrl.openConnection();  
            conn.setRequestProperty("accept", "*/*");  
            conn.setRequestProperty("connection", "Keep-Alive");  
            conn.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");  
            conn.setRequestProperty("Charset", "UTF-8");  
            conn.setDoOutput(true);  
            conn.setDoInput(true);  
            out = new PrintWriter(conn.getOutputStream());  
  
            String param = "";  
            if (paramMap != null && paramMap.size() > 0) {  
                Iterator<String> ite = paramMap.keySet().iterator();  
                while (ite.hasNext()) {  
                    String key = ite.next();  
                    String value = paramMap.get(key);  
                    param += key + "=" + value + "&";  
                }  
                param = param.substring(0, param.length() - 1);  
            }  
  
            out.print(param);  
            out.flush();  
            in = new BufferedReader(  
                    new InputStreamReader(conn.getInputStream()));  
            String line;  
            while ((line = in.readLine()) != null) {  
                result += line;  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        finally {  
            try {  
                if (out != null) {  
                    out.close();  
                }  
                if (in != null) {  
                    in.close();  
                }  
            } catch (IOException ex) {  
                ex.printStackTrace();  
            }  
        }  
        return result;  
    }  

	/**
	 * 数据流post请求
	 * 
	 * @param urlStr
	 * @param xmlInfo
	 */
	public static String doPost(String urlStr, String strInfo) {
		String reStr = "";
		try {
			URL url = new URL(urlStr);
			URLConnection con = url.openConnection();
			con.setDoOutput(true);
			con.setRequestProperty("Pragma:", "no-cache");
			con.setRequestProperty("Cache-Control", "no-cache");
			con.setRequestProperty("Content-Type", "text/xml");
			OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
			out.write(new String(strInfo.getBytes("utf-8")));
			out.flush();
			out.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
			String line = "";
			for (line = br.readLine(); line != null; line = br.readLine()) {
				reStr += line;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return reStr;
	} 

	public static String getURL(HttpServletRequest request) {
		String contextPath = request.getContextPath().equals("/") ? "" : request.getContextPath();
		String url = "http://" + request.getServerName();
		if (null2Int(Integer.valueOf(request.getServerPort())) != 80) {
			url = url + ":" + null2Int(Integer.valueOf(request.getServerPort())) + contextPath;
		} else {
			url = url + contextPath;
		}
		return url;
	}

	/**
	 * 将null值转化为0
	 * 
	 * @param s
	 * @return
	 */
	public static int null2Int(Object s) {
		int v = 0;
		if (s != null)
			try {
				v = Integer.parseInt(s.toString());
			} catch (Exception localException) {
			}
		return v;
	}

	/**
	 * 读取流返回json
	 * 
	 * @param is
	 * @return
	 */
	public static JSONObject loadString(InputStream is) {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				stringBuilder.append(inputLine);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject paramJson = JSONObject.fromObject(stringBuilder.toString());
		return paramJson;
	}
	
	/**
	 * 去除字符串中的所有特殊字符和空格
	 * @param str
	 * @return
	 * @throws PatternSyntaxException
	 */
	public static String removeSpecialchar(String str) throws PatternSyntaxException {
		if(str != null){
			String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\]<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(str);
			return m.replaceAll("").trim().replace(" ", "");
		}
		return null;
	}
	/**
	 * 解决中文乱码
	 * @param str
	 * @return
	 * @throws PatternSyntaxException
	 */
	public static String chineseGarbledCode(String str) throws PatternSyntaxException {
		if(str != null){
			//解决中文
			try {
				str = new String(str.getBytes("ISO-8859-1"),"UTF-8");				
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return str;
	}

	/**
	 * 插入并获取刚插入的数据的自增主键Id
	 * 
	 * @param insertSql
	 * @return
	 */
	@SuppressWarnings("resource")
	public static Object insertAndBackId(String insertSql) {
		Object id = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = C3P0ConnentionProvider.getConnection();
			ps = conn.prepareStatement(insertSql);
			int j = ps.executeUpdate();
			if (j == 1) {
				ps = conn.prepareStatement("SELECT @@IDENTITY");
				rs = ps.executeQuery();
				ResultSetMetaData rsmd = rs.getMetaData();
				// 可以得到有多少列
				int columnNum = rsmd.getColumnCount();
				// 将数据封装到list中
				while (rs.next()) {
					for (int i = 0; i < columnNum; i++) {
						id = rs.getObject(i + 1);
					}
				}
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
		return id;
	}
	
	/**
	 *上传图片
	 * @param path(图片存储服务器) folderName(图片存储文件夹名称) fileName(图片名称)  pictureData(图片流)
	 * @return
	 */
	public static String uploadPicture(String path,String folderName,String fileName,String pictureData) {
		// 图片存储相对路径
		String networkPath = "";		
		try {
			String imgStr = pictureData.replace("data:image/jpeg;base64,", "");
			BASE64Decoder decoder = new BASE64Decoder();
			byte[] bytes = decoder.decodeBuffer(imgStr);
			
			Calendar calendar = Calendar.getInstance();
	    	String year = String.valueOf(calendar.get(Calendar.YEAR));
	    	String months = String.valueOf(calendar.get(Calendar.MONTH) + 1);            
	    	if(months.length()<2) months = 0 + months;//将月份变为两位数
	    	String day = String.valueOf(calendar.get(Calendar.DATE));
	    	if(day.length()<2) day = 0 + day;//将日期变为两位数
	    	
	    	String pakageName = year + "/" + months + "/" + day;	
			String picturePath = path +"/"+ folderName +"/"+ pakageName;			
			File pakageFile = new File(picturePath);
			if (!pakageFile.exists() && !pakageFile.isDirectory()) {
				pakageFile.mkdirs();// 创建文件夹
			}			
			String name = fileName + ".jpg";
			File file = new File(picturePath, name);			
			// 将文件存到当前controller所在服务器			
			FileOutputStream os = new FileOutputStream(file);
			InputStream iStream = new ByteArrayInputStream(bytes);
			byte[] b = new byte[1024];
			int byteRead = 0;
			while ((byteRead = iStream.read(b)) != -1) {
				os.write(b, 0, byteRead);
			}
			os.flush();
			os.close();
			
			networkPath = folderName + "/" + pakageName + "/" + name;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return networkPath;
	}
}
