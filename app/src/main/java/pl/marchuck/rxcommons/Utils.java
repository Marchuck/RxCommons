package pl.marchuck.rxcommons;

import android.os.Build;

/**
 * Project "RxCommons"
 * <p>
 * Created by Lukasz Marczak
 * on 18.04.2017.
 */

public class Utils {
    public static boolean isAtLeastLollipop() {
        return Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }
}
