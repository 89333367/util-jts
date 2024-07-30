package sunyu.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.Closeable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 坐标转换工具类
 *
 * @author 孙宇
 * <p>
 * WGS84，GCJ02， BD09坐标转换
 * <p>
 * WGS84坐标系：即地球坐标系，国际上通用的坐标系。设备一般包含GPS芯片或者北斗芯片获取的经纬度为WGS84地理坐标系,谷歌地图采用的是WGS84地理坐标系（中国范围除外）;
 * <p>
 * GCJ02坐标系：即火星坐标系，是由中国国家测绘局制订的地理信息系统的坐标系统。由WGS84坐标系经加密后的坐标系。
 * <p>
 * 谷歌中国地图和搜搜中国地图采用的是GCJ02地理坐标系; BD09坐标系：即百度坐标系，GCJ02坐标系经加密后的坐标系;
 * <p>
 * 搜狗坐标系、图吧坐标系等，估计也是在GCJ02基础上加密而成的。
 */
public enum CoordTransformUtil implements Serializable, Closeable {
    INSTANCE;
    private Log log = LogFactory.get();

    /**
     * 获得工具类工厂
     *
     * @return
     */
    public static CoordTransformUtil builder() {
        return INSTANCE;
    }

    /**
     * 构建工具类
     *
     * @return
     */
    public CoordTransformUtil build() {
        return INSTANCE;
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
    }

    private double AXIS = 6378245.0;
    private double OFFSET = 0.00669342162296594323; // (a^2 - b^2) / a^2
    private double X_PI = Math.PI * 3000.0 / 180.0;

    private double[] delta(double lat, double lon) {
        double[] latlng = new double[2];
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - OFFSET * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((AXIS * (1 - OFFSET)) / (magic * sqrtMagic) * Math.PI);
        dLon = (dLon * 180.0) / (AXIS / sqrtMagic * Math.cos(radLat) * Math.PI);
        latlng[0] = dLat;
        latlng[1] = dLon;
        return latlng;
    }

