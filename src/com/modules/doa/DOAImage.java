package com.modules.doa;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;
import com.modules.utils.CommUtil;
import com.modules.utils.ConfigurationFileHelper;


@RestController
@RequestMapping(value = "tf-api/doa/image")
public class DOAImage {
	private static String savePicturePath = ConfigurationFileHelper.getSavePicturePath();
	/**
	 * DOA图片上传
	 * @param imgData
	 * @param doaSn
	 * @return boolean
	 */
	@RequestMapping(value = "upload")
	@ResponseBody
	public boolean addPhoto(String doaSn,String image,String description) throws IOException {
		SQLHelper sh = new SQLHelper();
		//上传图片
		if(image!=null&&!image.isEmpty()) {
			image = CommUtil.uploadPicture(savePicturePath,"DOAImage",doaSn+"_"+System.currentTimeMillis(),image);
		}
		String sql = "INSERT INTO tf_doa_image (doa_sn,image,description,create_time) VALUES ('"+doaSn+"','"+image+"','"+description+"',now())";
		boolean boo = sh.update(sql);
	   return boo;   
	}
	
	/**
	 * DOA图片获取显示
	 * @param page
	 * @return ajax
	 */
	@RequestMapping(value = "list")
	@ResponseBody
	public AjaxJson list(AjaxJson ajax,HttpServletRequest request) {
		Integer page = 	Integer.parseInt(request.getParameter("page"))*6;
		SQLHelper sh = new SQLHelper();
		String sql = "SELECT * FROM tf_doa_image ORDER BY id DESC LIMIT "+page+",6";
		List<Object> list = sh.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("imageList", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * DOA图片退换货单号查询
	 * @param searchName
	 */
	@RequestMapping(value = "search")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sh = new SQLHelper();
		String sql = "select * from tf_doa_image where doa_sn like '%"+searchName+"%' order by id desc";
		List<Object> list = sh.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("imageList", list);
		ajax.setBody(body);
		return ajax;
	}	
	/**
	 * DOA图片时间段查询
	 * @param searchName
	 */
	@RequestMapping(value = "dateSearch")
	@ResponseBody
	public AjaxJson dateSearch(HttpServletRequest request){
		String startTime = request.getParameter("startTime");
		String endTime = request.getParameter("endTime");
		AjaxJson ajax = new AjaxJson();			
		String sql = "select * from tf_doa_image where create_time between '"+startTime+"' and DATE_ADD('"+endTime+"',INTERVAL 1 DAY) order by create_time desc";
		SQLHelper sh = new SQLHelper();
		List<Object> list = sh.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("imageList", list);
		ajax.setBody(body);
		return ajax;
	}	
}
