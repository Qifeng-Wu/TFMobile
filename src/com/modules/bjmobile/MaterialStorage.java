package com.modules.bjmobile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.utils.AjaxJson;
import com.modules.utils.SAPConnection;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;


/**
 * 材料入库
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/materialStorage")
public class MaterialStorage {

	/**
	 * 1、获取所有特殊操作记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
        JCoFunction function = null;  
        List<String> list = new ArrayList<String>();
        //连接sap，其实就类似于连接数据库  
        JCoDestination destination = SAPConnection.connect(); 
        System.out.println("调用返回状态---===222======>"+destination);  
        try {  
            //调用ZMES_IF_CLRK函数  
            function = destination.getRepository().getFunction("ZMES_IF_CLRK");  
            System.out.println("调用返回状态---===333======>"+function);  
            //将当前传入的值赋予各个参数  
            function.getImportParameterList().setValue("I_EBELNK", searchName);  
            function.getImportParameterList().setValue("I_EBELNJ", searchName);        
            function.execute(destination);  
            //从表中获取数据
            JCoTable tableParameter = function.getTableParameterList().getTable("TD_ZMESCLRK");
            System.out.println("stephen==="+tableParameter.getNumRows());          
            if(tableParameter.getNumRows()>0) {
	            for (int i = 0; i < tableParameter.getNumRows(); i++) {
	            	tableParameter.setRow(i);  
	                String EBELN = tableParameter.getString("EBELN");
	                String EBELP = tableParameter.getString("EBELP");
	                String MENGE = tableParameter.getString("MENGE");
	                String MENGES = tableParameter.getString("MENGES");
	                String LOSMENGE = tableParameter.getString("LOSMENGE");
	                String LMENGE01 = tableParameter.getString("LMENGE01");
		            list.add(EBELN);
		            list.add(EBELP);
		            list.add(MENGE);
		            list.add(MENGES);
		            list.add(LOSMENGE);
		            list.add(LMENGE01);
	            }
            }else {
            	ajax.setMessage("没有查询到任何结果！");
            }
        } catch (JCoException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }   
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}

}
