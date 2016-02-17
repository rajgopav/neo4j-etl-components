package org.neo4j.integration.sql.exportcsv.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.integration.sql.ConnectionConfig;
import org.neo4j.integration.sql.metadata.DatabaseObject;
import org.neo4j.integration.sql.metadata.TableName;
import org.neo4j.integration.sql.metadata.Join;
import org.neo4j.integration.sql.metadata.Table;
import org.neo4j.integration.neo4j.importcsv.config.Formatting;
import org.neo4j.integration.util.Preconditions;

import static java.lang.String.format;

import static org.neo4j.integration.util.StringListBuilder.stringList;

public class ExportToCsvConfig implements ExportProperties
{
    public static Builder.SetDestination builder()
    {
        return new ExportToCsvConfigBuilder();
    }

    private final Path destination;
    private final ConnectionConfig connectionConfig;
    private final Formatting formatting;
    private final Collection<Table> tables;
    private final Collection<Join> joins;

    ExportToCsvConfig( ExportToCsvConfigBuilder builder )
    {
        this.destination = Preconditions.requireNonNull( builder.destination, "Destination" );
        this.connectionConfig = Preconditions.requireNonNull( builder.connectionConfig, "Connection" );
        this.formatting = Preconditions.requireNonNull( builder.formatting, "Formatting" );
        this.tables = Collections.unmodifiableCollection( Preconditions.requireNonNull( builder.tables, "Tables" ) );
        this.joins = Collections.unmodifiableCollection( Preconditions.requireNonNull( builder.joins, "Joins" ) );

        validate();
    }



    @Override
    public Path destination()
    {
        return destination;
    }

    @Override
    public ConnectionConfig connectionConfig()
    {
        return connectionConfig;
    }

    @Override
    public Formatting formatting()
    {
        return formatting;
    }

    public Collection<Table> tables()
    {
        return tables;
    }

    public Collection<Join> joins()
    {
        return joins;
    }

    public Collection<DatabaseObject> databaseObjects()
    {
        Collection<DatabaseObject>  results = new ArrayList<>(  );
        results.addAll( tables );
        results.addAll( joins );
        return results;
    }

    private void validate()
    {
        List<TableName> allTableNames = tables.stream().map( Table::name ).collect( Collectors.toList() );

        joins.forEach(
                join -> join.tableNames().forEach(
                        tableName ->
                        {
                            if ( !allTableNames.contains( tableName ) )
                            {
                                throw new IllegalStateException(
                                        format( "Config is missing table definition '%s' for join [%s]",
                                                tableName.fullName(),
                                                stringList( join.tableNames(), " -> ", TableName::fullName ) ) );
                            }
                        } ) );

    }

    public interface Builder
    {
        interface SetDestination
        {
            SetMySqlConnectionConfig destination( Path directory );
        }

        interface SetMySqlConnectionConfig
        {
            SetFormatting connectionConfig( ConnectionConfig config );
        }

        interface SetFormatting
        {
            Builder formatting( Formatting formatting );
        }

        Builder addTable( Table table );

        Builder addTables( Collection<Table> tables );

        Builder addJoin( Join join );

        Builder addJoins( Collection<Join> joins );

        ExportToCsvConfig build();
    }
}
