package action;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import action.utils.RSAUtil;
import action.utils.RequestUtils;

/**
 * Servlet implementation class PayServlet
 */
public class NotifyServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public NotifyServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		JSONObject jsonObject =  RequestUtils.getJsonResultStream(request);
    	if(jsonObject == null){
    		return;
    	}
    	
    	String sign = jsonObject.getString("sign");
    	if(RequestUtils.isEmpty(sign)){
    		return;
    	}
    	
    	byte[] context = jsonObject.getBytes("context");
    	if(RequestUtils.isEmpty(context)){
    		return;
		}

    	try {
    		String source = new String(RSAUtil.decryptByPrivateKey(context, Config.merchantPrivateKey));

    		JSONObject jo = JSON.parseObject(source);
    		if (RSAUtil.verify(context, Config.platformPublicKey, sign)) {
    			boolean paySuccess = String.valueOf(Config.orderStateSucc).equals(jo.getString("orderState"));
//    			String orderAmt = jo.getString("amount");
//    			String businessNo = jo.getString("businessNo");
//    			String payCode = jo.getString("orderNo");
    			if (paySuccess) {
					System.out.println("do somethinig pay success!");
				}
    			PrintWriter pw = response.getWriter();
    			pw.write("ok");
    			pw.flush();
    			pw.close();
    		}
		} catch (Exception e) {
			// TODO: handle exception
		}
	
	}

}

