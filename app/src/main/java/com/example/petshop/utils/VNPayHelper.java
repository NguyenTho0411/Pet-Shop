package com.example.petshop.utils;

import android.text.format.DateFormat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPay sandbox payment URL builder.
 * Đặt TMN_CODE + HASH_SECRET vào local.properties.
 */
public class VNPayHelper {

    private static final String VNPAY_VERSION = "2.1.0";
    private static final String VNPAY_COMMAND = "pay";
    private static final String VNPAY_CURR_CODE = "VND";
    private static final String VNPAY_LOCALE = "vn";
    private static final String VNPAY_ORDER_TYPE = "other";

    public static String buildPaymentUrl(String orderId, long amount, String orderInfo) {
        String tmnCode    = Constants.VNPAY_TMN_CODE;
        String hashSecret = Constants.VNPAY_HASH_SECRET;
        String vnpUrl     = Constants.VNPAY_URL;
        String returnUrl  = Constants.VNPAY_RETURN_URL;

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));

        String createDate = formatter.format(cld.getTime());

        // Expire time: +15 minutes
        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());

        Map<String, String> params = new TreeMap<>(); // TreeMap sorts keys alphabetically
        params.put("vnp_Version",    VNPAY_VERSION);
        params.put("vnp_Command",    VNPAY_COMMAND);
        params.put("vnp_TmnCode",    tmnCode);
        params.put("vnp_Amount",     String.valueOf(amount * 100)); // VNPay nhân 100
        params.put("vnp_CurrCode",   VNPAY_CURR_CODE);
        params.put("vnp_TxnRef",     orderId);
        params.put("vnp_OrderInfo",  orderInfo);
        params.put("vnp_OrderType",  VNPAY_ORDER_TYPE);
        params.put("vnp_Locale",     VNPAY_LOCALE);
        params.put("vnp_ReturnUrl",  returnUrl);
        params.put("vnp_IpAddr",     "127.0.0.1");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        StringBuilder hashData  = new StringBuilder();
        StringBuilder query     = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) { hashData.append('&'); query.append('&'); }
            hashData.append(e.getKey()).append('=').append(e.getValue());
            try {
                query.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                     .append('=')
                     .append(URLEncoder.encode(e.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                query.append(e.getKey()).append('=').append(e.getValue());
            }
            first = false;
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnpUrl + "?" + query;
    }

    /** Verify callback from VNPay */
    public static boolean verifyReturn(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null) return false;

        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");

        StringBuilder data = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (!first) data.append('&');
            data.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }

        String hash = hmacSHA512(Constants.VNPAY_HASH_SECRET, data.toString());
        return hash.equalsIgnoreCase(secureHash);
    }

    public static boolean isSuccess(String responseCode) {
        return "00".equals(responseCode);
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
