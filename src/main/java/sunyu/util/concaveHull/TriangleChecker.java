/**
 * Interface for checkers to determine if a boundary edge should be removed
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

/**
 * {@link https://github.com/OrdnanceSurvey/OS_ConcaveHull}
 *
 * @Author WeiJinglun
 * @Date 2019.03.26
 */
public interface TriangleChecker {
    /**
     * @param coordS starting point of a boundary edge (CCW order)
     * @param coordE ending point of a boundary edge
     * @param coordO internal point of triangle s-e-o that is being examined
     * @return true if this triangle can be dug according predefined criteria; false if not
     */
    boolean removeable(Coordinate coordS, Coordinate coordE, Coordinate coordO);
}
