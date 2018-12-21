package action;

import java.io.IOException;

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
public class QueryServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public QueryServlet() {
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
		// TODO Auto-generated method stub
		request.setCharacterEncoding("UTF-8");
		String merchNo = request.getParameter("merchNo");
		String orderNo = request.getParameter("orderNo");
		String url = "http://127.0.0.1:8888/pay/order/query";
		
		JSONObject sendContextJson = new JSONObject();
		// 商户号
		sendContextJson.put("merchNo", merchNo);
		// 订单号
		sendContextJson.put("orderNo", orderNo);
		
		byte[] sendContext;
		String sendSign;
		try {
			sendContext = RSAUtil.encryptByPublicKey(JSON.toJSONBytes(sendContextJson), Config.platformPublicKey);
			sendSign = RSAUtil.sign(sendContext, Config.merchantPrivateKey);
		} catch (Exception e) {
			response.sendRedirect(Config.errorPage);
			return;
		}

		JSONObject requestParamsJson = new JSONObject();
		requestParamsJson.put("sign", sendSign);
		requestParamsJson.put("context", sendContext);
		requestParamsJson.put("encryptType", "RSA");

		String result;
		try {
			result = RequestUtils.doPostJson(url, requestParamsJson.toJSONString(), "utf-8");
		} catch (Exception e) {
			response.sendRedirect(Config.errorPage);
			return;
		}

		JSONObject resultJson = JSONObject.parseObject(result);
		if (!"0".equals(resultJson.getString("code"))) {
			response.sendRedirect(Config.errorPage);
			return;
		}
		

		String recvSign = resultJson.getString("sign");
		byte[] recvContext = resultJson.getBytes("context");
		try {
			if (!RSAUtil.verify(recvContext, Config.platformPublicKey, recvSign)) {
				response.sendRedirect(Config.errorPage);

				return;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String recvSource = null;
		try {
			recvSource = new String(RSAUtil.decryptByPrivateKey(recvContext, Config.merchantPrivateKey));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (recvSource == null) {
			response.sendRedirect(Config.errorPage);
			return;
		}
		
		JSONObject recvSourceJson = JSONObject.parseObject(recvSource);
		if (recvSourceJson.getString("orderState") != null) {
			String orderState = recvSourceJson.getString("orderState");
			if (orderState.equals(Config.orderStateSucc)) {
				response.sendRedirect("paySucc.html");
			} else {
				response.sendRedirect("payError.html");
			}
			return;
		}
		
		response.sendRedirect(Config.errorPage);
	}

}

