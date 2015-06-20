/*
 * OrRule.java
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

package beast.xml;

/**
 * @author Arman Bilge
 */
public final class OrRule extends CompoundRule {

    public static XMLSyntaxRule newOrRule(final XMLSyntaxRule... rules) {
        return newOrRule(false, rules);
    }

    public static XMLSyntaxRule newOrRule(final boolean optional, final XMLSyntaxRule... rules) {
        return OptionalRule.newOptionalRule(new OrRule(rules), optional);
    }

    private OrRule(final XMLSyntaxRule... rules) {
        super("|", rules);
    }

    /**
     * Returns true if the rule is satisfied for the given XML object.
     */
    @Override
    public boolean isSatisfied(final XMLObject xo) {
        return getRules().stream().anyMatch(rule -> rule.isSatisfied(xo));
    }

}