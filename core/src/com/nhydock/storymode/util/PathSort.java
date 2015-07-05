package com.nhydock.storymode.util;

import java.util.Comparator;

public class PathSort implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (o1 == null && o2 == null)
            return 0;
        if (o1 == null)
            return 1;
        if (o2 == null)
            return -1;

        boolean aDir = o1.endsWith("/");
        boolean bDir = o2.endsWith("/");

        if (aDir && !bDir) {
            return -1;
        }
        if (!aDir && bDir) {
            return 1;
        }

        return o1.toLowerCase().compareTo(o2.toLowerCase());
    }

}