    /**
     * 是否在中国外
     *
     * @param lat 经度
     * @param lon 纬度
     * @return
     */
    public boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * GCJ-02 -> BD09 火星坐标系 -> 百度坐标系
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] gcj2BD09(double lat, double lon) {
        double x = lon;
        double y = lat;
        double[] latlon = new double[2];
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);
        latlon[0] = z * Math.sin(theta) + 0.006;
        latlon[1] = z * Math.cos(theta) + 0.0065;
        return latlon;
    }

    /**
     * GCJ-02 -> BD09 火星坐标系 -> 百度坐标系
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] gcj2BD09(double[] latAndLon) {
        return gcj2BD09(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 GCJ-02 -> BD09 火星坐标系 -> 百度坐标系
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> gcj2BD09List(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(gcj2BD09(point));
        }
        return result;
    }

    /**
     * BD09 -> GCJ-02 百度坐标系 -> 火星坐标系
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] bd092GCJ(double lat, double lon) {
        double x = lon - 0.0065;
        double y = lat - 0.006;
        double[] latlon = new double[2];
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        latlon[0] = z * Math.sin(theta);
        latlon[1] = z * Math.cos(theta);
        return latlon;
    }

    /**
     * BD09 -> GCJ-02 百度坐标系 -> 火星坐标系
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] bd092GCJ(double[] latAndLon) {
        return bd092GCJ(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 BD09 -> GCJ-02 百度坐标系 -> 火星坐标系
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> bd092GCJList(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(bd092GCJ(point));
        }
        return result;
    }

    /**
     * BD09 -> WGS84 百度坐标系 -> 地球坐标系
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] bd092WGS(double lat, double lon) {
        double[] latlon = bd092GCJ(lat, lon);
        return gcj2WGS(latlon[0], latlon[1]);
    }

    /**
     * BD09 -> WGS84 百度坐标系 -> 地球坐标系
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] bd092WGS(double[] latAndLon) {
        return bd092WGS(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 BD09 -> WGS84 百度坐标系 -> 地球坐标系
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> bd092WGSList(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(bd092WGS(point));
        }
        return result;
    }

    /**
     * WGS84 -> BD09 地球坐标系 -> 百度坐标系
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] wgs2BD09(double lat, double lon) {
        double[] latlon = wgs2GCJ(lat, lon);
        return gcj2BD09(latlon[0], latlon[1]);
    }

    /**
     * WGS84 -> BD09 地球坐标系 -> 百度坐标系
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] wgs2BD09(double[] latAndLon) {
        return wgs2BD09(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 WGS84 -> BD09 地球坐标系 -> 百度坐标系
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> wgs2BD09List(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(wgs2BD09(point));
        }
        return result;
    }

    /**
     * WGS84 -> GCJ02 地球坐标系 -> 火星坐标系
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] wgs2GCJ(double lat, double lon) {
        double[] latlon = new double[2];
        if (outOfChina(lat, lon)) {
            latlon[0] = lat;
            latlon[1] = lon;
            return latlon;
        }
        double[] deltaD = delta(lat, lon);
        latlon[0] = lat + deltaD[0];
        latlon[1] = lon + deltaD[1];
        return latlon;
    }

    /**
     * WGS84 -> GCJ02 地球坐标系 -> 火星坐标系
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] wgs2GCJ(double[] latAndLon) {
        return wgs2GCJ(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 WGS84 -> GCJ02 地球坐标系 -> 火星坐标系
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> wgs2GCJList(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(wgs2GCJ(point));
        }
        return result;
    }

    /**
     * GCJ02 -> WGS84 火星坐标系 -> 地球坐标系(粗略)
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon(纬度在前)
     */
    public double[] gcj2WGS(double lat, double lon) {
        double[] latlon = new double[2];
        if (outOfChina(lat, lon)) {
            latlon[0] = lat;
            latlon[1] = lon;
            return latlon;
        }
        double[] deltaD = delta(lat, lon);
        latlon[0] = lat - deltaD[0];
        latlon[1] = lon - deltaD[1];
        return latlon;
    }

    /**
     * GCJ02 -> WGS84 火星坐标系 -> 地球坐标系(粗略)
     *
     * @param latAndLon [纬度，经度]
     * @return latLon(纬度在前)
     */
    public double[] gcj2WGS(double[] latAndLon) {
        return gcj2WGS(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 GCJ02 -> WGS84 火星坐标系 -> 地球坐标系(粗略)
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> gcj2WGSList(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(gcj2WGS(point));
        }
        return result;
    }

    /**
     * GCJ02 -> WGS84 火星坐标系 -> 地球坐标系（精确）
     *
     * @param lat 纬度
     * @param lon 经度
     * @return latLon ( 纬度在前 )
     */
    public double[] gcj2WGSExactly(double lat, double lon) {
        double initDelta = 0.01;
        double threshold = 0.000000001;
        double dLat = initDelta, dLon = initDelta;
        double mLat = lat - dLat, mLon = lon - dLon;
        double pLat = lat + dLat, pLon = lon + dLon;
        double wgsLat, wgsLon, i = 0;
        while (true) {
            wgsLat = (mLat + pLat) / 2;
            wgsLon = (mLon + pLon) / 2;
            double[] tmp = wgs2GCJ(wgsLat, wgsLon);
            dLat = tmp[0] - lat;
            dLon = tmp[1] - lon;
            if ((Math.abs(dLat) < threshold) && (Math.abs(dLon) < threshold))
                break;

            if (dLat > 0)
                pLat = wgsLat;
            else
                mLat = wgsLat;
            if (dLon > 0)
                pLon = wgsLon;
            else
                mLon = wgsLon;

            if (++i > 10000)
                break;
        }
        double[] latlon = new double[2];
        latlon[0] = wgsLat;
        latlon[1] = wgsLon;
        return latlon;
    }

    /**
     * GCJ02 -> WGS84 火星坐标系 -> 地球坐标系（精确）
     *
     * @param latAndLon [纬度，经度]
     * @return latLon ( 纬度在前 )
     */
    public double[] gcj2WGSExactly(double[] latAndLon) {
        return gcj2WGSExactly(latAndLon[0], latAndLon[1]);
    }

    /**
     * 转换整个集合内的点 GCJ02 -> WGS84 火星坐标系 -> 地球坐标系（精确）
     *
     * @param points List<[纬度，经度]>
     * @return List<latLon ( 纬度在前 )>
     */
    public List<double[]> gcj2WGSExactlyList(List<double[]> points) {
        ArrayList<double[]> result = new ArrayList<>();
        for (double[] point : points) {
            result.add(gcj2WGSExactly(point));
        }
        return result;
    }

}