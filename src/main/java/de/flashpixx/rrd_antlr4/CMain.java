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


package de.flashpixx.rrd_antlr4;

import de.flashpixx.rrd_antlr4.engine.CEngine;
import de.flashpixx.rrd_antlr4.engine.template.ETemplate;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * standalone program and Maven plugin
 *
 * @see https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
 */
public final class CMain extends AbstractMojo
{
    /**
     * engine instance
     */
    private static final CEngine ENGINE = new CEngine();
    /**
     * default output directory
     */
    private static final String DEFAULTOUTPUT = "rrd-output";
    /**
     * default export format
     */
    private static final String DEFAULTTEMPLATE = "HTML";
    /**
     * default grammar file extension
     */
    private static final String GRAMMARFILEEXTENSION = ".g4";



    /**
     * Maven plugin parameter for output
     */
    @Parameter( defaultValue = "target/" + DEFAULTOUTPUT )
    private String output;
    /**
     * Maven plugin used templates option
     */
    @Parameter( defaultValue = DEFAULTTEMPLATE )
    private String[] template;
    /**
     * Maven plugin default directories of grammars
     */
    @Parameter( defaultValue = "src/main/antlr4" )
    private String[] grammar;
    /**
     * Maven plugin default grammar import directories
     */
    @Parameter( defaultValue = "src/main/antlr4/imports" )
    private String[] imports;
    /**
     * Maven plugin exclude file list
     */
    @Parameter
    private String[] exclude;



    /**
     * main
     *
     * @param p_args command-line arguments
     */
    public static void main( final String[] p_args )
    {
        // --- define CLI options --------------------------------------------------------------------------------------
        final Options l_clioptions = new Options();
        l_clioptions.addOption( "help", false, CCommon.getLanguageString( CMain.class, "help" ) );
        l_clioptions.addOption( "output", true, CCommon.getLanguageString( CMain.class, "output", DEFAULTOUTPUT ) );
        l_clioptions.addOption( "import", true, CCommon.getLanguageString( CMain.class, "import" ) );
        l_clioptions.addOption( "exclude", true, CCommon.getLanguageString( CMain.class, "exclude" ) );
        l_clioptions.addOption( "grammar", true, CCommon.getLanguageString( CMain.class, "grammar" ) );
        l_clioptions.addOption( "template", true, CCommon.getLanguageString( CMain.class, "template", Arrays.asList( ETemplate.values() ), DEFAULTTEMPLATE ) );


        final CommandLine l_cli;
        try
        {
            l_cli = new DefaultParser().parse( l_clioptions, p_args );
        }
        catch ( final Exception l_exception )
        {
            System.err.println( CCommon.getLanguageString( CMain.class, "parseerror", l_exception.getLocalizedMessage() ) );
            System.exit( -1 );
            return;
        }


        // --- process CLI arguments and push configuration ------------------------------------------------------------
        if ( l_cli.hasOption( "help" ) )
        {
            final HelpFormatter l_formatter = new HelpFormatter();
            l_formatter.printHelp(
                    ( new java.io.File( CMain.class.getProtectionDomain().getCodeSource().getLocation().getPath() ).getName() ), l_clioptions );
            System.exit( 0 );
        }


        if ( !l_cli.hasOption( "grammar" ) )
        {
            System.err.println( CCommon.getLanguageString( CMain.class, "grammarnotset" ) );
            System.exit( -1 );
        }

        final Set<String> l_exclude = l_cli.hasOption( "exclude" ) ? new HashSet<String>()
        {{
            Arrays.stream( l_cli.getOptionValue( "exclude" ).split( "," ) ).forEach( i -> add( i.trim() ) );
        }} : Collections.<String>emptySet();
        final String[] l_templates = l_cli.hasOption( "template" ) ? l_cli.getOptionValue( "template" ).split( "," ) : new String[]{DEFAULTTEMPLATE};
        final String l_outputdirectory = l_cli.hasOption( "output" ) ? l_cli.getOptionValue( "output" ) : DEFAULTOUTPUT;


        // --- run generating ------------------------------------------------------------------------------------------
        final Collection<String> l_errors = Arrays.stream( l_cli.getOptionValue( "grammar" ).split( "," ) )
                                                  .parallel()
                                                  .flatMap( i -> generate( l_outputdirectory, l_exclude, new File( i ), l_templates ).stream() )
                                                  .collect( Collectors.toList() );

        if ( !l_errors.isEmpty() )
        {
            l_errors.stream().forEach( System.err::println );
            System.exit( -1 );
        }
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException
    {

    }


    /**
     * generating export (generate template instances and call engine)
     *
     * @param p_outputdirectory output directory
     * @param p_exclude file names which are ignored
     * @param p_grammar path to grammar file or grammar file directory
     * @param p_template string with export name
     * @return returns a collection with error messages
     */
    private static Collection<String> generate( final String p_outputdirectory, final Set<String> p_exclude, final File p_grammar,
                                                final String... p_template
    )
    {
        return getFileList( p_grammar, p_exclude )
                .flatMap( i -> {
                    try
                    {
                        return ENGINE.generate(
                                p_outputdirectory,
                                Arrays.stream( p_template )
                                      .map( j -> ETemplate.valueOf( j.trim().toUpperCase() ).generate() )
                                      .collect( Collectors.toSet() ), i
                        ).stream();
                    }
                    catch ( final IOException p_exception )
                    {
                        return new LinkedList<String>()
                        {{
                            add( p_exception.getMessage() );
                        }}.stream();
                    }
                } )
                .filter( i -> i != null )
                .collect( Collectors.toList() );

    }

    /**
     * returns a list of grammar files
     *
     * @param p_input grammar file or directory with grammar files
     * @param p_exclude file names which are ignored
     * @return stream of file objects
     */
    private static Stream<File> getFileList( final File p_input, final Set<String> p_exclude )
    {
        return (
                p_input.isFile()

                ? new LinkedList<File>()
                {{
                    add( p_input );
                }}.stream()

                : Arrays.stream( p_input.listFiles( new FilenameFilter()
                {
                    @Override
                    public final boolean accept( final File p_dir, final String p_name )
                    {
                        return p_name.endsWith( GRAMMARFILEEXTENSION );
                    }
                } ) )
        ).filter( i -> !p_exclude.contains( i.getName() ) );
    }
}
