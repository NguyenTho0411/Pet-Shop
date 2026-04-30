package com.example.petshop.utils;

import android.os.Handler;
import android.os.Looper;

import com.example.petshop.model.entity.Address;

/**
 * Tính phí ship dựa theo tỉnh/thành.
 * Có thể swap bằng GHN/GHTK API thật bằng cách thay phần simulateApiCall.
 */
public class ShippingHelper {

    public interface ShippingCallback {
        void onResult(double fee, String estimatedDays);
        void onError(String error);
    }

    // Shop location (TP.HCM)
    private static final String SHOP_CITY = "Hồ Chí Minh";

    private static final double FEE_SAME_DISTRICT  = 15_000;
    private static final double FEE_SAME_CITY      = 30_000;
    private static final double FEE_SOUTH          = 45_000;   // miền Nam
    private static final double FEE_CENTRAL        = 60_000;   // miền Trung
    private static final double FEE_NORTH          = 75_000;   // miền Bắc

    private static final double FREE_SHIP_THRESHOLD = 500_000; // miễn ship >= 500k

    public static void calculate(Address address, double orderSubtotal, ShippingCallback cb) {
        if (address == null) { cb.onError("Chưa chọn địa chỉ giao hàng"); return; }

        // Simulate async call (replace with real API call here)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            double fee      = calcFee(address);
            String etaDays  = calcEta(address);

            // Free ship nếu đủ điều kiện
            if (orderSubtotal >= FREE_SHIP_THRESHOLD) fee = 0;

            cb.onResult(fee, etaDays);
        }, 500);
    }

    private static double calcFee(Address addr) {
        String city = addr.getCity();
        if (city == null) return FEE_CENTRAL;

        String cityLower = city.toLowerCase();
        if (cityLower.contains("hồ chí minh") || cityLower.contains("ho chi minh")) {
            return FEE_SAME_CITY;
        }
        if (isSouthCity(cityLower))    return FEE_SOUTH;
        if (isCentralCity(cityLower))  return FEE_CENTRAL;
        return FEE_NORTH;
    }

    private static String calcEta(Address addr) {
        String city = addr.getCity();
        if (city == null) return "3-5 ngày";
        String cl = city.toLowerCase();
        if (cl.contains("hồ chí minh") || cl.contains("ho chi minh")) return "1-2 ngày";
        if (isSouthCity(cl))   return "2-3 ngày";
        if (isCentralCity(cl)) return "3-4 ngày";
        return "4-6 ngày";
    }

    private static boolean isSouthCity(String city) {
        return city.contains("bình dương") || city.contains("đồng nai")
                || city.contains("bà rịa") || city.contains("long an")
                || city.contains("tiền giang") || city.contains("cần thơ")
                || city.contains("an giang") || city.contains("vĩnh long")
                || city.contains("kiên giang") || city.contains("cà mau");
    }

    private static boolean isCentralCity(String city) {
        return city.contains("đà nẵng") || city.contains("huế")
                || city.contains("quảng nam") || city.contains("bình định")
                || city.contains("khánh hòa") || city.contains("nha trang")
                || city.contains("lâm đồng") || city.contains("đà lạt")
                || city.contains("gia lai") || city.contains("kon tum");
    }
}
