/**
 * Copyright (c) 2011 The Regents of The University of Michigan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met: redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer;
 * redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution;
 * neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author: David Meisner (meisner@umich.edu)
 *
 */
package stochastic;

/**
 * Creates random numbers from an exponential distribution.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public class ExponentialGenerator extends Generator {

    /** The serialization id. */
    private static final long serialVersionUID = 1L;

    /** The exponential distribution's lambda parameter. */
    private double lambda;

    /**
     * Creates a new ExponentialGenerator.
     *
     * @param mtRandom - the random number generator to
     * get uniform random number from.
     * @param theLambda - the exponential distribution's lambda parameter
     */
    public ExponentialGenerator(final MTRandom mtRandom,
                                final double theLambda) {
        super(mtRandom);
        this.lambda = theLambda;
    }

    /**
     * Generates the next value.
     *
     * @return the next value
     */
    public double next() {
        double random = this.generator.nextDouble();
        double value = -Math.log(random) / this.lambda;
        assert (value != Float.NaN);

        return value;
    }

    /**
     * Gets the name of the generator.
     *
     * @return the name of the generator
     */
    @Override
    public String getName() {
        return "Exponential Generator param " + this.lambda;
    }

}
