package com.example.petshop.view.dialog;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

/**
 * DialogUtils - Utility class để quản lý dialogs
 * Cung cấp các method tiện lợi để hiển thị dialog một cách đơn giản
 * Support cả Activity và Fragment
 */
public class DialogUtils {

    /**
     * Lấy FragmentManager từ Activity
     */
    private static FragmentManager getFragmentManager(Activity activity) {
        if (activity instanceof AppCompatActivity) {
            return ((AppCompatActivity) activity).getSupportFragmentManager();
        }
        throw new RuntimeException("Activity phải extends AppCompatActivity");
    }

    /**
     * Hiển thị ConfirmDialog với callback
     * 
     * @param fragmentManager FragmentManager từ Activity/Fragment
     * @param title Tiêu đề dialog
     * @param message Nội dung dialog
     * @param positiveText Text nút dương tính (Có, Xóa, v.v)
     * @param negativeText Text nút âm tính (Không, Hủy, v.v)
     * @param listener Callback xử lý sự kiện
     * @param tag Unique tag cho dialog
     */
    public static void showConfirmDialog(FragmentManager fragmentManager,
                                        String title,
                                        String message,
                                        String positiveText,
                                        String negativeText,
                                        ConfirmDialog.OnConfirmListener listener,
                                        String tag) {
        ConfirmDialog dialog = ConfirmDialog.newInstance(title, message, positiveText, negativeText, listener);
        dialog.show(fragmentManager, tag);
    }

    /**
     * Hiển thị ConfirmDialog từ Activity
     */
    public static void showConfirmDialog(Activity activity,
                                        String title,
                                        String message,
                                        String positiveText,
                                        String negativeText,
                                        ConfirmDialog.OnConfirmListener listener,
                                        String tag) {
        showConfirmDialog(getFragmentManager(activity), title, message, positiveText, negativeText, listener, tag);
    }

