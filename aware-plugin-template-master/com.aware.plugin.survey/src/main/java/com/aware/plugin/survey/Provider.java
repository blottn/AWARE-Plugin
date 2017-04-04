import android.content.ContentProvider;
import android.net.Uri;
import android.provider.BaseColumns;

import com.aware.plugin.survey.DataManager;

public class Provider extends ContentProvider {

    //CONSTANTS
    public static String AUTHORITY = "com.aware.provider.plugin.location.survey";
    public static final int DATABASE_VERSION = 1;
    public static final int Location_Survey = 1;
    public static final int Location_Survey_ID = 2;

    public static String DATABASE_NAME = "location_survey.db";


    public static class Location_Survey_Table implements BaseColumns {
        private Location_Survey_Table() {}

        // Important init stuff
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/location_survey");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.location_survey";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.location_survey";

        public static final String _ID="_id";

        public static final String TIMESTAMP="timestamp";
        public static final String DEVICE_ID="device_id";

        //Actual columns
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ACCURACY = "accuracy";
        public static final String LOCATION_NAME = "locaion_name";
        public static final String FREQUENCY = "location_frequency";
        public static final String WITH = "company";
        public static final String RANGE = "range";
    }

    private static final Uri CONTENT_URI = Uri.parse("content://" + )


}