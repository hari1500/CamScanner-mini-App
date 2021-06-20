package cam.scanner.mini.utils;

public class Constants {
    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss aa";
    public static final String DATE_FORMAT_FILE_NAME = "yyyy-MM-dd hh.mm.ss";

    public static final int NUM_CORNERS_OF_POLYGON = 4;

    // Reserved numbers -- 11
    public static final int LAUNCH_CAMERA_ACTIVITY_FROM_DOCUMENTS_ACTIVITY              = 1;
    public static final int LAUNCH_CAMERA_ACTIVITY_FROM_PAGES_ACTIVITY                  = 2;
    public static final int LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_CAMERA_ACTIVITY        = 3;
    public static final int LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_DOCUMENTS_ACTIVITY     = 4;
    public static final int LAUNCH_CAPTURED_IMAGES_ACTIVITY_FROM_PAGES_ACTIVITY         = 5;
    public static final int LAUNCH_GALLERY_FROM_PAGES_ACTIVITY                          = 6;
    public static final int LAUNCH_EDIT_IMAGE_ACTIVITY2_FROM_EDIT_IMAGE_ACTIVITY1       = 7;
    public static final int LAUNCH_EDIT_IMAGE_ACTIVITY1_FROM_PAGES_ACTIVITY2            = 8;
    public static final int LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_CAPTURED_IMAGES_ACTIVITY  = 9;
    public static final int LAUNCH_RETAKE_IMAGE_ACTIVITY_FROM_PAGES_ACTIVITY2           = 10;

    public static final int CAMERA_PERMISSION_REQ_CODE              = 100;
    public static final int READ_EXT_STORAGE_PERMISSION_REQ_CODE    = 101;
    public static final int WRITE_EXT_STORAGE_PERMISSION_REQ_CODE   = 102;

    public static final int CLOCK_WISE_ROTATION_DEGREES     = 90;
    public static final int TWO_ROTATION_DEGREES            = 180;
    public static final int ANTI_CLOCK_WISE_ROTATION_DEGREES= 270;

    public static final String CURRENT_POSITION_FOR_PAGES_2_ACTIVITY_KEY            = "CURRENT_POSITION_FOR_PAGES_2_ACTIVITY";
    public static final String SELECTED_IND_FOR_PAGES_MULTI_SELECT_ACTIVITY_KEY     = "SELECTED_IND_FOR_PAGES_MULTI_SELECT_ACTIVITY";
    public static final String SELECTED_IND_FOR_DOCUMENTS_MULTI_SELECT_ACTIVITY_KEY = "SELECTED_IND_FOR_DOCUMENTS_MULTI_SELECT_ACTIVITY";

    public static final String ORIGINAL_IMAGE_PATH_FORMAT = "%s_%s_original_image.jpeg";
    public static final String MODIFIED_IMAGE_PATH_FORMAT = "%s_%s_modified_image.jpeg";
}
