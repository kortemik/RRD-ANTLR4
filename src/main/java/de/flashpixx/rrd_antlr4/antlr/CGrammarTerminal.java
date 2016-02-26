/**
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the RRD-AntLR4                                                #
 * # Copyright (c) 2016, Philipp Kraus (philipp.kraus@tu-clausthal.de)                  #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package de.flashpixx.rrd_antlr4.antlr;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;


/**
 * represenation of a terminal symbol
 */
public class CGrammarTerminal implements IGrammarTerminal
{
    /**
     * ID
     */
    private final String m_id;
    /**
     * fragment flag
     */
    private final boolean m_isfragment;
    /**
     * documentation string
     */
    private final String m_documentation;
    /**
     * alternatives
     */
    private final List<List<IGrammarSimpleElement<?>>> m_alternatives;

    /**
     * ctor
     *
     * @param p_id ID
     * @param p_isfragment fragment
     * @param p_documentation documentation
     * @param p_alternatives alternatives
     */
    public CGrammarTerminal( final String p_id, final boolean p_isfragment, final String p_documentation,
                             final List<List<IGrammarSimpleElement<?>>> p_alternatives
    )
    {
        m_id = p_id;
        m_isfragment = p_isfragment;
        m_documentation = p_documentation == null ? "" : p_documentation;
        m_alternatives = p_alternatives == null ? Collections.<List<IGrammarSimpleElement<?>>>emptyList() : p_alternatives;
    }

    @Override
    public final boolean isFragment()
    {
        return m_isfragment;
    }

    @Override
    public final List<List<IGrammarSimpleElement<?>>> alternatives()
    {
        return m_alternatives;
    }

    @Override
    public final String id()
    {
        return m_id;
    }

    @Override
    public final String documentation()
    {
        return m_documentation;
    }

    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public final boolean equals( final Object p_object )
    {
        return this.hashCode() == p_object.hashCode();
    }

    @Override
    public final String toString()
    {
        return MessageFormat.format(
                "{0}{1} -> {2}{3}",
                this.id(),
                m_isfragment ? " (fragment)" : "",
                StringUtils.join( m_alternatives, " | " ),
                m_documentation.isEmpty() ? "" : "   // " + m_documentation
        );
    }

}