    /**
     * Hiển thị ConfirmDialog với các giá trị mặc định
     */
    public static void showConfirmDialog(FragmentManager fragmentManager,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(fragmentManager, "Xác nhận", message, "Có", "Không", listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog với title và message tùy chỉnh, nút mặc định
     */
    public static void showConfirmDialog(FragmentManager fragmentManager,
                                        String title,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(fragmentManager, title, message, "Có", "Không", listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog với title, message, và nút tùy chỉnh
     */
    public static void showConfirmDialog(FragmentManager fragmentManager,
                                        String title,
                                        String message,
                                        String positiveText,
                                        String negativeText,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(fragmentManager, title, message, positiveText, negativeText, listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog từ Activity với các giá trị mặc định
     */
    public static void showConfirmDialog(Activity activity,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(activity, "Xác nhận", message, "Có", "Không", listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog từ Activity với title và message tùy chỉnh
     */
    public static void showConfirmDialog(Activity activity,
                                        String title,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(activity, title, message, "Có", "Không", listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog từ Activity với title, message và nút tùy chỉnh
     */
    public static void showConfirmDialog(Activity activity,
                                        String title,
                                        String message,
                                        String positiveText,
                                        String negativeText,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(getFragmentManager(activity), title, message, positiveText, negativeText, listener, "confirmDialog");
    }

    /**
     * Hiển thị ConfirmDialog để xóa
     */
    public static void showDeleteConfirmDialog(FragmentManager fragmentManager,
                                              String itemName,
                                              ConfirmDialog.OnConfirmListener listener) {
        String message = "Xoá \"" + itemName + "\"? Không thể hoàn tác!";
        showConfirmDialog(fragmentManager, "Xóa " + itemName, message, "Xóa", "Hủy", listener, "deleteDialog");
    }

    /**
     * Hiển thị ConfirmDialog để xóa từ Activity
     */
    public static void showDeleteConfirmDialog(Activity activity,
                                              String itemName,
                                              ConfirmDialog.OnConfirmListener listener) {
        String message = "Xoá \"" + itemName + "\"? Không thể hoàn tác!";
        showConfirmDialog(activity, "Xóa " + itemName, message, "Xóa", "Hủy", listener, "deleteDialog");
    }

    /**
     * Hiển thị ConfirmDialog để xóa khỏi giỏ hàng
     */
    public static void showRemoveFromCartDialog(Activity activity,
                                               String itemName,
                                               ConfirmDialog.OnConfirmListener listener) {
        String message = "Xoá \"" + itemName + "\" khỏi giỏ hàng?";
        showConfirmDialog(activity, "Xóa khỏi giỏ", message, "Xóa", "Hủy", listener, "removeCartDialog");
    }

    /**
     * Hiển thị LoadingDialog
     * 
     * @param fragmentManager FragmentManager từ Activity/Fragment
     * @param message Thông báo tải
     * @return LoadingDialog instance để có thể tắt sau này
     */
    public static LoadingDialog showLoadingDialog(FragmentManager fragmentManager, String message) {
        LoadingDialog dialog = LoadingDialog.newInstance(message);
        dialog.show(fragmentManager, "loadingDialog");
        return dialog;
    }

    /**
     * Hiển thị LoadingDialog từ Activity
     */
    public static LoadingDialog showLoadingDialog(Activity activity, String message) {
        return showLoadingDialog(getFragmentManager(activity), message);
    }

    /**
     * Hiển thị LoadingDialog với message mặc định
     */
    public static LoadingDialog showLoadingDialog(FragmentManager fragmentManager) {
        return showLoadingDialog(fragmentManager, "Đang tải...");
    }

    /**
     * Hiển thị LoadingDialog từ Activity với message mặc định
     */
    public static LoadingDialog showLoadingDialog(Activity activity) {
        return showLoadingDialog(activity, "Đang tải...");
    }

    /**
     * Hiển thị dialog thanh toán
     */
    public static void showCheckoutConfirmDialog(FragmentManager fragmentManager,
                                                String amount,
                                                ConfirmDialog.OnConfirmListener listener) {
        String message = "Thanh toán " + amount + "? Vui lòng kiểm tra lại thông tin.";
        showConfirmDialog(fragmentManager, "Xác nhận thanh toán", message, "Thanh toán", "Hủy", listener, "checkoutDialog");
    }

    /**
     * Hiển thị dialog thanh toán từ Activity
     */
    public static void showCheckoutConfirmDialog(Activity activity,
                                                String amount,
                                                ConfirmDialog.OnConfirmListener listener) {
        String message = "Thanh toán " + amount + "? Vui lòng kiểm tra lại thông tin.";
        showConfirmDialog(activity, "Xác nhận thanh toán", message, "Thanh toán", "Hủy", listener, "checkoutDialog");
    }

    /**
     * Hiển thị dialog cảnh báo
     */
    public static void showWarningDialog(FragmentManager fragmentManager,
                                        String title,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(fragmentManager, title, message, "Đóng", "Quay lại", listener, "warningDialog");
    }

    /**
     * Hiển thị dialog cảnh báo từ Activity
     */
    public static void showWarningDialog(Activity activity,
                                        String title,
                                        String message,
                                        ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(activity, title, message, "Đóng", "Quay lại", listener, "warningDialog");
    }

    /**
     * Hiển thị dialog lỗi
     */
    public static void showErrorDialog(FragmentManager fragmentManager,
                                       String errorMessage,
                                       ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(fragmentManager, "Lỗi", errorMessage, "Đóng", null, listener, "errorDialog");
    }

    /**
     * Hiển thị dialog lỗi từ Activity
     */
    public static void showErrorDialog(Activity activity,
                                       String errorMessage,
                                       ConfirmDialog.OnConfirmListener listener) {
        showConfirmDialog(activity, "Lỗi", errorMessage, "Đóng", null, listener, "errorDialog");
    }
}
