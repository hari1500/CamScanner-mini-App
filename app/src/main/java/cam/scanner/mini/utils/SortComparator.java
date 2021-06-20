package cam.scanner.mini.utils;

import cam.scanner.mini.localdatabase.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class SortComparator {
    public static final int METHOD_INCREASING_CREATED_TIMESTAMP     = 0;
    public static final int METHOD_DECREASING_CREATED_TIMESTAMP     = 1;
    public static final int METHOD_INCREASING_MODIFIED_TIMESTAMP    = 2;
    public static final int METHOD_DECREASING_MODIFIED_TIMESTAMP    = 3;
    public static final int METHOD_DOC_NAME_A_2_Z                   = 4;
    public static final int METHOD_DOC_NAME_Z_2_A                   = 5;

    public static class SortByCreatedDescending implements Comparator<Document>
    {
        @Override
        public int compare(Document o1, Document o2){
            SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
            try {
                Date d1 = format.parse(o1.getCreatedAt());
                Date d2 = format.parse(o2.getCreatedAt());
                return (d2 != null) ? d2.compareTo(d1) : 0;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class SortByCreatedAscending implements Comparator<Document>
    {
        @Override
        public int compare(Document o1, Document o2){
            SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
            try {
                Date d2 = format.parse(o1.getCreatedAt());
                Date d1 = format.parse(o2.getCreatedAt());
                return (d2 != null) ? d2.compareTo(d1) : 0;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class SortByModifiedDescending implements Comparator<Document>
    {
        @Override
        public int compare(Document o1, Document o2){
            SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
            try {
                Date d1 = format.parse(o1.getModifiedAt());
                Date d2 = format.parse(o2.getModifiedAt());
                return (d2 != null) ? d2.compareTo(d1) : 0;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class SortByModifiedAscending implements Comparator<Document>
    {
        @Override
        public int compare(Document o1, Document o2){
            SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
            try {
                Date d2 = format.parse(o1.getModifiedAt());
                Date d1 = format.parse(o2.getModifiedAt());
                return (d2 != null) ? d2.compareTo(d1) : 0;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public static class SortByDocNameDescending implements Comparator<Document>{

        @Override
        public int compare(Document o1, Document o2) {
            String s1 = o1.getName();
            String s2 = o2.getName();
            return s2.compareTo(s1);
        }
    }

    public static class SortByDocNameAscending implements Comparator<Document>{

        @Override
        public int compare(Document o1, Document o2) {
            String s2 = o1.getName();
            String s1 = o2.getName();
            return s2.compareTo(s1);
        }
    }
}