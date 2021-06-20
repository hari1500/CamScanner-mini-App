package cam.scanner.mini.localdatabase;

import android.graphics.PointF;
import android.text.TextUtils;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final String STRING_DELIMITER_REGEX = ", ";

    @TypeConverter
    public static List<Long> toListOfLongs(String string) {
        List<Long> longList = new ArrayList<>();
        if (string == null || string.trim().equals("")) {
            return longList;
        }

        String[] stringsArray = string.trim().split(STRING_DELIMITER_REGEX);
        for (String s : stringsArray) {
            longList.add(Long.parseLong(s));
        }

        return longList;
    }

    @TypeConverter
    public static String fromListOfLongs(List<Long> longList) {
        return (longList == null) ? null : TextUtils.join(STRING_DELIMITER_REGEX, longList);
    }

    @TypeConverter
    public static List<PointF> toListOfPointFs(String string) {
        List<PointF> pointFList = new ArrayList<>();
        if (string == null || string.trim().equals("")) {
            return pointFList;
        }

        String[] stringsArray = string.trim().split(STRING_DELIMITER_REGEX);
        for (int i = 0; i + 1 < stringsArray.length; i += 2) {
            pointFList.add(new PointF(Float.parseFloat(stringsArray[i]), Float.parseFloat(stringsArray[i+1])));
        }

        return pointFList;
    }

    @TypeConverter
    public static String fromListOfPointFs(List<PointF> pointFList) {
        if (pointFList == null) {
            return null;
        }
        ArrayList<Float> floatArrayList = new ArrayList<>();
        for (PointF pointF : pointFList) {
            floatArrayList.add(pointF.x);
            floatArrayList.add(pointF.y);
        }
        return TextUtils.join(STRING_DELIMITER_REGEX, floatArrayList);
    }
}
