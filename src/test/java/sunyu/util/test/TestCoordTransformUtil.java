package sunyu.util.test;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.jupiter.api.Test;
import sunyu.util.CoordTransformUtil;

public class TestCoordTransformUtil {
    Log log = LogFactory.get();

    @Test
    void t001() {
        CoordTransformUtil coordTransformUtil = CoordTransformUtil.INSTANCE.build();
        log.debug("{}", coordTransformUtil.outOfChina(39.178762, 118.546783));
    }
}
