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
public class PayServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public PayServlet() {
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
		String amount = request.getParameter("amount");
		String currency = request.getParameter("currency");
		String outChannel = request.getParameter("outChannel");
		String title = request.getParameter("title");
		String product = request.getParameter("product");
		String memo = request.getParameter("memo");
		String returnUrl = request.getParameter("returnUrl");
		String notifyUrl = request.getParameter("notifyUrl");
		String reqTime = request.getParameter("reqTime");
		String userId = request.getParameter("userId");

		String url = "http://127.0.0.1:8888/pay/order";
		
		JSONObject sendContextJson = new JSONObject();
		// 商户号
		sendContextJson.put("merchNo", merchNo);
		// 订单号
		sendContextJson.put("orderNo", orderNo);
		// 支付渠道
		sendContextJson.put("outChannel", outChannel);
		// 订单标题
		sendContextJson.put("title", title);
		// 产品名称
		sendContextJson.put("product", product);
		// 备注
		sendContextJson.put("memo", memo);
		// 支付金额 单位 元
		sendContextJson.put("amount", amount);
		// 币种
		sendContextJson.put("currency", currency);
		// 前端返回地址
		sendContextJson.put("returnUrl", returnUrl);
		// 后台通知地址
		sendContextJson.put("notifyUrl", notifyUrl);
		// 请求时间
		sendContextJson.put("reqTime", reqTime);
		// userId
		sendContextJson.put("userId", userId);
		
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
			response.sendRedirect(Config.errorPage);

			e.printStackTrace();
		}
		
		if (recvSource == null) {
			response.sendRedirect(Config.errorPage);
			return;
		}
		
		JSONObject recvSourceJson = JSONObject.parseObject(recvSource);
		if (recvSourceJson.getString("code_url") != null) {
			String code_url = recvSourceJson.getString("code_url");
			response.sendRedirect(code_url);
			return;
		}
		
		if (recvSourceJson.getString("qrcode_url") != null) {
			String qrcode_url = recvSourceJson.getString("qrcode_url");
			response.sendRedirect("payQrcode.html?outChannel=" +outChannel+ 
									"&qrcodeUrl=" +qrcode_url+ 
									"&orderNo=" +orderNo+ 
									"&merchNo=" +merchNo);
			return;
		}
		
		response.sendRedirect(Config.errorPage);
	}

}

