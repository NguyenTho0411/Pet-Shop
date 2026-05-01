package com.example.petshop.utils;

import android.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Theo đúng VNPay Java official sample:
 *  hashData : fieldName (raw) = URLEncoder.encode(value, UTF-8)  ← dùng + cho space
 *  query    : URLEncode(key)  = URLEncode(value).replace(+,%20)  ← dùng %20 cho URL đẹp
 *  secureHash: lowercase hex (không valueForHash  )
 */
public class VNPayHelper {

    public static String buildPaymentUrl(String orderCode, long amount, String orderInfo) {
        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version",   "2.1.0");
        vnp_Params.put("vnp_Command",   "pay");
        vnp_Params.put("vnp_TmnCode",   Constants.VNPAY_TMN_CODE.trim());
        vnp_Params.put("vnp_Amount",    String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode",  "VND");
        vnp_Params.put("vnp_TxnRef",    orderCode);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale",    "vn");
        vnp_Params.put("vnp_ReturnUrl", Constants.VNPAY_RETURN_URL.trim());
        vnp_Params.put("vnp_IpAddr",    "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnp_Params.put("vnp_CreateDate", sdf.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", sdf.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);

            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }

            try {
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());

                if (hashData.length() > 0) {
                    hashData.append('&');
                    query.append('&');
                }

                hashData.append(encodedName).append('=').append(encodedValue);
                query.append(encodedName).append('=').append(encodedValue);

            } catch (Exception e) {
                Log.e("VNPAY_DEBUG", "Encode error", e);
            }
        }

        String secureHash = hmacSHA512(Constants.VNPAY_HASH_SECRET.trim(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = Constants.VNPAY_URL + "?" + query;

        Log.d("VNPAY_DEBUG", "HashData: " + hashData);
        Log.d("VNPAY_DEBUG", "Hash: " + secureHash);
        Log.d("VNPAY_DEBUG", "PaymentUrl: " + paymentUrl);

        return paymentUrl;
    }

    public static boolean isSuccess(String responseCode) {
        return "00".equals(responseCode);
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
