package com.example.petshop.view.dialog;

import android.widget.Toast;
import androidx.fragment.app.FragmentManager;

/**
 * QUICK GUIDE - Cách sử dụng Dialogs trong Pet-Shop
 * 
 * ==========================================
 * 1. CONFIRM DIALOG - Xác nhận hành động
 * ==========================================
 */
public class DialogExamples {

    /**
     * Ví dụ 1: Xóa sản phẩm
     */
    public void exampleDeleteProduct(FragmentManager fragmentManager) {
        DialogUtils.showDeleteConfirmDialog(fragmentManager, "sản phẩm này", 
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    // Xóa sản phẩm
                    deleteProduct();
                }

                @Override
                public void onCancel() {
                    Toast.makeText(null, "Đã hủy xóa", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * Ví dụ 2: Thanh toán đơn hàng
     */
    public void exampleCheckout(FragmentManager fragmentManager) {
        DialogUtils.showCheckoutConfirmDialog(fragmentManager, "50,000 đ",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    // Xử lý thanh toán
                    processPayment();
                }

                @Override
                public void onCancel() {
                    // Quay lại giỏ hàng
                }
            });
    }

    /**
     * Ví dụ 3: Đăng xuất
     */
    public void exampleLogout(FragmentManager fragmentManager) {
        DialogUtils.showConfirmDialog(fragmentManager, "Đăng xuất",
            "Bạn có chắc muốn đăng xuất?", "Đăng xuất", "Không",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    logout();
                }

                @Override
                public void onCancel() {
                    // Hủy
                }
            }, "logoutDialog");
    }

    /**
     * ==========================================
     * 2. LOADING DIALOG - Hiển thị khi tải dữ liệu
     * ==========================================
     */
    
    /**
     * Ví dụ 4: Tải dữ liệu từ API
     */
    public void exampleFetchData(FragmentManager fragmentManager) {
        // Hiển thị loading
        LoadingDialog loadingDialog = DialogUtils.showLoadingDialog(
            fragmentManager, "Đang tải dữ liệu...");

        // Giả sử đây là API call
        fetchDataFromAPI(new ApiCallback() {
            @Override
            public void onSuccess(Object data) {
                // Ẩn loading
                loadingDialog.dismissSafely();
                // Xử lý dữ liệu
                updateUI(data);
            }

            @Override
            public void onError(String error) {
                loadingDialog.dismissSafely();
                DialogUtils.showErrorDialog(fragmentManager, error,
                    new ConfirmDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() { }

                        @Override
                        public void onCancel() { }
                    });
            }
        });
    }

    /**
     * Ví dụ 5: Upload ảnh
     */
    public void exampleUploadImage(FragmentManager fragmentManager) {
        LoadingDialog dialog = DialogUtils.showLoadingDialog(
            fragmentManager, "Đang tải ảnh lên...");

        uploadImage(new ProgressCallback() {
            @Override
            public void onProgress(int percent) {
                dialog.setMessage("Đang tải ảnh (" + percent + "%)...");
            }

            @Override
            public void onComplete() {
                dialog.dismissSafely();
                Toast.makeText(null, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                dialog.dismissSafely();
                DialogUtils.showErrorDialog(fragmentManager, error,
                    new ConfirmDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() { }

                        @Override
                        public void onCancel() { }
                    });
            }
        });
    }

    /**
     * ==========================================
     * 3. ỨNG DỤNG THỰC TẾ TRONG PROJECT
     * ==========================================
     */

    /**
     * Cách sử dụng trong Activity
     */
    public void exampleInActivity() {
        // Trong onCreate hoặc một method bất kỳ
        // this.getSupportFragmentManager() là FragmentManager
        
        // Xóa sản phẩm
        // DialogUtils.showDeleteConfirmDialog(getSupportFragmentManager(), 
        //     "sản phẩm", listener);

        // Hiển thị loading
        // LoadingDialog loading = DialogUtils.showLoadingDialog(
        //     getSupportFragmentManager(), "Đang xử lý...");
    }

    /**
     * Cách sử dụng trong Fragment
     */
    public void exampleInFragment() {
        // Trong Fragment, sử dụng getParentFragmentManager()
        
        // DialogUtils.showConfirmDialog(getParentFragmentManager(), 
        //     "Thông báo", "Bạn có chắc?", listener);
    }

    // ============ Dummy methods ============
    private void deleteProduct() { }
    private void processPayment() { }
    private void logout() { }
    private void updateUI(Object data) { }
    private void fetchDataFromAPI(ApiCallback callback) { }
    private void uploadImage(ProgressCallback callback) { }

    interface ApiCallback {
        void onSuccess(Object data);
        void onError(String error);
    }

    interface ProgressCallback {
        void onProgress(int percent);
        void onComplete();
        void onError(String error);
    }
}
