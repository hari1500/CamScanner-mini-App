package cam.scanner.mini.utils;

import android.content.Context;
import android.view.OrientationEventListener;

public abstract class RotateOrientationEventListener extends OrientationEventListener {
    public static final int ORIENTATION_PORTRAIT = 0;
    public static final int ORIENTATION_LANDSCAPE_REVERSE = 1;
    public static final int ORIENTATION_PORTRAIT_REVERSE = 2;
    public static final int ORIENTATION_LANDSCAPE = 3;

    private int lastOrientation = 0;

    public RotateOrientationEventListener(Context context) {
        super(context);
    }

    public RotateOrientationEventListener(Context context, int rate) {
        super(context, rate);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation < 0) {
            return; // Flip screen, Not take account
        }

        int curOrientation = (orientation <= 45)
                ? ORIENTATION_PORTRAIT
                : (
                        (orientation <= 135)
                        ? ORIENTATION_LANDSCAPE_REVERSE
                        : (
                                (orientation <= 225)
                                ? ORIENTATION_PORTRAIT_REVERSE
                                : (
                                        (orientation <= 315)
                                        ? ORIENTATION_LANDSCAPE
                                        : ORIENTATION_PORTRAIT
                                )
                        )
                );

        if (curOrientation != lastOrientation) {
            onChanged(lastOrientation, curOrientation);
            lastOrientation = curOrientation;
        }
    }

    public final void onChanged(int lastOrientation, int orientation) {
        int startDeg = (lastOrientation == ORIENTATION_PORTRAIT)
                ? (
                        (orientation == ORIENTATION_LANDSCAPE_REVERSE) ? 360 : 0
                )
                : (
                        (lastOrientation == ORIENTATION_LANDSCAPE)
                        ? 90
                        : (
                                (lastOrientation == ORIENTATION_PORTRAIT_REVERSE) ? 180 : 270
                        )
                );
        int endDeg = (orientation == ORIENTATION_PORTRAIT)
                ? (
                        (lastOrientation == ORIENTATION_LANDSCAPE) ? 0 : 360
                )
                : (
                        (orientation == ORIENTATION_LANDSCAPE)
                        ? 90
                        : (
                                (orientation == ORIENTATION_PORTRAIT_REVERSE) ? 180 : 270
                        )
                );

        onRotateChanged(startDeg, endDeg);
    }

    public abstract void onRotateChanged(int startDeg, int endDeg);

    public int getOrientation() {
        return lastOrientation;
    }
}
