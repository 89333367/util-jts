package sunyu.util.test;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import sunyu.util.JtsUtil;

public class TestJtsUtil {
    Log log = LogFactory.get();
    JtsUtil jtsUtil = JtsUtil.INSTANCE.build();

    @Test
    public void 测试多边形面积() throws ParseException {
        String wkt = ResourceUtil.readUtf8Str("polygon.txt");
        log.debug(wkt);
        double area = jtsUtil.geometryArea(jtsUtil.getWktReader().read(wkt));
        log.debug("多边形面积：{} 平方米", area);
        //平方米换为亩，计算口诀为“加半左移三”
        //1平方米＝0.0015亩，如128平方米等于多少亩?计算方法是先用128加128的一半：128＋64＝192，再把小数点左移3位，即得出亩数为0.192
        log.debug("多边形面积：{} 亩", NumberUtil.div(area + NumberUtil.div(area, 2), 1000));
        log.debug("多边形面积：{} 亩", area / 666.67);
    }

    @Test
    public void 测试点是否在矩形内() {
        boolean out = jtsUtil.inRectangle(116.30699, 40.05496, 116.291323, 40.057832, 116.303828, 40.051536);
        Assert.isFalse(out);
        log.debug("是否在矩形内：{}", out);
        boolean in = jtsUtil.inRectangle(116.301456, 40.055623, 116.291323, 40.057832, 116.303828, 40.051536);
        Assert.isTrue(in);
        log.debug("是否在矩形内：{}", in);
    }


    @Test
    public void 测试点是否在圆内() {
        boolean out = jtsUtil.inCircle(116.311158, 40.055043, 116.302247, 40.05797, 427.25225281516754);
        Assert.isFalse(out);
        log.debug("是否在圆内：{}", out);
        boolean in = jtsUtil.inCircle(116.304474, 40.055319, 116.302247, 40.05797, 427.25225281516754);
        Assert.isTrue(in);
        log.debug("是否在圆内：{}", in);
    }

    @Test
    public void 测试点是否在多边形内() throws ParseException {
        Geometry polygon = jtsUtil.getWktReader().read("POLYGON((116.312496 40.05944,116.314333 40.059426,116.314284 40.058046,116.312465 40.058129,116.313826 40.05876,116.312496 40.05944))");
        Geometry outPoint = jtsUtil.getWktReader().read("POINT(116.313484 40.058736)");
        Assert.isFalse(jtsUtil.inGeometry((Point) outPoint, polygon));
        log.debug("是否在多边形内：{}", jtsUtil.inGeometry((Point) outPoint, polygon));
        Geometry inPoint = jtsUtil.getWktReader().read("POINT(116.31299 40.058187)");
        Assert.isTrue(jtsUtil.inGeometry((Point) inPoint, polygon));
        log.debug("是否在多边形内：{}", jtsUtil.inGeometry((Point) inPoint, polygon));
    }
}
