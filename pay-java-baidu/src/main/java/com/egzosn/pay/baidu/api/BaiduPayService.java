package com.egzosn.pay.baidu.api;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.baidu.bean.BaiduBillType;
import com.egzosn.pay.baidu.bean.BaiduPayOrder;
import com.egzosn.pay.baidu.bean.BaiduTransactionType;
import com.egzosn.pay.baidu.bean.type.AuditStatus;
import com.egzosn.pay.baidu.util.Asserts;
import com.egzosn.pay.common.api.BasePayService;
import com.egzosn.pay.common.bean.BaseRefundResult;
import com.egzosn.pay.common.bean.BillType;
import com.egzosn.pay.common.bean.CurType;
import com.egzosn.pay.common.bean.MethodType;
import com.egzosn.pay.common.bean.PayMessage;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.http.UriVariables;
import com.egzosn.pay.common.util.DateUtils;
import com.egzosn.pay.common.util.Util;
import com.egzosn.pay.common.util.sign.SignUtils;
import com.egzosn.pay.common.util.str.StringUtils;


public class BaiduPayService extends BasePayService<BaiduPayConfigStorage> {
    public static final String APP_KEY = "appKey";
    public static final String APP_ID = "appId";
    public static final String DEAL_ID = "dealId";
    public static final String TP_ORDER_ID = "tpOrderId";
    public static final String DEAL_TITLE = "dealTitle";
    public static final String TOTAL_AMOUNT = "totalAmount";
    public static final String SIGN_FIELDS_RANGE = "signFieldsRange";
    public static final String BIZ_INFO = "bizInfo";
    public static final String RSA_SIGN = "rsaSign";
    public static final String ORDER_ID = "orderId";
    public static final String USER_ID = "userId";
    public static final String SITE_ID = "siteId";
    public static final String SIGN = "sign";
    public static final String METHOD = "method";
    public static final String TYPE = "type";

    public static final Integer RESPONSE_SUCCESS = 2;
    public static final String RESPONSE_STATUS = "status";


    public BaiduPayService(BaiduPayConfigStorage payConfigStorage) {
        super(payConfigStorage);
    }

    public BaiduPayService(BaiduPayConfigStorage payConfigStorage,
                           HttpConfigStorage configStorage) {
        super(payConfigStorage, configStorage);
    }

    /**
     * ????????????
     *
     * @param params ????????????????????????
     * @return ??????
     */
    @Override
    public boolean verify(Map<String, Object> params) {
        if (!RESPONSE_SUCCESS.equals(params.get(RESPONSE_STATUS))) {
            return false;
        }
        return signVerify(params, String.valueOf(params.get(RSA_SIGN)));
    }

    /**
     * ????????????
     *
     * @param params ?????????
     * @param sign   ????????????
     * @return ??????
     */
    public boolean signVerify(Map<String, Object> params, String sign) {
        String rsaSign = String.valueOf(params.get(RSA_SIGN));
        String targetRsaSign = getRsaSign(params, RSA_SIGN);
        LOG.debug("?????????????????????: " + rsaSign + " ?????????????????????: " + targetRsaSign);
        return StringUtils.equals(rsaSign, targetRsaSign);
    }


    /**
     * ???????????????????????????
     *
     * @param order ????????????
     * @return ??????
     */
    @Override
    public Map<String, Object> orderInfo(PayOrder order) {
        Map<String, Object> params = getUseOrderInfoParams(order);
        String rsaSign = getRsaSign(params, RSA_SIGN);
        params.put(RSA_SIGN, rsaSign);
        return params;
    }

    /**
     * ??????"??????????????????"????????????
     *
     * @return ??????
     */
    public Map<String, Object> getUseQueryPay() {
        String appKey = payConfigStorage.getAppKey();
        Map<String, Object> result = new HashMap<>();
        result.put(APP_KEY, appKey);
        result.put(APP_ID, payConfigStorage.getAppId());
        return result;
    }

