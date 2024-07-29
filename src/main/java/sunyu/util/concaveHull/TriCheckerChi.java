/**
 * For concave hull construction based on Chi criterion
 * <p>
 * Original paper: Matt Duckham et al 2008 Efficient generation of simple polygons for characterizing the shape of a set of points in the plane
 * <p>
 * Author: Sheng Zhou (Sheng.Zhou@os.uk)
 * <p>
 * version 0.4
 * <p>
 * Date: 2019-01-31
 * <p>
 * Copyright (C) 2019 Ordnance Survey
 * <p>
 * Licensed under the Open Government Licence v3.0 (the "License");
 * <p>
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * <p>
 * http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *
 */
package sunyu.util.concaveHull;

import org.locationtech.jts.geom.Coordinate;

/**
 * {@link https://github.com/OrdnanceSurvey/OS_ConcaveHull}
 *
 * @Author WeiJinglun
 * @Date 2019.03.26
 */
public class TriCheckerChi implements TriangleChecker {
    double length = Double.MAX_VALUE;
    Coordinate sp = new Coordinate();
    Coordinate ep = new Coordinate();

    public TriCheckerChi(double L) {
        length = L;
    }

    public double getLength() {
        return length;
    }

    public boolean removeable(Coordinate coordS, Coordinate coordE, Coordinate coordO) {
        sp.setCoordinate(coordS);
        ep.setCoordinate(coordE);
        return sp.distance(ep) > length;
    }

}
