package com.example.petshop.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.example.petshop.R;

/**
 * ConfirmDialog - Dialog xác nhận hành động
 * Sử dụng cho các trường hợp: xóa, hủy, mua hàng, v.v.
 */
public class ConfirmDialog extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE_TEXT = "positive_text";
    private static final String ARG_NEGATIVE_TEXT = "negative_text";

    private OnConfirmListener listener;
    private String title;
    private String message;
    private String positiveText;
    private String negativeText;

    public interface OnConfirmListener {
        void onConfirm();
        void onCancel();
    }

    public ConfirmDialog() {
        // Required empty public constructor
    }

    /**
     * Tạo instance của ConfirmDialog với các tham số
     */
    public static ConfirmDialog newInstance(String title, String message, 
                                           String positiveText, String negativeText) {
        ConfirmDialog dialog = new ConfirmDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE_TEXT, positiveText);
        args.putString(ARG_NEGATIVE_TEXT, negativeText);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "Xác nhận");
            message = getArguments().getString(ARG_MESSAGE, "Bạn có chắc chắn?");
            positiveText = getArguments().getString(ARG_POSITIVE_TEXT, "Có");
            negativeText = getArguments().getString(ARG_NEGATIVE_TEXT, "Không");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_confirm, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        Button btnPositive = view.findViewById(R.id.btn_dialog_positive);
        Button btnNegative = view.findViewById(R.id.btn_dialog_negative);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(positiveText);
        btnNegative.setText(negativeText);

        btnPositive.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm();
            }
            dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });

        return view;
    }

    /**
     * Set listener cho dialog
     */
    public void setConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    /**
     * Shortcut để set listener khi khởi tạo
     */
    public static ConfirmDialog newInstance(String title, String message, 
                                           String positiveText, String negativeText,
                                           OnConfirmListener listener) {
        ConfirmDialog dialog = newInstance(title, message, positiveText, negativeText);
        dialog.setConfirmListener(listener);
        return dialog;
    }
}
