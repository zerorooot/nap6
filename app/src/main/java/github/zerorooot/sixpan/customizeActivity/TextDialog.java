package github.zerorooot.sixpan.customizeActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Objects;

import github.zerorooot.sixpan.R;
import lombok.Setter;

@Setter
public class TextDialog extends DialogFragment {
    private int visibility;
    private byte[] content;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        R.style.BottomDialog
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TextDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Objects.requireNonNull(getDialog()).getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);

        View v = inflater.inflate(R.layout.text_dialog, container, false);

        TextInputLayout textInputLayout = v.findViewById(R.id.codeEditViewLayout);
        textInputLayout.setVisibility(visibility);

        EditText editText = v.findViewById(R.id.codeEditView);


        editText.setVisibility(visibility);
        editText.setText(Charset.defaultCharset().toString());

        TextView textView = v.findViewById(R.id.contentTextView);
        textView.setText(new String(content, Charset.defaultCharset()));

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    textView.setText(new String(content, s.toString()));
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        Window window = Objects.requireNonNull(getDialog()).getWindow();
        WindowManager.LayoutParams params = window.getAttributes();

        params.dimAmount = 0.4f;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;

        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        params.gravity = Gravity.CENTER;

        window.setAttributes(params);
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, "text_dialog");
    }

}
