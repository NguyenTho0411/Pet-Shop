package com.example.petshop.view.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.petshop.R;

/**
 * LoadingDialog - Dialog hiển thị quá trình tải dữ liệu
 * Sử dụng khi đang fetch API, upload file, xử lý dữ liệu...
 */
public class LoadingDialog extends DialogFragment {
    private static final String ARG_MESSAGE = "message";
    
    private String loadingMessage;
    private ProgressBar progressBar;
    private TextView tvMessage;

    public LoadingDialog() {
        // Required empty public constructor
    }

    /**
     * Tạo instance của LoadingDialog với message
     */
    public static LoadingDialog newInstance(String message) {
        LoadingDialog dialog = new LoadingDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Tạo instance mặc định với message "Đang tải..."
     */
    public static LoadingDialog newInstance() {
        return newInstance("Đang tải...");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            loadingMessage = getArguments().getString(ARG_MESSAGE, "Đang tải...");
        }
        // Set style để dialog không bị hủy khi click bên ngoài
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Không cho phép đóng dialog bằng cách click bên ngoài
        dialog.setCanceledOnTouchOutside(false);
        // Không cho phép đóng bằng nút back
        dialog.setCancelable(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_loading, container, false);

        progressBar = view.findViewById(R.id.progress_bar_loading);
        tvMessage = view.findViewById(R.id.tv_loading_message);

        tvMessage.setText(loadingMessage);

        return view;
    }

    /**
     * Cập nhật message của loading dialog
     */
    public void setMessage(String message) {
        this.loadingMessage = message;
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }

    /**
     * Kiểm tra dialog có đang hiển thị hay không
     */
    public boolean isShowing() {
        return getDialog() != null && getDialog().isShowing();
    }

    /**
     * Hủy dialog một cách an toàn
     */
    public void dismissSafely() {
        if (isAdded()) {
            dismiss();
        }
    }
}