    /**
     * ??????"????????????"????????????
     *
     * @param order ????????????
     * @return ??????
     */
    private Map<String, Object> getUseOrderInfoParams(PayOrder order) {
        BaiduPayOrder payOrder = (BaiduPayOrder) order;
        Map<String, Object> result = new HashMap<>();
        String appKey = payConfigStorage.getAppKey();
        String dealId = payConfigStorage.getDealId();
        result.put(APP_KEY, appKey);
        result.put(TP_ORDER_ID, payOrder.getTradeNo());
        result.put(DEAL_ID, dealId);
        result.put(DEAL_TITLE, payOrder.getSubject());
        result.put(SIGN_FIELDS_RANGE, payOrder.getSignFieldsRange());
        result.put(BIZ_INFO, JSON.toJSONString(payOrder.getBizInfo()));
        result.put(TOTAL_AMOUNT, String.valueOf(Util.conversionAmount(order.getPrice())));

        return result;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param code    ??????
     * @param message ??????
     * @return ??????
     */
    @Override
    @Deprecated
    public PayOutMessage getPayOutMessage(String code, String message) {
        throw new UnsupportedOperationException("????????? " + getClass().getName() + "#getPayOutMessageUseBaidu");
    }

    /**
     * ???????????????????????????/????????????
     * http://smartprogram.baidu.com/docs/develop/function/tune_up_examine/
     *
     * @param errno          ????????????
     * @param message        ??????
     * @param auditStatus    ??????
     * @param refundPayMoney ????????????
     * @return ??????
     */
    public PayOutMessage getApplyRefundOutMessageUseBaidu(Integer errno,
                                                          String message,
                                                          AuditStatus auditStatus,
                                                          BigDecimal refundPayMoney) {
        JSONObject data = new JSONObject();
        data.put("auditStatus", auditStatus.getCode());
        JSONObject calculateRes = new JSONObject();
        calculateRes.put("refundPayMoney", refundPayMoney);
        data.put("calculateRes", calculateRes);
        return PayOutMessage.JSON()
                .content("errno", errno)
                .content("message", message)
                .content("data", data)
                .build();

    }

    /**
     * ??????????????????/????????????
     * http://smartprogram.baidu.com/docs/develop/function/tune_up_drawback/
     *
     * @param errno   ????????????
     * @param message ??????
     * @return ??????
     */
    public PayOutMessage getRefundOutMessageUseBaidu(Integer errno,
                                                     String message) {
        return PayOutMessage.JSON()
                .content("errno", errno)
                .content("message", message)
                .content("data", "{}")
                .build();

    }

    /**
     * ????????????/????????????
     *
     * @param errno        ????????????
     * @param message      ??????
     * @param isConsumed   ????????????
     * @param isErrorOrder ????????????
     * @return ??????
     */
    public PayOutMessage getPayOutMessageUseBaidu(Integer errno,
                                                  String message,
                                                  Integer isConsumed,
                                                  Integer isErrorOrder) {
        Asserts.isNoNull(errno, "errno ????????????");
        Asserts.isNoNull(message, "message ????????????");
        Asserts.isNoNull(isConsumed, "isConsumed ????????????");
        JSONObject data = new JSONObject();
        data.put("isConsumed", isConsumed);
        if (isErrorOrder != null) {
            data.put("isErrorOrder", isErrorOrder);
        }
        return PayOutMessage.JSON()
                .content("errno", errno)
                .content("message", message)
                .content("data", data)
                .build();
    }

    /**
     * ????????????/????????????
     * http://smartprogram.baidu.com/docs/develop/function/tune_up_notice/
     *
     * @param code       ?????????
     * @param message    ??????
     * @param isConsumed ????????????
     * @return ??????
     */
    public PayOutMessage getPayOutMessageUseBaidu(Integer code,
                                                  String message,
                                                  Integer isConsumed) {
        return getPayOutMessageUseBaidu(code, message, isConsumed, null);
    }

    /**
     * ????????????/????????????
     * http://smartprogram.baidu.com/docs/develop/function/tune_up_notice/
     *
     * @param payMessage ??????????????????
     * @return ??????
     */
    @Override
    public PayOutMessage successPayOutMessage(PayMessage payMessage) {
        return getPayOutMessageUseBaidu(0, "success", 2);
    }

    /**
     * ?????????????????????????????????????????????, ?????????web???
     *
     * @param orderInfo ???????????????????????????
     * @param method    ????????????  "post" "get",
     * @return ??????
     */
    @Override
    public String buildRequest(Map<String, Object> orderInfo,
                               MethodType method) {
        throw new UnsupportedOperationException("???????????????PC??????");
    }


    /**
     * ????????????????????????
     *
     * @param order ???????????????????????????
     * @return ??????
     */
    @Override
    public String getQrPay(PayOrder order) {
        throw new UnsupportedOperationException("????????????????????????");
    }

    /**
     * ????????????????????????
     *
     * @param order ???????????????????????????
     * @return ??????
     */
    @Override
    public Map<String, Object> microPay(PayOrder order) {
        throw new UnsupportedOperationException("????????????????????????");
    }

    /**
     * ????????????
     *
     * @param tradeNo    ?????????????????????
     * @param outTradeNo ????????????
     * @return ??????
     */
    @Override
    public Map<String, Object> query(String tradeNo, String outTradeNo) {
        return secondaryInterface(tradeNo, outTradeNo, BaiduTransactionType.PAY_QUERY);
    }

    /**
     * ????????????????????????
     *
     * @param tradeNo    ?????????????????????
     * @param outTradeNo ????????????
     * @return ??????
     */
    @Override
    public Map<String, Object> close(String tradeNo, String outTradeNo) {
        throw new UnsupportedOperationException("??????????????????");
    }


    /**
     * ??????
     *
     * @param refundOrder ??????????????????
     * @return ????????????
     */
    @Override
    public BaseRefundResult refund(RefundOrder refundOrder) {
        Map<String, Object> parameters = getUseQueryPay();
        BaiduTransactionType transactionType = BaiduTransactionType.APPLY_REFUND;
        parameters.put(METHOD, transactionType.getMethod());
        parameters.put(ORDER_ID, refundOrder.getOutTradeNo());
        parameters.put(USER_ID, refundOrder.getUserId());
        setParameters(parameters, "refundType", refundOrder);
        parameters.put("refundReason", refundOrder.getDescription());
        parameters.put(TP_ORDER_ID, refundOrder.getTradeNo());
        parameters.put("applyRefundMoney", refundOrder.getRefundAmount());
        parameters.put("bizRefundBatchId", refundOrder.getRefundNo());
        parameters.put(APP_KEY, payConfigStorage.getAppKey());
        parameters.put(RSA_SIGN, getRsaSign(parameters, RSA_SIGN));
        final JSONObject result = requestTemplate.getForObject(String.format("%s?%s", getReqUrl(transactionType), UriVariables.getMapToParameters(parameters)), JSONObject.class);
        return new BaseRefundResult(result) {
            @Override
            public String getCode() {
                return getAttrString(RESPONSE_STATUS);
            }

            @Override
            public String getMsg() {
                return null;
            }

            @Override
            public String getResultCode() {
                return null;
            }

            @Override
            public String getResultMsg() {
                return null;
            }

            @Override
            public BigDecimal getRefundFee() {
                return null;
            }

            @Override
            public CurType getRefundCurrency() {
                return null;
            }

            @Override
            public String getTradeNo() {
                return null;
            }

            @Override
            public String getOutTradeNo() {
                return null;
            }

            @Override
            public String getRefundNo() {
                return null;
            }
        };

    }


    /**
     * ????????????
     *
     * @param refundOrder ????????????????????????
     * @return ??????????????????
     */
    @Override
    public Map<String, Object> refundquery(RefundOrder refundOrder) {

        Map<String, Object> parameters = getUseQueryPay();
        BaiduTransactionType transactionType = BaiduTransactionType.REFUND_QUERY;
        parameters.put(METHOD, transactionType.getMethod());
        parameters.put(TYPE, 3);
        parameters.put(ORDER_ID, refundOrder.getTradeNo());
        parameters.put(USER_ID, refundOrder.getUserId());
        parameters.put(APP_KEY, payConfigStorage.getAppKey());
        parameters.put(RSA_SIGN, getRsaSign(parameters, RSA_SIGN));
        return requestTemplate.getForObject(String.format("%s?%s", getReqUrl(transactionType), UriVariables.getMapToParameters(parameters)), JSONObject.class);
    }

    /**
     * ?????????????????????
     *
     * @param billDate    ?????????????????????????????????yyyy-MM-dd
     * @param accessToken ??????token
     * @return ?????????
     */
    @Deprecated
    @Override
    public Map<String, Object> downloadbill(Date billDate, String accessToken) {
        return downloadBill(billDate, new BaiduBillType(accessToken, BaiduTransactionType.DOWNLOAD_ORDER_BILL.name()));
    }

    /**
     * ???????????????
     *
     * @param billDate ?????????????????????????????????yyyy-MM-dd?????????????????????yyyy-MM???
     * @param billType ???????????? {@link BaiduBillType}
     * @return ???????????????????????????????????????
     */
    public Map<String, Object> downloadBill(Date billDate, BillType billType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("access_token", billType.getCustom());
        parameters.put("billTime", DateUtils.formatDate(billDate, billType.getDatePattern()));
        final String type = billType.getType();
        BaiduTransactionType transactionType = BaiduTransactionType.DOWNLOAD_ORDER_BILL;
        if (BaiduTransactionType.DOWNLOAD_BILL.name().equals(type)) {
            transactionType = BaiduTransactionType.DOWNLOAD_BILL;
        }
        return requestTemplate.getForObject(String.format("%s?%s", getReqUrl(transactionType),
                UriVariables.getMapToParameters(parameters)), JSONObject.class);
    }

    /**
     * ??????????????????
     *
     * @param billDate    ?????????????????????????????????yyyy-MM-dd
     * @param accessToken ??????token
     * @return ????????????
     */
    @Deprecated
    public Map<String, Object> downloadMoneyBill(Date billDate, String accessToken) {
        return downloadBill(billDate, new BaiduBillType(accessToken, BaiduTransactionType.DOWNLOAD_BILL.name()));
    }

    /**
     * ??????????????????
     *
     * @param orderId         ??????id
     * @param siteId          ??????id
     * @param transactionType ????????????
     * @return ??????
     */
    public Map<String, Object> secondaryInterface(Object orderId,
                                                  String siteId,
                                                  TransactionType transactionType) {
        if (!BaiduTransactionType.PAY_QUERY.equals(transactionType)) {
            throw new UnsupportedOperationException("??????????????????");
        }

        Map<String, Object> parameters = getUseQueryPay();
        parameters.put(ORDER_ID, orderId);
        parameters.put(SITE_ID, siteId);
        parameters.put(SIGN, getRsaSign(parameters, SIGN));
        return requestTemplate.getForObject(String.format("%s?%s", getReqUrl(transactionType), UriVariables.getMapToParameters(parameters)), JSONObject.class);
    }

    /**
     * ????????????????????????
     *
     * @param transactionType ????????????
     * @return ??????URL
     */
    @Override
    public String getReqUrl(TransactionType transactionType) {
        return ((BaiduTransactionType) transactionType).getUrl();
    }

    /**
     * ??????
     *
     * @param params     ??????
     * @param ignoreKeys ????????????
     * @return ????????????
     */
    private String getRsaSign(Map<String, Object> params, String... ignoreKeys) {
        String waitSignVal = SignUtils.parameterText(params, "&", false, ignoreKeys);
        return SignUtils.valueOf(payConfigStorage.getSignType()).createSign(waitSignVal, payConfigStorage.getKeyPrivate(), payConfigStorage.getInputCharset());
    }
}
