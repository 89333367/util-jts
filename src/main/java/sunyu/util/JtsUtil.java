package sunyu.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.buffer.BufferOp;
import sunyu.util.concaveHull.ConcaveHullJTS;
import sunyu.util.concaveHull.TriCheckerAlpha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * JTS工具类
 *
 * @author 孙宇
 * <p>
 * WKT格式
 * <p>
 * POINT(6 10)
 * <p>
 * LINESTRING(3 4,10 50,20 25)
 * <p>
 * POLYGON((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2))
 * <p>
 * MULTIPOINT(3.5 5.6, 4.8 10.5)
 * <p>
 * MULTILINESTRING((3 4,10 50,20 25),(-5 -8,-10 -8,-15 -4))
 * <p>
 * MULTIPOLYGON(((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((6 3,9 2,9 4,6 3)))
 * <p>
 * GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))
 * <p>
 * POINT ZM (1 1 5 60)
 * <p>
 * POINT M (1 1 80)
 * <p>
 * POINT EMPTY
 * <p>
 * MULTIPOLYGON EMPTY
 */
public class JtsUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    public static Builder builder() {
        return new Builder();
    }

    private JtsUtil(Config config) {
        log.info("[构建JtsUtil] 开始");
        log.info("[构建JtsUtil] 结束");
        this.config = config;
    }

    private static class Config {
        private final String POLYGON = "Polygon";
        private final GeometryFactory geometryFactory = new GeometryFactory();
        private final WKTReader wktReader = new WKTReader(geometryFactory);
    }

    public static class Builder {
        private final Config config = new Config();

        public JtsUtil build() {
            return new JtsUtil(config);
        }
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        log.info("[销毁JtsUtil] 开始");
        log.info("[销毁JtsUtil] 结束");
    }

    /**
     * 由WGS84坐标转换高斯投影坐标
     * 6度带宽
     *
     * @param coordinate 点(经度，纬度)
     * @return [x, y]
     */
    private Coordinate wgs84ToGauss(Coordinate coordinate) {
        double longitude = coordinate.getX();
        double latitude = coordinate.getY();
        int ProjNo;//投影带号
        int ZoneWide;//带宽
        double longitude1, latitude1, longitude0, latitude0, X0, Y0, xval, yval;
        double a, f, e2, ee, NN, T, C, A, M, iPI;
        iPI = 0.0174532925199433;               //3.1415926535898/180.0;
        ZoneWide = 6;                           //6度带宽
        a = 6378137.0;
        f = 1.0 / 298.257223563;//WGS84
        ProjNo = (int) (longitude / ZoneWide);
        longitude0 = ProjNo * ZoneWide + ZoneWide / 2;
        longitude0 = longitude0 * iPI;
        longitude1 = longitude * iPI; //经度转换为弧度
        latitude1 = latitude * iPI; //纬度转换为弧度
        e2 = 2 * f - f * f;
        ee = e2 * (1.0 - e2);
        NN = a / Math.sqrt(1.0 - e2 * Math.sin(latitude1) * Math.sin(latitude1));
        T = Math.tan(latitude1) * Math.tan(latitude1);
        C = ee * Math.cos(latitude1) * Math.cos(latitude1);
        A = (longitude1 - longitude0) * Math.cos(latitude1);
        M = a * ((1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * latitude1 - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2 * e2 / 1024) * Math.sin(2 * latitude1) + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * Math.sin(4 * latitude1) - (35 * e2 * e2 * e2 / 3072) * Math.sin(6 * latitude1));
        xval = NN * (A + (1 - T + C) * A * A * A / 6 + (5 - 18 * T + T * T + 72 * C - 58 * ee) * A * A * A * A * A / 120);
        yval = M + NN * Math.tan(latitude1) * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24 + (61 - 58 * T + T * T + 600 * C - 330 * ee) * A * A * A * A * A * A / 720);
        X0 = 1000000L * (ProjNo + 1) + 500000L;
        Y0 = 0;
        return new Coordinate(xval + X0, yval + Y0);
    }

    /**
     * WKT读取器
     *
     * @return WKT读取器
     */
    public WKTReader getWktReader() {
        return config.wktReader;
    }

    /**
     * 判断两个几何图形是否相等
     * <p>
     * 几何形状拓扑上相等
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean equals(Geometry geometry1, Geometry geometry2) {
        return geometry1.equals(geometry2);
    }

    /**
     * 判断两个几何图形是否脱节
     * <p>
     * 几何形状没有共有的点
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean disjoint(Geometry geometry1, Geometry geometry2) {
        return geometry1.disjoint(geometry2);
    }

    /**
     * 判断两个几何图形相交
     * <p>
     * 几何形状至少有一个共有点（区别于脱节）
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean intersects(Geometry geometry1, Geometry geometry2) {
        return geometry1.intersects(geometry2);
    }

    /**
     * 判断两个几何图形是否接触
     * <p>
     * 几何形状有至少一个公共的边界点，但是没有内部点
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean touches(Geometry geometry1, Geometry geometry2) {
        return geometry1.touches(geometry2);
    }

    /**
     * 判断两个几何图形是否交叉
     * <p>
     * 几何形状共享一些但不是所有的内部点
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean crosses(Geometry geometry1, Geometry geometry2) {
        return geometry1.crosses(geometry2);
    }

    /**
     * 几何形状1的线都在几何形状2内部
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean within(Geometry geometry1, Geometry geometry2) {
        return geometry1.within(geometry2);
    }

    /**
     * 判断几何图形1是否包含几何图形2
     * <p>
     * 几何形状2的线都在几何形状1内部（区别于内含）
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean contains(Geometry geometry1, Geometry geometry2) {
        return geometry1.contains(geometry2);
    }

    /**
     * 几何图形1和几何图形2是否重叠
     * <p>
     * 几何形状共享一部分但不是所有的公共点，而且相交处有他们自己相同的区域
     *
     * @param geometry1
     * @param geometry2
     * @return
     */
    public boolean overlaps(Geometry geometry1, Geometry geometry2) {
        return geometry1.overlaps(geometry2);
    }

    /**
     * 将WKT字符串转换为google格式字符串[[lat,lon],[lat,lon],……]
     * 目前仅保留多边形的外轮廓
     * 公司内部使用
     */
    public String wktToGoogleStr(Polygon polygon) {
        StringBuilder sb = new StringBuilder();
        LineString exteriorRing = polygon.getExteriorRing();//多边形外轮廓
        String text = exteriorRing.toText();
        text = text.substring("LINEARRING (".length(), text.lastIndexOf(")"));
        String[] split = text.split(",");
        for (String s : split) {
            String[] split1 = s.trim().split(" ");
            sb.append('[').append(split1[1]).append(',').append(split1[0]).append("],");
        }
        String substring = sb.substring(0, sb.lastIndexOf(","));
        if (substring.isEmpty()) {
            return null;
        }
        return '[' + substring + ']';
    }

    /**
     * 将google字符串转换为WKT字符串
     */
    public String googleStrToWkt(String googleStr) {
        StringBuilder sb = new StringBuilder();
        String[] split = googleStr.substring("[[".length(), googleStr.lastIndexOf("]]")).split("\\]\\,\\[");
        if (split.length > 3) {
            for (String s : split) {
                String[] split1 = s.split(",");
                sb.append(split1[1]).append(" ").append(split1[0]).append(",");
            }
            String r = sb.substring(0, sb.lastIndexOf(","));
            if (r.isEmpty()) {
                return null;
            }
            return "POLYGON ((" + r + "))";
        }
        return null;
    }

    /**
     * 离散点构建凹多边形
     *
     * @param multipPoint 离散点集
     * @return 凹壳多边形
     */
    public Polygon polygon(MultiPoint multipPoint) throws InterruptedException, ExecutionException, TimeoutException {
        return polygon(multipPoint, Double.MAX_VALUE);
    }

    /**
     * 离散点构建凹多边形，农机专用
     *
     * @param multiPoint 离散点集
     * @param area       实际作业面积 单位：平方米 切记不是亩！！
     * @param timeOut    计算轮廓超时时间，默认为5秒
     * @return 返回值为[wktStr, googleStr]
     */
    public String[] polygonFarm(MultiPoint multiPoint, Double area, Integer timeOut) {
        String[] result = new String[2];
        //开始计算凹壳
        ConcaveHullJTS cah = new ConcaveHullJTS(multiPoint);
        //先检测面积推断检测半径
        TriCheckerAlpha checkerAlpha;
        if (area > 0 && area < 666) { //阈值设置为1亩
            checkerAlpha = new TriCheckerAlpha(0.00001);
        } else { //非法参数或者大于1亩都设半径为0.0001
            checkerAlpha = new TriCheckerAlpha(0.0001);
        }
        Collection<Geometry> hulls = cah.getConcaveHullBFS(checkerAlpha, true, false, timeOut);
        if (CollUtil.isNotEmpty(hulls)) {
            double sum = 0;
            for (Geometry hull : hulls) {
                sum += hull.getArea();
            }
            //去除面积较小的5%
            List<Geometry> newhulls = new ArrayList<>();
            for (Geometry hull : hulls) {
                if (hull.getArea() >= sum * 0.05) {
                    newhulls.add(hull);
                }
            }
            hulls.clear();
            hulls.addAll(newhulls);
            GeometryCollection geometryCollection = config.geometryFactory.createGeometryCollection(hulls.toArray(new Geometry[hulls.size()]));
            Geometry resultGeometry = geometryCollection.buffer(0);//返回集合对象
            if (resultGeometry.isEmpty()) {
                return result;
            }
            //返回的WKT字符串
            result[0] = resultGeometry.toText();
            if (newhulls.size() > 1) {
                //构造连通多边形之间的管道
                Coordinate coordinatePre = newhulls.get(0).getCoordinate();
                for (int j = 1; j < newhulls.size(); j++) {
                    Coordinate coordinate = newhulls.get(j).getCoordinate();
                    Coordinate[] coordinates = {coordinate, coordinatePre};
                    LineString lineString = config.geometryFactory.createLineString(coordinates);
                    BufferOp bufferOp = new BufferOp(lineString);
                    bufferOp.setEndCapStyle(BufferOp.CAP_ROUND);
                    Geometry line = bufferOp.getResultGeometry(0.0000001);//用于连通的多边形
                    hulls.add(line);
                    coordinatePre = coordinate;
                }
                //合并管道和多边形
                GeometryCollection collection = config.geometryFactory.createGeometryCollection(hulls.toArray(new Geometry[hulls.size()]));
                Geometry buffer = collection.buffer(0);
                if (config.POLYGON.equals(buffer.getGeometryType())) {
                    result[1] = wktToGoogleStr((Polygon) buffer);
                }
            } else if (hulls.size() == 1) { //过滤完结果只剩一个多边形
                for (Geometry hull : hulls) {
                    if (config.POLYGON.equals(hull.getGeometryType())) {
                        result[1] = wktToGoogleStr((Polygon) hull);
                    } else {
                        log.error("凹壳转换多边形异常");
                    }
                }
            } else { //数据异常过滤完之后没有多边形
                log.error("多边形过滤数据异常数据信息有误");
            }
        }
        return result;
    }

    /**
     * 根据点集计算凹壳 根据返回的多边形面积和作业是上传的作业面积对比 判断依据凹壳面积和作业面积1.5倍比较
     * <p>
     * 如果5秒中都没有计算出来，就直接抛超时异常
     *
     * @param multipPoint 离散点集
     * @param area        实际作业面积 单位：平方米 切记不是亩！！
     * @return 凹壳多边形
     */
    public Polygon polygon(MultiPoint multipPoint, Double area) throws InterruptedException, ExecutionException, TimeoutException {
        ConcaveHullJTS cah = new ConcaveHullJTS(multipPoint);
        //开始计算凹壳
        //先检测面积推断检测半径
        TriCheckerAlpha checkerAlpha; //检测半径
        if (area > 0 && area < 666) { //阈值设置为1亩
            checkerAlpha = new TriCheckerAlpha(0.00001);
        } else { //非法参数或者大于1亩都设半径为0.0001
            checkerAlpha = new TriCheckerAlpha(0.0001);
        }
        Collection<Geometry> hulls = cah.getConcaveHullBFS(checkerAlpha, false, false, null);
        //结果只会有一个简单多边形 不会含有洞的情况
        //返回值
        Polygon result = null;
        if (CollUtil.isNotEmpty(hulls) && hulls.size() == 1) {
            for (Geometry hull : hulls) {
                if (config.POLYGON.equals(hull.getGeometryType())) {
                    result = (Polygon) hull;
                } else {
                    log.error("凹壳转换多边形异常");
                }
            }
        }
        return result;
    }

    /**
     * 点是否在矩形区域内
     *
     * @param pointLat 点纬度
     * @param pointLon 点经度
     * @param leftLon  左上角经度
     * @param leftLat  左上角纬度
     * @param rightLon 右下角经度
     * @param rightLat 右下角纬度
     * @return
     */
    public boolean inRectangle(double pointLon, double pointLat, double leftLon, double leftLat, double rightLon, double rightLat) {
        return new Envelope(new Coordinate(leftLon, leftLat), new Coordinate(rightLon, rightLat)).contains(pointLon, pointLat);
    }

    /**
     * 判断点是否在圆内
     *
     * @param pointLon        点经度
     * @param pointLat        点纬度
     * @param centerCircleLon 圆心经度
     * @param centerCircleLat 圆心纬度
     * @param radius          圆半径(米)
     * @return
     */
    public boolean inCircle(double pointLon, double pointLat, double centerCircleLon, double centerCircleLat, double radius) {
        return pointDistance(pointLon, pointLat, centerCircleLon, centerCircleLat) < radius;
    }

    /**
     * 点是否在几何图形内
     *
     * @param point
     * @param geometry
     * @return
     */
    public boolean inGeometry(Point point, Geometry geometry) {
        return geometry.contains(point);
    }

    /**
     * 计算球面中点与点的距离
     *
     * @param lon1 点1经度
     * @param lat1 点1纬度
     * @param lon2 点2经度
     * @param lat2 点2纬度
     * @return
     */
    public double pointDistance(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * 6378137;// 地球半径
        s = round(s * 10000) / 10000;
        return s;
    }

    /**
     * 计算任意几何图形面积
     *
     * @param geometry
     * @return
     */
    public double geometryArea(Geometry geometry) throws ParseException {
        String geometryType = geometry.getGeometryType().toUpperCase();
        double area = 0.0;
        int geometriesNum;
        switch (geometryType) { //先判断类型
            case "POLYGON": //多边形 直接转换类型计算
                area = polygonArea((Polygon) geometry);
                break;
            case "MULTIPOLYGON": //多多边形 遍历 然后计算
                geometriesNum = geometry.getNumGeometries();//多边形数量
                for (int i = 0; i < geometriesNum; i++) { //遍历叠加
                    Geometry geometryN = geometry.getGeometryN(i);
                    area += polygonArea((Polygon) geometryN);
                }
                break;
            case "GEOMETRYCOLLECTION": //先遍历 判断类型
                geometriesNum = geometry.getNumGeometries();//内部几何图形数量
                for (int i = 0; i < geometriesNum; i++) { //遍历叠加
                    Geometry geometryN = geometry.getGeometryN(i);
                    area += geometryArea(geometryN); //递归计算
                }
                break;
            default: //其他类型不做面积计算
                area = 0.0;
                break;
        }
        return area;
    }

    /**
     * 复杂多边形面积 会含有洞的情况
     *
     * @param polygon
     * @return
     */
    private double polygonArea(Polygon polygon) throws ParseException {
        double interiorArea = 0.0; //内部洞的面积
        double totalArea = 0.0;//总面积
        LineString interiorRingN = null;//内部洞的多边形的边
        LineString exteriorRing = polygon.getExteriorRing(); //最外层的多边形的边
        int interiorRingNum = polygon.getNumInteriorRing(); //内层洞的数量
        if (interiorRingNum > 0) { //判断是否含有洞的情况
            for (int i = 0; i < interiorRingNum; i++) {
                interiorRingN = polygon.getInteriorRingN(i);//获得洞的多边形的边
                interiorArea += polygonAreaFromLineString(interiorRingN);
            }
            totalArea += polygonAreaFromLineString(exteriorRing);
            totalArea -= interiorArea;
            if (totalArea <= 0) { //检测最后多边形面积是否有异常
                throw new ParseException("多边形面积计算异常：" + polygon.toText());
            } else {
                return totalArea;
            }
        } else { //如果没有洞 按照简单多边形计算
            return polygonAreaFromLineString(polygon.getExteriorRing());
        }
    }

    /**
     * 计算多边形面积 输入多边形轮廓
     * 返回值 平方米
     */
    private double polygonAreaFromLineString(LineString lineString) {
        Coordinate[] coordinates = lineString.getCoordinates();
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = wgs84ToGauss(coordinates[i]);
        }
        return Math.abs(Area.ofRing(coordinates));
    }

    private double rad(double d) {
        return d * Math.PI / 180.0;
    }

    private double round(double d) {
        return Math.floor(d + 0.5);
    }

}