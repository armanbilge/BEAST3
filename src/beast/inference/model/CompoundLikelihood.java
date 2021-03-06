/*
 * CompoundLikelihood.java
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

package beast.inference.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A likelihood function which is simply the product of a set of likelihood functions.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Arman Bilge
 */
public final class CompoundLikelihood extends Likelihood {

    private final int threadCount;
    private final boolean unroll;

    private final ExecutorService pool;

    private final ArrayList<Likelihood> likelihoods = new ArrayList<>();

    private final ArrayList<Likelihood> earlyLikelihoods = new ArrayList<>();
    private final ArrayList<Likelihood> lateLikelihoods = new ArrayList<>();

    private final List<Callable<Double>> likelihoodCallers = new ArrayList<>();

    public CompoundLikelihood(final boolean unroll, final int threads, final Likelihood... likelihoods) {

        super(likelihoods[0].getModel(), Arrays.stream(likelihoods).map(Likelihood::getModel).skip(1).toArray(Model[]::new));

        this.unroll = unroll;
        Arrays.stream(likelihoods).forEach(this::addLikelihood);

        if (threads < 0 && this.likelihoods.size() > 1)
            // asking for an automatic threadpool size and there is more than one likelihood to compute
            threadCount = this.likelihoods.size();
        else if (threads > 0)
            threadCount = threads;
        else // no thread pool requested or only one likelihood
            threadCount = 0;

        if (threadCount > 0)
            pool = Executors.newFixedThreadPool(threadCount, r -> {
                final Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true); // Use daemon threads to prevent hangup on program termination
                return t;
            });
        else
            pool = null;
    }

    public CompoundLikelihood(final int threads, final Likelihood... likelihoods) {
        this(true, threads, likelihoods);
    }

    public CompoundLikelihood(final Likelihood... likelihoods) {
        this(false, 0, likelihoods);
    }

    private void addLikelihood(final Likelihood likelihood) {

        // unroll any compound likelihoods
        if (unroll && likelihood instanceof CompoundLikelihood) {
            ((CompoundLikelihood) likelihood).getLikelihoods().forEach(this::addLikelihood);
        } else {
            if (!likelihoods.contains(likelihood)) {
                likelihoods.add(likelihood);
                if (likelihood.evaluateEarly()) {
                    earlyLikelihoods.add(likelihood);
                } else {
                    // late likelihood list is used to evaluate them if the thread pool is not being used...
                    lateLikelihoods.add(likelihood);
                    if (pool != null)
                        likelihoodCallers.add(likelihood::getLogLikelihood);
                }
            }
        }
    }

    public List<Likelihood> getLikelihoods() {
        return Collections.unmodifiableList(likelihoods);
    }

    @Override
    public double calculateLogLikelihood() {

        double logLikelihood = evaluateLikelihoods(earlyLikelihoods);

        if (logLikelihood == Double.NEGATIVE_INFINITY)
            return Double.NEGATIVE_INFINITY;

        if (pool == null) // Single threaded
            logLikelihood += evaluateLikelihoods(lateLikelihoods);
        else
            logLikelihood += evaluateCallers(likelihoodCallers);

        return logLikelihood;
    }

    @Override
    protected void calculateGradient(final Gradient gradient, final double chain) {
        if (pool == null) { // Single threaded
            likelihoods.forEach(l -> l.calculateGradient(gradient, chain));
        } else {
            likelihoods.forEach(l -> pool.submit(() -> l.calculateGradient(gradient, chain)));
            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (final InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static double evaluateLikelihoods(final List<Likelihood> likelihoods) {
        double logLikelihood = 0.0;
        for (Likelihood likelihood : likelihoods) {
            final double l = likelihood.getLogLikelihood();
            if (l == Double.NEGATIVE_INFINITY) // if likelihood is zero then short circuit the rest
                return Double.NEGATIVE_INFINITY;
            logLikelihood += l;
        }
        return logLikelihood;
    }

    private double evaluateCallers(final List<? extends Callable<Double>> callers) {
        try {
            return pool.invokeAll(callers).stream().mapToDouble(f -> {
                try {
                    return f.get();
                } catch (final InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }).sum();
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void makeLikelihoodDirty() {
        likelihoods.forEach(Likelihood::makeDirty);
    }

    @Override
    public boolean evaluateEarly() {
        return !earlyLikelihoods.isEmpty();
    }

    @Override
    protected void storeCalculations() {
        // Nothing to do
    }

    @Override
    protected void restoreCalculations() {
        // Nothing to do
    }

}
