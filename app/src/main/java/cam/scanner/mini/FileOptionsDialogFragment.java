package cam.scanner.mini;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cam.scanner.mini.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FileOptionsDialogFragment extends BottomSheetDialogFragment {
    private OnButtonClickListener mListener;
    private String mButtonsPrefix;
    private boolean mShowPDF;
    private boolean mShowLongImage;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onButtonClick(v.getId());
            }
            dismiss();
        }
    };

    public FileOptionsDialogFragment(String buttonsPrefix, OnButtonClickListener buttonClickListener, boolean showPDFoption, boolean showLongImageoption) {
        mButtonsPrefix = buttonsPrefix;
        mListener = buttonClickListener;
        mShowPDF = showPDFoption;
        mShowLongImage = showLongImageoption;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_file_options, container, false);

        View divider1 = view.findViewById(R.id.dialog_file_options_divider_1);
        View divider2 = view.findViewById(R.id.dialog_file_options_divider_2);
        Button pdfButton = view.findViewById(R.id.dialog_file_options_pdf_button);
        Button jpegButton = view.findViewById(R.id.dialog_file_options_jpeg_button);
        Button longImageButton = view.findViewById(R.id.dialog_file_options_long_image_button);

        pdfButton.setText(String.format("%s %s", mButtonsPrefix, getString(R.string.pdf_small)));
        jpegButton.setText(String.format("%s %s", mButtonsPrefix, getString(R.string.jpeg)));
        longImageButton.setText(String.format("%s %s", mButtonsPrefix, getString(R.string.long_image)));

        pdfButton.setOnClickListener(onClickListener);
        jpegButton.setOnClickListener(onClickListener);
        longImageButton.setOnClickListener(onClickListener);

        if (mShowPDF) {
            divider1.setVisibility(View.VISIBLE);
            pdfButton.setVisibility(View.VISIBLE);
        } else {
            divider1.setVisibility(View.GONE);
            pdfButton.setVisibility(View.GONE);
        }
        if (mShowLongImage) {
            divider2.setVisibility(View.VISIBLE);
            longImageButton.setVisibility(View.VISIBLE);
        } else {
            divider2.setVisibility(View.GONE);
            longImageButton.setVisibility(View.GONE);
        }

        return view;
    }

    public interface OnButtonClickListener {
        public void onButtonClick(int resource);
    }
}
