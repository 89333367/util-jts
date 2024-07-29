/**
 * For concave hull construction based on Alpha shape criterion (but not exactly following the definition)
 * Original paper: Edelsbrunner, Herbert; Kirkpatrick, David G.; Seidel, Raimund (1983), "On the shape of a set of points in the plane",
 * IEEE Transactions on Information Theory, 29 (4): 551�559, doi:10.1109/TIT.1983.1056714
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
 */
package sunyu.util.concaveHull;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Triangle;

/**
 * {@link https://github.com/OrdnanceSurvey/OS_ConcaveHull}
 *
 * @Author WeiJinglun
 * @Date 2019.03.26
 */
public class TriCheckerAlpha implements TriangleChecker {
    double radius = Double.MAX_VALUE;

    public TriCheckerAlpha(double r) {
        radius = r;
    }

    public double getR() {
        return radius;
    }

    public void setR(double r) {
        radius = r;
    }

    //
    public boolean removeable(Coordinate coordS, Coordinate coordE, Coordinate coordO) {
        Coordinate cen = Triangle.circumcentre(coordS, coordE, coordO);
        double r = cen.distance(coordS);
        return r > radius;
    }

}
