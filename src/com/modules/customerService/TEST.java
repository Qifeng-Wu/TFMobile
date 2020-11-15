package com.modules.customerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.TreeMap;




import net.sf.json.JSONArray;


public class TEST {

	
	public static void main(String[] args) {
		String json = "111";
		Map<Object, Object> param = new TreeMap<Object, Object>();
		//param.put("json", json);
		//param.put("jsons", json);
		String jsonString = JSONArray.fromObject(param).toString();
		//jsonString = jsonString.substring(1, jsonString.length() - 1);
		//CommUtil.doPost("http://10.100.12.50:8080/BJService/tf-api/bj-mobile/test/findList",param);
		String p = "http://10.100.12.50:8080/BJService/api/test/findList";
		String minaOk = dopost(p,jsonString);
		
	}
	
	public static String dopost(String urlStr, String strInfo) {
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
	
}
