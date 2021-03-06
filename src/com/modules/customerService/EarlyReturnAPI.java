package com.modules.customerService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SaleSqlServerHelper;
import com.modules.utils.AjaxJson;

/**
 * 早返率数据统计控制层
 * @author Caroline
 */
@RestController
@RequestMapping(value = "/tf-api/customerService/EarlyReturn/data")
public class EarlyReturnAPI {
	/**
	 * 1、获取待查询条件，并输出最终的结果信息
	 * @param 
	 */
	@RequestMapping(value = "findPlatformList")
	@ResponseBody
	public AjaxJson ProductPlatform(HttpServletRequest request) {
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();	
		//System.out.println("555");
		//读取界面传过来的参数
		String productTyle=request.getParameter("productTyle");//"台式机";		
		System.out.println(productTyle);
		
		List<String> platformList=new ArrayList<String>();
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql="";
		sql="  select PRODUCT_PLATFORM from tbProduction where PRODUCT_TYPE ='"+productTyle +"' group by PRODUCT_PLATFORM ORDER BY PRODUCT_PLATFORM";
		//sql="  select PRODUCT_PLATFORM from tbProduction  group by PRODUCT_PLATFORM ORDER BY PRODUCT_PLATFORM";
		
		System.out.println(sql);
		List<Object> list1=sqlServerHelper.query(sql);
		if(list1.size()==1){
			//弹出提示信息
			ajax.setSuccess(false);
			ajax.setMessage("后台没有维护'产品平台'的产量！");
			return ajax;
		}
		
		if(list1.size()>1){
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String platform = String.valueOf(map.get("PRODUCT_PLATFORM")).trim();
				if(!platform.equals("-")) {
					platformList.add(platform);
				}
			}
		}
		System.out.println(platformList);
		
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("platformList", platformList);
		ajax.setBody(body);
		return ajax;
		}
	
	
	/**
	 * 2、获取待查询条件，并输出最终的结果信息
	 * @param 
	 */
	@RequestMapping(value = "findResultList")
	@ResponseBody
	public AjaxJson EarlyReturn(HttpServletRequest request) {
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();	
		List<String> DateList=new ArrayList<String>();
		List<Double> ResultList=new ArrayList<Double>();
		List<String> RemarksList=new ArrayList<String>();
		List<Object> list2=new ArrayList();
		
		double Index;
		
		//1.读取界面传过来的参数
		String inqueryDate=request.getParameter("inqueryDate"); //"201805";
		String productTyle=request.getParameter("productTyle");//"台式机";		
		String productPlatform=request.getParameter("productPlatform");//"S710";
		String diffMonth=request.getParameter("diffMonth");//"3";
		
		
		 try {
			//获得查询月的上一个月
			String lastMonth=getLastMonth(inqueryDate,0,-1,0);
			
			//2.验证参数的有效性，注意ProductPlatform
			if(inqueryDate.length()!=6) {
				//弹出提示信息
				ajax.setSuccess(false);
				ajax.setMessage("查询月份不正确！");
				return ajax;
			}

			//3.检验是否存在查询月份的产量数据，如果没有，则给出提示
			SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
			String sql="";
			if(productPlatform=="") {
				sql="select * from tbProduction where SUBSTRING(PRODUCT_DATE,1,4)='"+inqueryDate.substring(0,4)+"' and SUBSTRING(PRODUCT_DATE,6,2)='"+inqueryDate.substring(4)+"' and PRODUCT_TYPE='"+productTyle+"'and PRODUCT_PLATFORM='-'";
				
			}
			else {
				sql="select * from tbProduction where SUBSTRING(PRODUCT_DATE,1,4)='"+inqueryDate.substring(0,4)+"' and SUBSTRING(PRODUCT_DATE,6,2)='"+inqueryDate.substring(4)+"' and PRODUCT_TYPE='"+productTyle+"'and PRODUCT_PLATFORM='"+productPlatform+"'";
				
			}
			//sql="select * from tbProduction where SUBSTRING(PRODUCT_DATE,1,4)='"+inqueryDate.substring(0,4)+"' and SUBSTRING(PRODUCT_DATE,6,2)='"+inqueryDate.substring(4)+"' and PRODUCT_TYPE='"+productTyle+"'and PRODUCT_PLATFORM='"+productPlatform+"'";
			System.out.println(sql);
			List<Object> list01=sqlServerHelper.query(sql);
			if(list01.size()==0){
				//弹出提示信息
				ajax.setSuccess(false);
				ajax.setMessage("请先维护  "+inqueryDate+"  "+productTyle+" "+productPlatform+" 的月产量!");
				return ajax;
			}
			
			//4.检验退换数据是否维护
			sql="select top 100 * from tbReturnsBasicData where Datename(year,InformDate)+Datename(month,InformDate)='"+inqueryDate+"'";
			//System.out.println(sql);
			List<Object> list04=sqlServerHelper.query(sql);
			if(list04.size()==0){
				//弹出提示信息
				ajax.setSuccess(false);
				ajax.setMessage("请先维护"+inqueryDate+"的月退换数据!!!");
				return ajax;
			}
							
					
			//5.检验是否存在所查询类型的物料，如果没有，则给出提示
			sql="select * from tbTypeMateriel where PRODUCT_TYPE='"+productTyle+"'";
			//System.out.println(sql);
			List<Object> list02=sqlServerHelper.query(sql);
			if(list02.size()==0){
						//弹出提示信息
						ajax.setSuccess(false);
						ajax.setMessage("请先维护该产品类型的物料信息!");
						return ajax;
					}
			
			productPlatform=(productPlatform==""?"-":productPlatform);
			//6.检验是tbQualityIndex否存在目标值，如果没有，则给出提示
			sql="select QUALITY_INDEX from tbQualityIndex where KPI_TYPE='早返' and PRODUCT_TYPE='"+productTyle+"' and PRODUCT_PLATFORM='"+productPlatform+"'";
			List<Object> list03=sqlServerHelper.query(sql);
			if(list03.size()==0){
				//弹出提示信息
				ajax.setSuccess(false);
				ajax.setMessage("请维护该产品类型对应的指标!");
				return ajax;
			}
			else {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list03.get(0);
				Index = Double.parseDouble(String.valueOf(map.get("QUALITY_INDEX")));
				System.out.println(Index);
			}
			
			//7..根据查询条件，在tbEarlyReturn查看是否已经计算过该数据，如果已经有，则直接读取，如果没有，则执行存储过程生成结果
			int size= IsExist( diffMonth, inqueryDate, productTyle, productPlatform) ;
			
			//如果该月份的早返数据不存在，
			//判断上一个月的早返数据是否存在，如果不存在，则提示先查询上个月的早返数据;如果存在，则计算查询月早返数据
			if(size==0) {
				//判断上一个月的早返数据是否存在，如果不存在，则提示先查询上个月的早返数据
				if(IsExist( diffMonth, lastMonth, productTyle, productPlatform)==0) {
					//弹出提示信息
					ajax.setSuccess(false);
					ajax.setMessage("缺少上个月的早返记录，请先查询上个月记录!");
					return ajax;
				}
				//如果存在，则计算查询月早返数据
				if(CalEarlyReturn(diffMonth,productTyle,inqueryDate,productPlatform)) {
					//查询所有符合条件的一次开箱合格率
					list2=InqueryEarlyReturn(diffMonth,productTyle,inqueryDate,productPlatform);
				}  
			}
			//如果该月份的早返数据已经存在，则直接查询
			else {
				System.out.println("查询所有符合条件的早返数据");
				//查询所有符合条件的早返率
				list2=InqueryEarlyReturn(diffMonth,productTyle,inqueryDate,productPlatform);
			
			}
			
			for(int i=0;i<list2.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list2.get(i);
				String Date = String.valueOf(map.get("查询月份"));
				String Result = String.valueOf(map.get("早返率"));
				String Remarks = String.valueOf(map.get("备注"));
				
				DateList.add(Date);
				ResultList.add(Double.parseDouble(Result));
				RemarksList.add(Remarks);
				//System.out.println(Date+":"+Result+":"+RemarksList);
			}
			System.out.println(DateList);
			System.out.println(ResultList);
			System.out.println(RemarksList);

			LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
			body.put("DateList", DateList);
			body.put("ResultList", ResultList);
			body.put("RemarksList", RemarksList);
			body.put("Index", Index);
			ajax.setBody(body);
			return ajax;
			
		 } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
				
		 }
		return ajax;  
	}
	
	public static int IsExist(String diffMonth,String inqueryDate,String productTyle,String productPlatform) {
		String sql="";
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		if(productPlatform=="") {
			sql=" select *  FROM tbEarlyReturn "
					   +"  where 月份差='"+diffMonth+"' and 查询月份='"+inqueryDate+"' and 产品类型='"+productTyle+"'";
		}
		else {
			sql=" select *  FROM tbEarlyReturn "
				+"  where 月份差='"+diffMonth+"' and 查询月份='"+inqueryDate+"' and 产品类型='"+productTyle+"' "
				+"        and 产品平台='"+productPlatform+"' ";
		}
		List<Object> list1=sqlServerHelper.query(sql);
		return list1.size();
	}
	/** 
	* 获取指定月的前一月(年)或后一月(年) 
	* @param dateStr 
	* @param addYear 
	* @param addMonth 
	* @param addDate 
	* @return 输入的时期格式为yyyy-MM,输出的日期格式为yyyy-MM 
	* @throws Exception 
	*/ 
	public static String getLastMonth(String dateStr,int addYear, int addMonth, int addDate) throws Exception { 
		try { 
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMM"); 
			java.util.Date sourceDate = sdf.parse(dateStr); 
			Calendar cal = Calendar.getInstance(); 
			cal.setTime(sourceDate); 
			cal.add(Calendar.YEAR,addYear); 
			cal.add(Calendar.MONTH, addMonth); 
			cal.add(Calendar.DATE, addDate); 
			java.text.SimpleDateFormat returnSdf = new java.text.SimpleDateFormat("yyyyMM"); 
			String dateTmp = returnSdf.format(cal.getTime()); 
			java.util.Date returnDate = returnSdf.parse(dateTmp); 
			return dateTmp; 
		} catch (Exception e) { 
			e.printStackTrace(); 
			throw new Exception(e.getMessage()); 
		} 
	} 
	
	//执行存储过程，得出一次开箱合格率的值
	public static Boolean CalEarlyReturn(String diffMonth,String productTyle,String inqueryDate,String ProductPlatform) {
		String materialIDList="";		

		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		
		String sql="";
		try {
			//1..根据productTyle，连接数据库了，在tbTypeMateriel表中获取对应的料号
			
					sql=" select MATERIEL_ID  FROM tbTypeMateriel WHERE PRODUCT_TYPE='"+productTyle+"' ";
					//System.out.println(sql);
					List<Object> list1=sqlServerHelper.query(sql);
					for (int i=0;i<list1.size();i++) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) list1.get(i);
						String reporter = String.valueOf(map.get("MATERIEL_ID"));
						if(i==list1.size()-1) {
							//materialIDList+="''"+reporter+"'' '";
							materialIDList+=reporter;
							break;
						}
						materialIDList+=reporter+",";
					}
					//System.out.println(materialIDList);
					
					//2..在tbCondition表中获取对应筛选条件
					String factoryNumber="";
					String returnReason="'";
					sql=" select 无锡工厂的制令单号前两位,退换理由  FROM tbCondition WHERE KPI类别='早返' ";
					System.out.println(sql);
					List<Object> list2=sqlServerHelper.query(sql);
					if(list2.size()==1) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) list2.get(0);
						String num = String.valueOf(map.get("无锡工厂的制令单号前两位"));
						String[] temp1=num.split("\\|");
						for (int i = 0 ; i <temp1.length ; i++ ) {
							if(i ==temp1.length-1) {
								factoryNumber += temp1[i];
							}
							else {
								factoryNumber += temp1[i]+",";
							}
						    
						} 
						System.out.println(factoryNumber);
						String reason = String.valueOf(map.get("退换理由"));
						String[] temp2=reason.split("\\|");
						for (int i = 0 ; i <temp2.length ; i++ ) {
							returnReason +="and ProblemDescription not LIKE ''%"+temp2[i]+"%'' ";
						} 
						returnReason+="'";
						//System.out.println(returnReason);
					}

					//3.连接数据库,执行存储过程 得到一次开箱合格率的值		
					ProductPlatform=ProductPlatform=="-"?"":ProductPlatform;
					sql="DECLARE	@return_value int,\r\n" + 
							"		@Result float\r\n" + 
							"\r\n" + 
							"EXEC	 [dbo].[EarlyReturn]\r\n" + 
							"       @diffMonth ="+ diffMonth +","+
							"		@productTyle = "+productTyle+"," + 
							"		@materialIDList = \'"+materialIDList+"\'," +
							"		@inqueryDate = "+inqueryDate+", " + 
							"		@ProductPlatform = \'"+ProductPlatform+"\', " +  
							"		@factoryNumber = \'"+factoryNumber+"\'," +
							"       @returnReason = "+returnReason+"," +  
							"		@Result = @Result OUTPUT\r\n "
							+ "     SELECT	@Result as N'@Result'";
					
					List<Object> list=sqlServerHelper.query(sql);
					System.out.println(sql);
					//System.out.println(list);
					return true;

					
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}
	
	}
	
	     //查询已经存储的的早返数据
		public static List<Object> InqueryEarlyReturn(String diffMonth, String productTyle,String inqueryDate,String ProductPlatform) {
			SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
			String sql="";

			//查询出已经存储的： 月份+一早返率
			ProductPlatform=ProductPlatform==""?"-":ProductPlatform;
			sql="select * " + 
					"from (  select  top  6  查询月份,早返率,备注  " + 
					"         FROM tbEarlyReturn  " + 
					"         where   月份差='"+ diffMonth +"' and 产品类型='"+productTyle+"'  and 产品平台='"+ProductPlatform+"'  and 查询月份<='"+inqueryDate+"'" + 
					"         order by 查询月份 desc" + 
					"      ) a " + 
					"order by 查询月份 ";
			List<Object> list=sqlServerHelper.query(sql);
			System.out.println(sql);
			System.out.println(list);
			return list;
			
			
		}
		
	
}
