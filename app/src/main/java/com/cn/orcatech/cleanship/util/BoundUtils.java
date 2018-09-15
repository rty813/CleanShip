package com.cn.orcatech.cleanship.util;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.Polyline;

import java.util.List;

public class BoundUtils {
    private static double cross(LatLng A, LatLng B, LatLng C) {
        double cross1 = (C.latitude - A.latitude) * (B.longitude - A.longitude);
        double cross2 = (C.longitude - A.longitude) * (B.latitude - A.latitude);
        return cross1 - cross2;
    }

    private static boolean rectsIntersect(LatLng S1, LatLng E1, LatLng S2, LatLng E2) {
        return Math.min(S1.longitude, E1.longitude) <= Math.max(S2.longitude, E2.longitude) &&
                Math.max(S1.longitude, E1.longitude) >= Math.min(S2.longitude, E2.longitude) &&
                Math.min(S1.latitude, E1.latitude) <= Math.max(S2.latitude, E2.latitude) &&
                Math.max(S1.latitude, E1.latitude) >= Math.min(S2.latitude, E2.latitude);
    }

    private static boolean intersect(LatLng A1, LatLng A2, LatLng B1, LatLng B2) {
        double T1 = cross(A1, A2, B1);
        double T2 = cross(A1, A2, B2);
        double T3 = cross(B1, B2, A1);
        double T4 = cross(B1, B2, A2);
        if (((T1 * T2) > 0) || ((T3 * T4) > 0)) {    // 一条线段的两个端点在另一条线段的同侧，不相交。（可能需要额外处理以防止乘法溢出，视具体情况而定。）
            return false;
        } else if (T1 == 0 && T2 == 0) {             // 两条线段共线，利用快速排斥实验进一步判断。此时必有 T3 == 0 && T4 == 0。
            return rectsIntersect(A1, A2, B1, B2);
        } else {                                    // 其它情况，两条线段相交。
            return true;
        }
    }

    public static boolean detectIntersect(Polyline polyline, Polygon polygon) {
        LatLng a = polyline.getPoints().get(0);
        LatLng b = polyline.getPoints().get(1);
        List<LatLng> LatLngs = polygon.getPoints();
        for (int i = 0; i < LatLngs.size() - 1; i++) {
            if (intersect(a, b, LatLngs.get(i), LatLngs.get(i + 1))) {
                return true;
            }
        }
        return intersect(a, b, LatLngs.get(0), LatLngs.get(LatLngs.size() - 1));
    }
}
