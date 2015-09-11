/*
 * MCMCOperator.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.inference.operation;

import beast.xml.Identifiable;

/**
 * An MCMC operator.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Arman Bilge
 */
public interface Operator extends Identifiable {

    String WEIGHT = "weight";

    /**
     * operates on the model.
     *
     * @return the log hastings ratio of this operator.
     * @throws OperatorFailedException if the operator failed and should be rejected
     */
    double operate() throws OperatorFailedException;

    /**
     * Called to tell operator that operation was accepted
     *
     * @param deviation the log ratio accepted on
     */
    void accept(double deviation);

    /**
     * Called to tell operator that operation was rejected
     */
    void reject();

    /**
     * Reset operator acceptance records.
     */
    void reset();

    /**
     * @return the total number of operations since last call to reset().
     */
    int getCount();

    /**
     * @return the number of acceptances since last call to reset().
     */
    int getAcceptCount();

    /**
     * Set the number of acceptances since last call to reset(). This is used
     * to restore the state of the operator
     *
     * @param acceptCount number of acceptances
     */
    void setAcceptCount(int acceptCount);

    /**
     * @return the number of rejections since last call to reset().
     */
    int getRejectCount();

    /**
     * Set the number of rejections since last call to reset(). This is used
     * to restore the state of the operator
     *
     * @param rejectCount number of rejections
     */
    void setRejectCount(int rejectCount);

    /**
     * @return the mean deviation in log posterior per accepted operations.
     */
    double getMeanDeviation();

    double getSumDeviation();

    void setSumDeviation(double sumDeviation);

    /**
     * @return the optimal acceptance probability
     */
    double getTargetAcceptanceProbability();

    /**
     * @return the minimum acceptable acceptance probability
     */
    double getMinimumAcceptanceLevel();

    /**
     * @return the maximum acceptable acceptance probability
     */
    double getMaximumAcceptanceLevel();

    /**
     * @return the minimum good acceptance probability
     */
    double getMinimumGoodAcceptanceLevel();

    /**
     * @return the maximum good acceptance probability
     */
    double getMaximumGoodAcceptanceLevel();

    /**
     * @return a short descriptive message of the performance of this operator.
     */
    String getPerformanceSuggestion();

    /**
     * @return the relative weight of this operator.
     */
    double getWeight();

    /**
     * sets the weight of this operator. The weight
     * determines the proportion of time spent using
     * this operator. This is relative to a 'standard'
     * operator weight of 1.
     *
     * @param weight the relative weight of this parameter - should be positive.
     */
    void setWeight(double weight);

    /**
     * @return the name of this operator
     */
    String getName();

    double getMeanEvaluationTime();

    void addEvaluationTime(long time);

    long getTotalEvaluationTime();

    default int getOperationCount() {
        return getAcceptCount() + getRejectCount();
    }

    default double getAcceptanceProbability() {
        return getAcceptCount() / (double) getOperationCount();
    }

}